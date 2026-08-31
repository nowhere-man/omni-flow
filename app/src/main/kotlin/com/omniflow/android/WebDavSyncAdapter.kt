package com.omniflow.android

import android.content.Context
import android.net.Uri
import android.util.Base64
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import com.omniflow.core.data.sync.SyncAdapter
import com.omniflow.core.domain.model.BackupRecord
import com.omniflow.core.domain.model.RemoteBackupMeta
import java.security.KeyStore
import java.util.concurrent.TimeUnit
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.datetime.Instant
import okhttp3.Credentials
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONObject
import kotlin.coroutines.cancellation.CancellationException

internal class WebDavHttpClient(
    private val client: OkHttpClient,
    private val credentials: () -> Pair<String, String>,
) {
    fun execute(
        url: String,
        method: String,
        headers: Map<String, String> = emptyMap(),
        body: String? = null,
    ): Response {
        val (username, password) = credentials()
        val request = Request.Builder().url(url).apply {
            if (username.isNotEmpty() || password.isNotEmpty()) {
                header("Authorization", Credentials.basic(username, password, Charsets.UTF_8))
            }
            headers.forEach { (name, value) -> header(name, value) }
        }.method(method, body?.toRequestBody()).build()
        return client.newCall(request).execute()
    }
}

class WebDavSyncAdapter(context: Context) : SyncAdapter {
    private val context = context.applicationContext
    private val preferences = this.context.getSharedPreferences("webdav", Context.MODE_PRIVATE)
    private val httpClient = WebDavHttpClient(
        client = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build(),
        credentials = {
            preferences.getString("username", "").orEmpty() to WebDavCredentials.password(context)
        },
    )

    override suspend fun listBackups(): Result<List<RemoteBackupMeta>> = io {
        ensureDirectory()
        open(
            directoryUrl(),
            "PROPFIND",
            headers = mapOf("Depth" to "1", "Content-Type" to "application/xml; charset=utf-8"),
            body = PROPFIND_BODY,
        ).use { connection ->
            requireStatus(connection, setOf(207))
            val response = connection.body?.string().orEmpty()
            HREF.findAll(response).mapNotNull { match ->
                val name = Uri.decode(match.groupValues[1]).substringAfterLast('/')
                parseMeta(name)
            }.distinctBy { it.backupId }.toList()
        }
    }

    override suspend fun uploadBackup(backup: BackupRecord): Result<Unit> = io {
        ensureDirectory()
        val body = JSONObject()
            .put("deviceId", backup.deviceId)
            .put("backupId", backup.backupId)
            .put("createdAt", backup.createdAt.toEpochMilliseconds())
            .put("payload", backup.payload)
            .toString()
        open(
            fileUrl(fileName(backup.createdAt, backup.deviceId, backup.backupId)),
            "PUT",
            headers = mapOf("Content-Type" to "application/json; charset=utf-8"),
            body = body,
        ).use { requireStatus(it, setOf(200, 201, 204)) }
    }

    override suspend fun downloadBackup(meta: RemoteBackupMeta): Result<BackupRecord> = io {
        open(fileUrl(fileName(meta.createdAt, meta.deviceId, meta.backupId)), "GET").use { connection ->
            requireStatus(connection, setOf(200))
            val json = JSONObject(connection.body?.string().orEmpty())
            BackupRecord(
                deviceId = json.getString("deviceId"),
                backupId = json.getString("backupId"),
                createdAt = Instant.fromEpochMilliseconds(json.getLong("createdAt")),
                payload = json.getString("payload"),
            )
        }
    }

    override suspend fun deleteBackup(meta: RemoteBackupMeta): Result<Unit> = io {
        open(fileUrl(fileName(meta.createdAt, meta.deviceId, meta.backupId)), "DELETE").use {
            requireStatus(it, setOf(200, 204, 404))
        }
    }

    private fun ensureDirectory() {
        open(directoryUrl(), "MKCOL").use { requireStatus(it, setOf(201, 405)) }
    }

