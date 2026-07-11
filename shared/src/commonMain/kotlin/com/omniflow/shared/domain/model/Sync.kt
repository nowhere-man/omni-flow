package com.omniflow.shared.domain.model

import kotlinx.datetime.Instant

data class BackupRecord(
    val deviceId: String,
    val backupId: String,
    val createdAt: Instant,
    val payload: String,
)

data class RemoteBackupMeta(
    val deviceId: String,
    val backupId: String,
    val createdAt: Instant,
)

data class SyncConfig(
    val target: SyncTarget,
    val backupRetention: Int = DEFAULT_BACKUP_RETENTION.toInt(),
) {
    init {
        require(backupRetention > 0) { "备份保留数必须大于零" }
    }
}

enum class SyncPhase { IDLE, RUNNING, SUCCESS, ERROR }

data class SyncState(
    val phase: SyncPhase = SyncPhase.IDLE,
    val progress: Float? = null,
    val lastBackupAt: Instant? = null,
    val errorMessage: String? = null,
)

data class SyncResult(
    val backupId: String,
    val deletedOldBackups: Int,
)
