package com.omniflow.shared

import app.cash.sqldelight.db.SqlDriver
import com.omniflow.shared.data.facade.SqlDelightAnalyticsFacade
import com.omniflow.shared.data.facade.SqlDelightHomeFacade
import com.omniflow.shared.data.facade.SqlDelightImportWorkflow
import com.omniflow.shared.data.facade.SqlDelightManagementFacade
import com.omniflow.shared.data.local.createDatabase
import com.omniflow.shared.data.repository.SqlDelightAccountRepository
import com.omniflow.shared.data.repository.SqlDelightCategoryMemoryRepository
import com.omniflow.shared.data.repository.SqlDelightCategoryRepository
import com.omniflow.shared.data.repository.SqlDelightImportSessionRepository
import com.omniflow.shared.data.repository.SqlDelightInitialDataRepository
import com.omniflow.shared.data.repository.SqlDelightLedgerRepository
import com.omniflow.shared.data.repository.SqlDelightRuleRepository
import com.omniflow.shared.data.repository.SqlDelightTagRepository
import com.omniflow.shared.data.repository.SqlDelightTransactionDedupeRepository
import com.omniflow.shared.data.repository.SqlDelightTransactionRepository
import com.omniflow.shared.data.usecase.SqlDelightSearchTransactionsUseCase
import com.omniflow.shared.domain.facade.AnalyticsFacade
import com.omniflow.shared.domain.facade.HomeFacade
import com.omniflow.shared.domain.facade.ImportWorkflow
import com.omniflow.shared.domain.facade.ManagementFacade
import com.omniflow.shared.domain.usecase.CreateTransactionUseCase
import com.omniflow.shared.domain.usecase.DeleteTransactionUseCase
import com.omniflow.shared.domain.usecase.InitializeAppUseCase
import com.omniflow.shared.domain.usecase.SearchTransactionsUseCase
import com.omniflow.shared.domain.usecase.UpdateTransactionUseCase

class SharedApp(driver: SqlDriver) {
    private val database = createDatabase(driver)
    private val ledgers = SqlDelightLedgerRepository(database)
    private val accounts = SqlDelightAccountRepository(database)
    private val categories = SqlDelightCategoryRepository(database)
    private val tags = SqlDelightTagRepository(database)
    private val transactions = SqlDelightTransactionRepository(database)

    val home: HomeFacade = SqlDelightHomeFacade(database)
    val management: ManagementFacade = SqlDelightManagementFacade(database)
    val analytics: AnalyticsFacade = SqlDelightAnalyticsFacade(database)
    val search: SearchTransactionsUseCase = SqlDelightSearchTransactionsUseCase(database)
    val imports: ImportWorkflow = SqlDelightImportWorkflow(
        sessions = SqlDelightImportSessionRepository(database),
        commits = transactions,
        accounts = accounts,
        categories = categories,
        rules = SqlDelightRuleRepository(database),
        categoryMemory = SqlDelightCategoryMemoryRepository(database),
        dedupe = SqlDelightTransactionDedupeRepository(database),
    )
    val initialize = InitializeAppUseCase(SqlDelightInitialDataRepository(database))
    val createTransaction = CreateTransactionUseCase(transactions)
    val updateTransaction = UpdateTransactionUseCase(transactions)
    val deleteTransaction = DeleteTransactionUseCase(transactions)
}
