package com.omniflow.shared

import app.cash.sqldelight.db.SqlDriver
import com.omniflow.shared.data.facade.SqlDelightAnalyticsFacade
import com.omniflow.shared.data.facade.SqlDelightAppPreferenceFacade
import com.omniflow.shared.data.facade.SqlDelightHomeFacade
import com.omniflow.shared.data.facade.SqlDelightImportWorkflow
import com.omniflow.shared.data.facade.SqlDelightManagementFacade
import com.omniflow.shared.data.facade.SqlDelightQingziInteropFacade
import com.omniflow.shared.data.facade.SqlDelightReminderFacade
import com.omniflow.shared.data.local.createDatabase
import com.omniflow.shared.data.repository.SqlDelightAccountRepository
import com.omniflow.shared.data.repository.SqlDelightCategoryMemoryRepository
import com.omniflow.shared.data.repository.SqlDelightCategoryRepository
import com.omniflow.shared.data.repository.SqlDelightImportSessionRepository
import com.omniflow.shared.data.repository.SqlDelightInitialDataRepository
import com.omniflow.shared.data.repository.SqlDelightLedgerRepository
import com.omniflow.shared.data.repository.SqlDelightRuleRepository
import com.omniflow.shared.data.repository.SqlDelightReminderRepository
import com.omniflow.shared.data.repository.SqlDelightTagRepository
import com.omniflow.shared.data.repository.SqlDelightTransactionDedupeRepository
import com.omniflow.shared.data.repository.SqlDelightTransactionRepository
import com.omniflow.shared.data.usecase.SqlDelightSearchTransactionsUseCase
import com.omniflow.shared.data.sync.SqlDelightBackupStore
import com.omniflow.shared.data.sync.SqlDelightSyncEngine
import com.omniflow.shared.data.sync.SyncAdapter
import com.omniflow.shared.domain.facade.AnalyticsFacade
import com.omniflow.shared.domain.facade.AppPreferenceFacade
import com.omniflow.shared.domain.facade.HomeFacade
import com.omniflow.shared.domain.facade.ImportWorkflow
import com.omniflow.shared.domain.facade.ManagementFacade
import com.omniflow.shared.domain.facade.QingziInteropFacade
import com.omniflow.shared.domain.facade.ReminderFacade
import com.omniflow.shared.domain.facade.SyncFacade
import com.omniflow.shared.domain.model.SyncTarget
import com.omniflow.shared.domain.usecase.CreateTransactionUseCase
import com.omniflow.shared.domain.usecase.CalibrateAccountUseCase
import com.omniflow.shared.domain.usecase.CreateAccountUseCase
import com.omniflow.shared.domain.usecase.CreateCategoryUseCase
import com.omniflow.shared.domain.usecase.CreateLedgerUseCase
import com.omniflow.shared.domain.usecase.CreateRuleUseCase
import com.omniflow.shared.domain.usecase.CreateReminderUseCase
import com.omniflow.shared.domain.usecase.CreateTagUseCase
import com.omniflow.shared.domain.usecase.DeleteAccountUseCase
import com.omniflow.shared.domain.usecase.DeleteCategoryUseCase
import com.omniflow.shared.domain.usecase.ReorderPrimaryCategoriesUseCase
import com.omniflow.shared.domain.usecase.DeleteLedgerUseCase
import com.omniflow.shared.domain.usecase.DeleteRuleUseCase
import com.omniflow.shared.domain.usecase.DeleteReminderUseCase
import com.omniflow.shared.domain.usecase.DeleteTagUseCase
import com.omniflow.shared.domain.usecase.DeleteTransactionUseCase
import com.omniflow.shared.domain.usecase.InitializeAppUseCase
import com.omniflow.shared.domain.usecase.GetTransactionUseCase
import com.omniflow.shared.domain.usecase.SearchTransactionsUseCase
import com.omniflow.shared.domain.usecase.SetDefaultLedgerUseCase
import com.omniflow.shared.domain.usecase.SetReminderPausedUseCase
import com.omniflow.shared.domain.usecase.UpdateAccountUseCase
import com.omniflow.shared.domain.usecase.UpdateCategoryUseCase
import com.omniflow.shared.domain.usecase.UpdateLedgerUseCase
import com.omniflow.shared.domain.usecase.UpdateRuleUseCase
import com.omniflow.shared.domain.usecase.ReorderRulesUseCase
import com.omniflow.shared.domain.usecase.UpdateReminderUseCase
import com.omniflow.shared.domain.usecase.UpdateTagUseCase
import com.omniflow.shared.domain.usecase.UpdateTransactionUseCase

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
}
