package com.omniflow.shared.data.sync

import com.omniflow.shared.domain.model.BackupRecord
import com.omniflow.shared.domain.model.RemoteBackupMeta

interface SyncAdapter {
    suspend fun listBackups(): Result<List<RemoteBackupMeta>>
    suspend fun uploadBackup(backup: BackupRecord): Result<Unit>
    suspend fun downloadBackup(meta: RemoteBackupMeta): Result<BackupRecord>
    suspend fun deleteBackup(meta: RemoteBackupMeta): Result<Unit>
}

interface BackupStore {
    suspend fun create(deviceId: String, backupId: String, createdAtMillis: Long): BackupRecord
    suspend fun restore(backup: BackupRecord)
}
