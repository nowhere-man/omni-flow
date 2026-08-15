package com.omniflow.core

import app.cash.sqldelight.db.SqlDriver
import com.omniflow.core.data.facade.SqlDelightAnalyticsFacade
import com.omniflow.core.data.facade.SqlDelightAppPreferenceFacade
import com.omniflow.core.data.facade.SqlDelightHomeFacade
import com.omniflow.core.data.facade.SqlDelightImportWorkflow
import com.omniflow.core.data.facade.SqlDelightManagementFacade
import com.omniflow.core.data.facade.SqlDelightQingziInteropFacade
import com.omniflow.core.data.facade.SqlDelightReminderFacade
import com.omniflow.core.data.local.createDatabase
import com.omniflow.core.data.repository.SqlDelightAccountRepository
import com.omniflow.core.data.repository.SqlDelightCategoryMemoryRepository
import com.omniflow.core.data.repository.SqlDelightCategoryRepository
import com.omniflow.core.data.repository.SqlDelightImportSessionRepository
import com.omniflow.core.data.repository.SqlDelightInitialDataRepository
import com.omniflow.core.data.repository.SqlDelightLedgerRepository
import com.omniflow.core.data.repository.SqlDelightRuleRepository
import com.omniflow.core.data.repository.SqlDelightReminderRepository
import com.omniflow.core.data.repository.SqlDelightTagRepository
import com.omniflow.core.data.repository.SqlDelightTransactionDedupeRepository
import com.omniflow.core.data.repository.SqlDelightTransactionRepository
import com.omniflow.core.data.usecase.SqlDelightSearchTransactionsUseCase
import com.omniflow.core.data.sync.SqlDelightBackupStore
import com.omniflow.core.data.sync.SqlDelightSyncEngine
import com.omniflow.core.data.sync.SyncAdapter
import com.omniflow.core.domain.facade.AnalyticsFacade
import com.omniflow.core.domain.facade.AppPreferenceFacade
import com.omniflow.core.domain.facade.HomeFacade
import com.omniflow.core.domain.facade.ImportWorkflow
import com.omniflow.core.domain.facade.ManagementFacade
import com.omniflow.core.domain.facade.QingziInteropFacade
import com.omniflow.core.domain.facade.ReminderFacade
import com.omniflow.core.domain.facade.SyncFacade
import com.omniflow.core.domain.model.SyncTarget
import com.omniflow.core.domain.model.TransactionId
import com.omniflow.core.domain.model.TransactionRecordDetail
import com.omniflow.core.domain.model.toRecordDetail
import com.omniflow.core.domain.usecase.CreateTransactionUseCase
import com.omniflow.core.domain.usecase.CalibrateAccountUseCase
import com.omniflow.core.domain.usecase.CreateAccountUseCase
import com.omniflow.core.domain.usecase.CreateCategoryUseCase
import com.omniflow.core.domain.usecase.CreateLedgerUseCase
import com.omniflow.core.domain.usecase.CreateRuleUseCase
import com.omniflow.core.domain.usecase.CreateReminderUseCase
import com.omniflow.core.domain.usecase.CreateTagUseCase
import com.omniflow.core.domain.usecase.DeleteAccountUseCase
import com.omniflow.core.domain.usecase.DeleteCategoryUseCase
import com.omniflow.core.domain.usecase.ReorderPrimaryCategoriesUseCase
import com.omniflow.core.domain.usecase.DeleteLedgerUseCase
import com.omniflow.core.domain.usecase.DeleteRuleUseCase
import com.omniflow.core.domain.usecase.DeleteReminderUseCase
import com.omniflow.core.domain.usecase.DeleteTagUseCase
import com.omniflow.core.domain.usecase.DeleteTransactionUseCase
import com.omniflow.core.domain.usecase.InitializeAppUseCase
import com.omniflow.core.domain.usecase.GetTransactionUseCase
import com.omniflow.core.domain.usecase.SearchTransactionsUseCase
import com.omniflow.core.domain.usecase.SetDefaultLedgerUseCase
import com.omniflow.core.domain.usecase.SetReminderPausedUseCase
import com.omniflow.core.domain.usecase.UpdateAccountUseCase
import com.omniflow.core.domain.usecase.UpdateCategoryUseCase
import com.omniflow.core.domain.usecase.UpdateLedgerUseCase
import com.omniflow.core.domain.usecase.UpdateRuleUseCase
import com.omniflow.core.domain.usecase.ReorderRulesUseCase
import com.omniflow.core.domain.usecase.UpdateReminderUseCase
import com.omniflow.core.domain.usecase.UpdateTagUseCase
import com.omniflow.core.domain.usecase.UpdateTransactionUseCase

