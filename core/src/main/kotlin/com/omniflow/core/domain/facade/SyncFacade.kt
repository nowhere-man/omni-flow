package com.omniflow.core.domain.facade

import com.omniflow.core.domain.model.RemoteBackupMeta
import com.omniflow.core.domain.model.SyncConfig
import com.omniflow.core.domain.model.SyncResult
import com.omniflow.core.domain.model.SyncState
import kotlinx.coroutines.flow.StateFlow

interface SyncFacade {
    fun observeSyncState(): StateFlow<SyncState>
    suspend fun configure(config: SyncConfig): Result<Unit>
    suspend fun listBackups(): Result<List<RemoteBackupMeta>>
    suspend fun syncNow(): Result<SyncResult>
    suspend fun restore(meta: RemoteBackupMeta): Result<Unit>
    fun scheduleBackup()
}
