package com.omniflow.shared.data

import com.omniflow.shared.data.facade.SqlDelightAppPreferenceFacade
import com.omniflow.shared.data.local.createJvmDatabase
import com.omniflow.shared.data.sync.BackupStore
import com.omniflow.shared.data.sync.SqlDelightSyncEngine
import com.omniflow.shared.data.sync.SyncAdapter
import com.omniflow.shared.domain.model.BackupRecord
import com.omniflow.shared.domain.model.RemoteBackupMeta
import com.omniflow.shared.domain.model.SyncConfig
import com.omniflow.shared.domain.model.SyncTarget
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.Instant
import kotlin.test.Test
import kotlin.test.assertEquals

class SqlDelightSyncEngineTest {
    @Test
    fun manualBackupKeepsConfiguredNewestRecords() = runBlocking {
        val database = createJvmDatabase()
        val adapter = MemorySyncAdapter(
            (1L..12L).map { RemoteBackupMeta("device", "old-$it", Instant.fromEpochMilliseconds(it)) },
        )
        val engine = SqlDelightSyncEngine(
            database = database,
            preferences = SqlDelightAppPreferenceFacade(database),
            backupStore = object : BackupStore {
                override suspend fun create(deviceId: String, backupId: String, createdAtMillis: Long) =
                    BackupRecord(deviceId, backupId, Instant.fromEpochMilliseconds(createdAtMillis), "{}")
                override suspend fun restore(backup: BackupRecord) = Unit
            },
            adapters = mapOf(SyncTarget.WEBDAV to adapter),
            now = { Instant.fromEpochMilliseconds(100) },
        )
        engine.configure(SyncConfig(SyncTarget.WEBDAV, 10)).getOrThrow()

        val result = engine.syncNow().getOrThrow()

        assertEquals(3, result.deletedOldBackups)
        assertEquals(10, adapter.backups.size)
        assertEquals(100, adapter.backups.maxOf { it.createdAt.toEpochMilliseconds() })
    }

    private class MemorySyncAdapter(initial: List<RemoteBackupMeta>) : SyncAdapter {
        val backups = initial.toMutableList()
        override suspend fun listBackups() = Result.success(backups.toList())
        override suspend fun uploadBackup(backup: BackupRecord): Result<Unit> {
            backups += RemoteBackupMeta(backup.deviceId, backup.backupId, backup.createdAt)
            return Result.success(Unit)
        }
        override suspend fun downloadBackup(meta: RemoteBackupMeta) = Result.success(
            BackupRecord(meta.deviceId, meta.backupId, meta.createdAt, "{}"),
        )
        override suspend fun deleteBackup(meta: RemoteBackupMeta): Result<Unit> {
            backups.remove(meta)
            return Result.success(Unit)
        }
    }
}
