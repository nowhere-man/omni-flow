package com.omniflow.shared.domain.facade

import com.omniflow.shared.domain.model.RemoteBackupMeta
import com.omniflow.shared.domain.model.SyncConfig
import com.omniflow.shared.domain.model.SyncResult
import com.omniflow.shared.domain.model.SyncState
import kotlinx.coroutines.flow.StateFlow

interface SyncFacade {
    fun observeSyncState(): StateFlow<SyncState>
    suspend fun configure(config: SyncConfig): Result<Unit>
    suspend fun listBackups(): Result<List<RemoteBackupMeta>>
    suspend fun syncNow(): Result<SyncResult>
    suspend fun restore(meta: RemoteBackupMeta): Result<Unit>
    fun scheduleBackup()
}
