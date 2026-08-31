package com.omniflow.core.data

import com.omniflow.core.data.facade.SqlDelightAppPreferenceFacade
import com.omniflow.core.data.local.createJvmDatabase
import com.omniflow.core.data.sync.BackupStore
import com.omniflow.core.data.sync.SqlDelightSyncEngine
import com.omniflow.core.data.sync.SyncAdapter
import com.omniflow.core.domain.model.BackupRecord
import com.omniflow.core.domain.model.RemoteBackupMeta
import com.omniflow.core.domain.model.SyncConfig
import com.omniflow.core.domain.model.SyncTarget
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.Instant
import kotlin.coroutines.cancellation.CancellationException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

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

    @Test
    fun listBackupsPropagatesCancellation() = runBlocking {
        val database = createJvmDatabase()
        val engine = SqlDelightSyncEngine(
            database = database,
            preferences = SqlDelightAppPreferenceFacade(database),
            backupStore = EmptyBackupStore,
            adapters = mapOf(SyncTarget.WEBDAV to CancellingListAdapter()),
        )
        engine.configure(SyncConfig(SyncTarget.WEBDAV, 10)).getOrThrow()

        assertFailsWith<CancellationException> { engine.listBackups() }
        Unit
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

    private class CancellingListAdapter : SyncAdapter {
        override suspend fun listBackups(): Result<List<RemoteBackupMeta>> =
            throw CancellationException("cancelled")

        override suspend fun uploadBackup(backup: BackupRecord): Result<Unit> = Result.success(Unit)
        override suspend fun downloadBackup(meta: RemoteBackupMeta): Result<BackupRecord> =
            Result.success(BackupRecord(meta.deviceId, meta.backupId, meta.createdAt, "{}"))
        override suspend fun deleteBackup(meta: RemoteBackupMeta): Result<Unit> = Result.success(Unit)
    }

    private object EmptyBackupStore : BackupStore {
        override suspend fun create(deviceId: String, backupId: String, createdAtMillis: Long) =
            BackupRecord(deviceId, backupId, Instant.fromEpochMilliseconds(createdAtMillis), "{}")

        override suspend fun restore(backup: BackupRecord) = Unit
    }
}
