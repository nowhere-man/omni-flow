package com.omniflow.shared.data.sync

import com.omniflow.shared.domain.model.BackupRecord
import com.omniflow.shared.domain.model.RemoteBackupMeta
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import kotlinx.datetime.Instant
import platform.Foundation.NSData
import platform.Foundation.NSFileManager
import platform.Foundation.create
import platform.Foundation.writeToFile
import platform.Foundation.dataWithContentsOfFile
import platform.posix.memcpy

@OptIn(ExperimentalForeignApi::class)
class AppleFileSyncAdapter(baseDirectory: String) : SyncAdapter {
    private val directory = baseDirectory.trimEnd('/') + "/backups"
    private val files = NSFileManager.defaultManager

    override suspend fun listBackups(): Result<List<RemoteBackupMeta>> = runCatching {
        ensureDirectory()
        files.contentsOfDirectoryAtPath(directory, null)
            ?.mapNotNull { parseMeta(it.toString()) }
            .orEmpty()
    }

    override suspend fun uploadBackup(backup: BackupRecord): Result<Unit> = runCatching {
        ensureDirectory()
        val bytes = backup.payload.encodeToByteArray()
        val data = bytes.usePinned { NSData.create(it.addressOf(0), bytes.size.toULong()) }
        check(data.writeToFile(path(backup), atomically = true)) { "无法写入 iCloud 备份" }
    }

    override suspend fun downloadBackup(meta: RemoteBackupMeta): Result<BackupRecord> = runCatching {
        val data = NSData.dataWithContentsOfFile(path(meta)) ?: error("iCloud 备份不存在")
        val bytes = ByteArray(data.length.toInt())
        bytes.usePinned { memcpy(it.addressOf(0), data.bytes, data.length) }
        BackupRecord(meta.deviceId, meta.backupId, meta.createdAt, bytes.decodeToString())
    }

    override suspend fun deleteBackup(meta: RemoteBackupMeta): Result<Unit> = runCatching {
        val path = path(meta)
        if (files.fileExistsAtPath(path)) check(files.removeItemAtPath(path, null)) { "无法删除旧 iCloud 备份" }
    }

    private fun ensureDirectory() {
        if (!files.fileExistsAtPath(directory)) {
            check(files.createDirectoryAtPath(directory, true, null, null)) { "无法创建 iCloud 备份目录" }
        }
    }

    private fun path(backup: BackupRecord) = "$directory/${fileName(backup.createdAt, backup.deviceId, backup.backupId)}"
    private fun path(meta: RemoteBackupMeta) = "$directory/${fileName(meta.createdAt, meta.deviceId, meta.backupId)}"
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
}