    private fun open(
        url: String,
        method: String,
        headers: Map<String, String> = emptyMap(),
        body: String? = null,
    ): Response = httpClient.execute(url, method, headers, body)

    private fun requireStatus(connection: Response, expected: Set<Int>) {
        if (connection.code in expected) return
        val detail = connection.body?.string().orEmpty()
        error("WebDAV ${connection.request.method} 失败：${connection.code} ${detail.take(160)}")
    }

    private fun endpoint(): String = preferences.getString("endpoint", "")
        ?.trim()
        ?.trimEnd('/')
        ?.takeIf(String::isNotEmpty)
        ?.also { require(it.startsWith("https://")) { "WebDAV 地址必须使用 HTTPS" } }
        ?: error("请先填写 WebDAV 服务器目录")

    private fun directoryUrl(): String = "${endpoint()}/backups/"
    private fun fileUrl(name: String): String = directoryUrl() + Uri.encode(name)
    private fun fileName(createdAt: Instant, deviceId: String, backupId: String) =
        "${createdAt.toEpochMilliseconds()}_${deviceId}_${backupId}.backup"

    private fun parseMeta(name: String): RemoteBackupMeta? {
        if (!name.endsWith(".backup")) return null
        val parts = name.removeSuffix(".backup").split('_', limit = 3)
        if (parts.size != 3) return null
        return RemoteBackupMeta(
            deviceId = parts[1],
            backupId = parts[2],
            createdAt = parts[0].toLongOrNull()?.let(Instant::fromEpochMilliseconds) ?: return null,
        )
    }

    private suspend fun <T> io(block: () -> T): Result<T> = withContext(Dispatchers.IO) {
        try {
            Result.success(block())
        } catch (error: CancellationException) {
            throw error
        } catch (error: Throwable) {
            Result.failure(error)
        }
    }

    private companion object {
        val HREF = Regex("<[^>]*href[^>]*>(.*?)</[^>]*href>", RegexOption.IGNORE_CASE)
        const val PROPFIND_BODY = """<?xml version="1.0"?><propfind xmlns="DAV:"><prop><getlastmodified/></prop></propfind>"""
    }
}

object WebDavCredentials {
    private const val ALIAS = "omniflow-webdav"
    private const val TRANSFORMATION = "AES/GCM/NoPadding"

    fun save(context: Context, endpoint: String, username: String, password: String) {
        context.getSharedPreferences("webdav", Context.MODE_PRIVATE).edit()
            .putString("endpoint", endpoint.trimEnd('/'))
            .putString("username", username)
            .putString("encrypted_password", encrypt(password))
            .remove("password")
            .apply()
    }

    fun password(context: Context): String {
        val preferences = context.getSharedPreferences("webdav", Context.MODE_PRIVATE)
        preferences.getString("password", null)?.let { legacy ->
            save(
                context,
                preferences.getString("endpoint", "").orEmpty(),
                preferences.getString("username", "").orEmpty(),
                legacy,
            )
            return legacy
        }
        return preferences.getString("encrypted_password", null)?.let(::decrypt).orEmpty()
    }

    private fun encrypt(value: String): String {
        if (value.isEmpty()) return ""
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, key())
        return "${Base64.encodeToString(cipher.iv, Base64.NO_WRAP)}:${Base64.encodeToString(cipher.doFinal(value.encodeToByteArray()), Base64.NO_WRAP)}"
    }

    private fun decrypt(value: String): String = runCatching {
        if (value.isEmpty()) return@runCatching ""
        val (iv, payload) = value.split(':', limit = 2)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, Base64.decode(iv, Base64.NO_WRAP)))
        cipher.doFinal(Base64.decode(payload, Base64.NO_WRAP)).decodeToString()
    }.getOrDefault("")

    private fun key(): SecretKey {
        val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (store.getKey(ALIAS, null) as? SecretKey)?.let { return it }
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore").apply {
            init(
                KeyGenParameterSpec.Builder(
                    ALIAS,
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
                ).setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .build(),
            )
        }.generateKey()
    }
}
