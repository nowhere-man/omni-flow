package com.omniflow.shared.data.sync

import app.cash.sqldelight.coroutines.asFlow
import com.omniflow.shared.db.OmniFlowDatabase
import com.omniflow.shared.domain.facade.AppPreferenceFacade
import com.omniflow.shared.domain.facade.SyncFacade
import com.omniflow.shared.domain.model.RemoteBackupMeta
import com.omniflow.shared.domain.model.SyncConfig
import com.omniflow.shared.domain.model.SyncPhase
import com.omniflow.shared.domain.model.SyncResult
import com.omniflow.shared.domain.model.SyncState
import com.omniflow.shared.domain.model.SyncTarget
import com.omniflow.shared.domain.util.UuidGenerator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

class SqlDelightSyncEngine(
    private val database: OmniFlowDatabase,
    private val preferences: AppPreferenceFacade,
    private val backupStore: BackupStore,
    private val adapters: Map<SyncTarget, SyncAdapter>,
    private val ids: UuidGenerator = UuidGenerator(),
    private val now: () -> Instant = { Clock.System.now() },
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default),
) : SyncFacade {
    private val mutex = Mutex()
    private val _state = MutableStateFlow(
        SyncState(lastBackupAt = database.syncMetaQueries.syncMeta(LAST_BACKUP_AT).executeAsOneOrNull()
            ?.toLongOrNull()
            ?.let(Instant::fromEpochMilliseconds)),
    )
    private var scheduledBackup: Job? = null

    init {
        scope.launch {
            merge(
                database.backupQueries.allLedgersForBackup().asFlow().drop(1).map { Unit },
                database.backupQueries.allAccountsForBackup().asFlow().drop(1).map { Unit },
                database.backupQueries.allAccountBalanceRecordsForBackup().asFlow().drop(1).map { Unit },
                database.backupQueries.allCategoriesForBackup().asFlow().drop(1).map { Unit },
                database.backupQueries.allTagsForBackup().asFlow().drop(1).map { Unit },
                database.backupQueries.allTransactionTagsForBackup().asFlow().drop(1).map { Unit },
                database.backupQueries.allTransactionsForBackup().asFlow().drop(1).map { Unit },
                database.backupQueries.allRulesForBackup().asFlow().drop(1).map { Unit },
                database.backupQueries.allCategoryMemoriesForBackup().asFlow().drop(1).map { Unit },
                database.backupQueries.allRemindersForBackup().asFlow().drop(1).map { Unit },
                database.backupQueries.allAppPreferencesForBackup().asFlow().drop(1).map { Unit },
            ).collect { scheduleBackup() }
        }
    }

    override fun observeSyncState(): StateFlow<SyncState> = _state.asStateFlow()

    override suspend fun configure(config: SyncConfig): Result<Unit> = runCatching {
        val current = preferences.observe().first().getOrThrow()
        preferences.save(
            current.copy(syncTarget = config.target, backupRetention = config.backupRetention),
        ).getOrThrow()
    }

    override suspend fun listBackups(): Result<List<RemoteBackupMeta>> = runCatching {
        val (_, adapter) = adapterAndRetention()
        adapter.listBackups().getOrThrow().sortedByDescending(RemoteBackupMeta::createdAt)
    }

    override suspend fun syncNow(): Result<SyncResult> = mutex.withLock {
        scheduledBackup?.cancel()
        scheduledBackup = null
        _state.value = _state.value.copy(phase = SyncPhase.RUNNING, progress = 0f, errorMessage = null)
        runCatching {
            val (retention, adapter) = adapterAndRetention()
            val timestamp = now()
            val backup = backupStore.create(deviceId(), ids.next(), timestamp.toEpochMilliseconds())
            _state.value = _state.value.copy(progress = 0.5f)
            adapter.uploadBackup(backup).getOrThrow()
            val oldBackups = adapter.listBackups().getOrThrow()
                .sortedByDescending(RemoteBackupMeta::createdAt)
                .drop(retention)
            oldBackups.forEach { adapter.deleteBackup(it).getOrThrow() }
            database.syncMetaQueries.upsertSyncMeta(
                LAST_BACKUP_AT,
                timestamp.toEpochMilliseconds().toString(),
                timestamp.toEpochMilliseconds(),
            )
            _state.value = SyncState(SyncPhase.SUCCESS, 1f, timestamp, null)
            SyncResult(backup.backupId, oldBackups.size)
        }.onFailure { error ->
            _state.value = _state.value.copy(
                phase = SyncPhase.ERROR,
                progress = null,
                errorMessage = error.message ?: "备份失败",
            )
        }
    }

    override suspend fun restore(meta: RemoteBackupMeta): Result<Unit> = mutex.withLock {
        scheduledBackup?.cancel()
        scheduledBackup = null
        _state.value = _state.value.copy(phase = SyncPhase.RUNNING, progress = 0f, errorMessage = null)
        runCatching {
            val (_, adapter) = adapterAndRetention()
            val backup = adapter.downloadBackup(meta).getOrThrow()
            _state.value = _state.value.copy(progress = 0.5f)
            backupStore.restore(backup)
            _state.value = _state.value.copy(phase = SyncPhase.SUCCESS, progress = 1f, errorMessage = null)
        }.onFailure { error ->
            _state.value = _state.value.copy(
                phase = SyncPhase.ERROR,
                progress = null,
                errorMessage = error.message ?: "恢复失败",
            )
        }
    }

    override fun scheduleBackup() {
        scheduledBackup?.cancel()
        scheduledBackup = scope.launch {
            delay(AUTO_BACKUP_DEBOUNCE_MILLIS)
            val current = preferences.observe().first().getOrNull()
            if (current?.syncTarget != null && adapters.containsKey(current.syncTarget)) syncNow()
        }
    }

    private suspend fun adapterAndRetention(): Pair<Int, SyncAdapter> {
        val current = preferences.observe().first().getOrThrow()
        val target = current.syncTarget ?: error("尚未配置同步目标")
        return current.backupRetention to (adapters[target] ?: error("当前平台不支持该同步目标"))
    }

    private fun deviceId(): String {
        database.syncMetaQueries.syncMeta(DEVICE_ID).executeAsOneOrNull()?.let { return it }
        val id = ids.next()
        val timestamp = now().toEpochMilliseconds()
        database.syncMetaQueries.upsertSyncMeta(DEVICE_ID, id, timestamp)
        return id
    }

    private companion object {
        const val DEVICE_ID = "device_id"
        const val LAST_BACKUP_AT = "last_backup_at"
        const val AUTO_BACKUP_DEBOUNCE_MILLIS = 2_000L
    }
}