class SharedApp(
    driver: SqlDriver,
    syncAdapters: Map<SyncTarget, SyncAdapter> = emptyMap(),
) {
    private val database = createDatabase(driver)
    private val ledgers = SqlDelightLedgerRepository(database)
    private val accounts = SqlDelightAccountRepository(database)
    private val categories = SqlDelightCategoryRepository(database)
    private val tags = SqlDelightTagRepository(database)
    private val rules = SqlDelightRuleRepository(database)
    private val reminderRepository = SqlDelightReminderRepository(database)
    private val transactions = SqlDelightTransactionRepository(database)
    private val preferenceFacade = SqlDelightAppPreferenceFacade(database)

    val home: HomeFacade = SqlDelightHomeFacade(database)
    val management: ManagementFacade = SqlDelightManagementFacade(database)
    val analytics: AnalyticsFacade = SqlDelightAnalyticsFacade(database)
    val preferences: AppPreferenceFacade = preferenceFacade
    val reminders: ReminderFacade = SqlDelightReminderFacade(database)
    val qingzi: QingziInteropFacade = SqlDelightQingziInteropFacade(database)
    val sync: SyncFacade = SqlDelightSyncEngine(
        database = database,
        preferences = preferenceFacade,
        backupStore = SqlDelightBackupStore(database),
        adapters = syncAdapters,
    )
    val search: SearchTransactionsUseCase = SqlDelightSearchTransactionsUseCase(database)
    val imports: ImportWorkflow = SqlDelightImportWorkflow(
        sessions = SqlDelightImportSessionRepository(database),
        commits = transactions,
        accounts = accounts,
        categories = categories,
        rules = rules,
        categoryMemory = SqlDelightCategoryMemoryRepository(database),
        dedupe = SqlDelightTransactionDedupeRepository(database),
    )
    val initialize = InitializeAppUseCase(SqlDelightInitialDataRepository(database))
    val createLedger = CreateLedgerUseCase(ledgers)
    val updateLedger = UpdateLedgerUseCase(ledgers)
    val deleteLedger = DeleteLedgerUseCase(ledgers)
    val setDefaultLedger = SetDefaultLedgerUseCase(ledgers)
    val createAccount = CreateAccountUseCase(accounts)
    val updateAccount = UpdateAccountUseCase(accounts)
    val calibrateAccount = CalibrateAccountUseCase(accounts)
    val deleteAccount = DeleteAccountUseCase(accounts)
    val createCategory = CreateCategoryUseCase(categories)
    val updateCategory = UpdateCategoryUseCase(categories)
    val deleteCategory = DeleteCategoryUseCase(categories)
    val reorderPrimaryCategories = ReorderPrimaryCategoriesUseCase(categories)
    val createTag = CreateTagUseCase(tags)
    val updateTag = UpdateTagUseCase(tags)
    val deleteTag = DeleteTagUseCase(tags)
    val createRule = CreateRuleUseCase(rules)
    val updateRule = UpdateRuleUseCase(rules)
    val reorderRules = ReorderRulesUseCase(rules)
    val deleteRule = DeleteRuleUseCase(rules)
    val createReminder = CreateReminderUseCase(reminderRepository)
    val updateReminder = UpdateReminderUseCase(reminderRepository)
    val setReminderPaused = SetReminderPausedUseCase(reminderRepository)
    val deleteReminder = DeleteReminderUseCase(reminderRepository)
    val createTransaction = CreateTransactionUseCase(transactions)
    val getTransaction = GetTransactionUseCase(transactions)
    val updateTransaction = UpdateTransactionUseCase(transactions)
    val deleteTransaction = DeleteTransactionUseCase(transactions)

    suspend fun getTransactionRecordDetail(transactionId: TransactionId): Result<TransactionRecordDetail?> = runCatching {
        transactions.activeTransaction(transactionId)?.let { transaction ->
            transaction.toRecordDetail(
                ledgers = ledgers.activeLedgers(),
                accounts = accounts.activeAccounts(),
                categories = categories.activeCategories(transaction.ledgerId),
                tags = tags.activeTags(transaction.ledgerId),
            )
        }
    }
}
