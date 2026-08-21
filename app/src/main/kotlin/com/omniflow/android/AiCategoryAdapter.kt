package com.omniflow.android

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import com.omniflow.core.data.ai.OpenAiChatProtocol
import com.omniflow.core.domain.ai.CategorySuggester
import com.omniflow.core.domain.ai.CategorySuggestionRequest
import com.omniflow.core.domain.model.CategoryId
import java.net.HttpURLConnection
import java.net.URL
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 用 OpenAI 兼容接口给导入条目建议分类。
 *
 * 和 [WebDavSyncAdapter] 一个路子：core 只认接口，平台这边负责 HTTP 和密钥。
 * 请求体拼装和响应解析都在 core 的 [OpenAiChatProtocol] 里（那部分要能单测），
 * 这里只剩「把 body POST 出去」。
 */
class AiCategoryAdapter(context: Context) : CategorySuggester {
    private val context = context.applicationContext

    override suspend fun isConfigured(): Boolean = AiCredentials.load(context).isUsable

    override suspend fun suggest(request: CategorySuggestionRequest): Result<Map<String, CategoryId>> = io {
        val config = AiCredentials.load(context)
        require(config.isUsable) { "还没有配置 AI 服务" }
        buildMap {
            // 支出和收入的候选分类不同，分开问；每批最多 BATCH_SIZE 条，串行发避免触发限流
            request.entries.groupBy { it.type }.forEach { (type, typed) ->
                val options = request.optionsFor(type)
                if (options.isEmpty()) return@forEach
                typed.chunked(OpenAiChatProtocol.BATCH_SIZE).forEach { batch ->
                    val body = OpenAiChatProtocol.requestBody(config.model, batch, options)
                    val content = OpenAiChatProtocol.parseContent(post(config, body))
                    putAll(OpenAiChatProtocol.parseSuggestions(content, batch, options))
                }
            }
        }
    }

    /** 配置页的「测试连接」：一次最小请求就能同时验证地址、密钥和模型名。 */
    suspend fun testConnection(config: AiConfig): Result<String> = io {
        require(config.baseUrl.isNotBlank()) { "请先填写服务地址" }
        require(config.model.isNotBlank()) { "请先填写模型名称" }
        val reply = OpenAiChatProtocol.parseContent(post(config, OpenAiChatProtocol.pingBody(config.model))).trim()
        if (reply.isEmpty()) "连接成功" else "连接成功：${reply.take(40)}"
    }

    private fun post(config: AiConfig, body: String): String {
        val connection = (URL(OpenAiChatProtocol.endpoint(config.baseUrl)).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 15_000
            // 大模型出字慢，读超时给到 60s；再长用户早就以为卡死了
            readTimeout = 60_000
            doInput = true
            doOutput = true
            setRequestProperty("Content-Type", "application/json; charset=utf-8")
            if (config.apiKey.isNotEmpty()) setRequestProperty("Authorization", "Bearer ${config.apiKey}")
        }
        connection.outputStream.use { it.write(body.encodeToByteArray()) }
        if (connection.responseCode !in 200..299) {
            val detail = connection.errorStream?.bufferedReader()?.use { it.readText() }.orEmpty()
            error("AI 服务返回 ${connection.responseCode}：${detail.take(160)}")
        }
        return connection.inputStream.bufferedReader().use { it.readText() }
    }

    private suspend fun <T> io(block: () -> T): Result<T> = withContext(Dispatchers.IO) { runCatching(block) }
}

data class AiConfig(
    val enabled: Boolean,
    val baseUrl: String,
    val apiKey: String,
    val model: String,
) {
    /** 本地 Ollama 之类的服务不需要密钥，所以只要地址和模型名齐了就算可用。 */
    val isUsable: Boolean get() = enabled && baseUrl.isNotBlank() && model.isNotBlank()
}

/**
 * AI 配置的落盘。密钥必须走 Keystore 加密后再进 SharedPreferences——
 * 放 `AppPreferences` 会被 `SqlDelightBackupStore` 整表写进 WebDAV 备份的明文 JSON。
 * 逐行对应 [WebDavCredentials]。
 */
object AiCredentials {
    private const val FILE = "ai"
    private const val ALIAS = "omniflow-ai"
    private const val TRANSFORMATION = "AES/GCM/NoPadding"

    fun load(context: Context): AiConfig {
        val preferences = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
        return AiConfig(
            enabled = preferences.getBoolean("enabled", false),
            baseUrl = preferences.getString("base_url", "").orEmpty(),
            apiKey = preferences.getString("encrypted_api_key", null)?.let(::decrypt).orEmpty(),
            model = preferences.getString("model", "").orEmpty(),
        )
    }

    fun save(context: Context, config: AiConfig) {
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE).edit()
            .putBoolean("enabled", config.enabled)
            .putString("base_url", config.baseUrl.trim().trimEnd('/'))
            .putString("model", config.model.trim())
            .putString("encrypted_api_key", encrypt(config.apiKey))
            .apply()
    }

    private fun encrypt(value: String): String {
        if (value.isEmpty()) return ""
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, key())
        return "${Base64.encodeToString(cipher.iv, Base64.NO_WRAP)}:" +
            Base64.encodeToString(cipher.doFinal(value.encodeToByteArray()), Base64.NO_WRAP)
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
