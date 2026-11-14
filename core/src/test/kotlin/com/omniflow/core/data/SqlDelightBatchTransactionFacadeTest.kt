package com.omniflow.core.data

import com.omniflow.core.data.facade.SqlDelightBatchTransactionFacade
import com.omniflow.core.data.local.createJvmDatabase
import com.omniflow.core.data.repository.SqlDelightTransactionRepository
import com.omniflow.core.domain.model.Money
import com.omniflow.core.domain.model.Transaction
import com.omniflow.core.domain.model.TransactionType
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SqlDelightBatchTransactionFacadeTest {

    @Test
    fun batchDeleteRevertsBalancesIncludingTransfer() = runBlocking {
        val database = createJvmDatabase()
        seed(database)
        val repo = SqlDelightTransactionRepository(database)
        val facade = SqlDelightBatchTransactionFacade(database)

        repo.create(transaction("expense", TransactionType.EXPENSE, Money(200)))
        repo.create(transaction("income", TransactionType.INCOME, Money(500)))
        repo.create(
            transaction("transfer", TransactionType.TRANSFER, Money(100), categoryId = "")
                .copy(targetAccountId = "account-b"),
        )

        val balanceA = database.accountQueries.accountBalance("account-a").executeAsOne()
        val balanceB = database.accountQueries.accountBalance("account-b").executeAsOne()
        assertEquals(200L, balanceA) // -200 + 500 - 100 = 200
        assertEquals(100L, balanceB) // +100

        val result = facade.delete(listOf("expense", "income", "transfer")).getOrThrow()
        assertEquals(3, result.totalCount)
        assertEquals(3, result.processedCount)
        assertEquals(0, result.skippedCount)

        // 双账户余额均已完全回退
        assertEquals(0L, database.accountQueries.accountBalance("account-a").executeAsOne())
        assertEquals(0L, database.accountQueries.accountBalance("account-b").executeAsOne())

        // 交易已全部归档
        assertEquals(0, database.transactionQueries.transactionsForLedger("ledger-1").executeAsList().size)
    }

    @Test
    fun batchUpdateAccountAdjustsBalancesAndSkipsTransfer() = runBlocking {
        val database = createJvmDatabase()
        seed(database)
        val repo = SqlDelightTransactionRepository(database)
        val facade = SqlDelightBatchTransactionFacade(database)

        repo.create(transaction("expense", TransactionType.EXPENSE, Money(150)))
        repo.create(transaction("income", TransactionType.INCOME, Money(400)))
        repo.create(
            transaction("transfer", TransactionType.TRANSFER, Money(50), categoryId = "")
                .copy(targetAccountId = "account-b"),
        )

        // account-a: -150 + 400 - 50 = 200
        // account-b: +50
        assertEquals(200L, database.accountQueries.accountBalance("account-a").executeAsOne())
        assertEquals(50L, database.accountQueries.accountBalance("account-b").executeAsOne())

        val result = facade.updateAccount(listOf("expense", "income", "transfer"), "account-b").getOrThrow()
        assertEquals(3, result.totalCount)
        assertEquals(2, result.processedCount)
        assertEquals(1, result.skippedCount) // transfer 跳过

        // account-a: 之前 -150, +400 回退，还剩 transfer 的 -50 => -50
        assertEquals(-50L, database.accountQueries.accountBalance("account-a").executeAsOne())
        // account-b: 之前 50，新迁入 -150, +400 => 50 - 150 + 400 = 300
        assertEquals(300L, database.accountQueries.accountBalance("account-b").executeAsOne())

        // transfer 的账户未被破坏
        val transferRecord = database.transactionQueries.activeTransaction("transfer").executeAsOne()
        assertEquals("account-a", transferRecord.account_id)
        assertEquals("account-b", transferRecord.target_account_id)
    }

    @Test
    fun batchUpdateCategoryAndLedgerDoNotAffectAnyBalance() = runBlocking {
        val database = createJvmDatabase()
        seed(database)
        val repo = SqlDelightTransactionRepository(database)
        val facade = SqlDelightBatchTransactionFacade(database)

        repo.create(transaction("expense-1", TransactionType.EXPENSE, Money(200)))
        repo.create(transaction("expense-2", TransactionType.EXPENSE, Money(300)))

        val balanceBefore = database.accountQueries.accountBalance("account-a").executeAsOne()
        assertEquals(-500L, balanceBefore)

        // 批量改分类
        val catResult = facade.updateCategory(listOf("expense-1", "expense-2"), "cat-expense-2").getOrThrow()
        assertEquals(2, catResult.processedCount)
        assertEquals(0, catResult.skippedCount)
        assertEquals(balanceBefore, database.accountQueries.accountBalance("account-a").executeAsOne())

        // 批量改账本
        val ledgerResult = facade.updateLedger(listOf("expense-1", "expense-2"), "ledger-2").getOrThrow()
        assertEquals(2, ledgerResult.processedCount)
        assertEquals(0, ledgerResult.skippedCount)
        assertEquals(balanceBefore, database.accountQueries.accountBalance("account-a").executeAsOne())

        // 验证账本更新与分类在新账本中解析
        val t1 = database.transactionQueries.activeTransaction("expense-1").executeAsOne()
        assertEquals("ledger-2", t1.ledger_id)
    }

    @Test
    fun transferSkippedInBatchCategoryAndAccountReportingSkippedCount() = runBlocking {
        val database = createJvmDatabase()
        seed(database)
        val repo = SqlDelightTransactionRepository(database)
        val facade = SqlDelightBatchTransactionFacade(database)

        repo.create(transaction("expense", TransactionType.EXPENSE, Money(100)))
        repo.create(transaction("income", TransactionType.INCOME, Money(200)))
        repo.create(
            transaction("transfer-1", TransactionType.TRANSFER, Money(50), categoryId = "")
                .copy(targetAccountId = "account-b"),
        )
        repo.create(
            transaction("transfer-2", TransactionType.TRANSFER, Money(60), categoryId = "")
                .copy(targetAccountId = "account-b"),
        )

        // 批量改分类：2 笔 transfer 跳过，类型不匹配的 income 跳过
        val catResult = facade.updateCategory(
            listOf("expense", "income", "transfer-1", "transfer-2"),
            "cat-expense-2",
        ).getOrThrow()
        assertEquals(4, catResult.totalCount)
        assertEquals(1, catResult.processedCount)
        assertEquals(3, catResult.skippedCount) // 2 笔 transfer + 1 笔类型不匹配的 income

        // 批量改账户：2 笔 transfer 跳过
        val accResult = facade.updateAccount(
            listOf("expense", "income", "transfer-1", "transfer-2"),
            "account-b",
        ).getOrThrow()
        assertEquals(4, accResult.totalCount)
        assertEquals(2, accResult.processedCount)
        assertEquals(2, accResult.skippedCount) // 2 笔 transfer 跳过
    }

    @Test
    fun batchOperationIsAtomicAndRollsBackOnFailure() = runBlocking {
        val database = createJvmDatabase()
        seed(database)
        val repo = SqlDelightTransactionRepository(database)
        val facade = SqlDelightBatchTransactionFacade(database)

        repo.create(transaction("valid-expense", TransactionType.EXPENSE, Money(100)))
        val balanceBefore = database.accountQueries.accountBalance("account-a").executeAsOne()
        assertEquals(-100L, balanceBefore)

        // 传入有效 ID 和不存在的 ID，触发事务中途失败
        val result = facade.delete(listOf("valid-expense", "non-existent-id"))
        assertTrue(result.isFailure)

        // 验证整体回滚：valid-expense 未被删除，余额未被改变
        val validItem = database.transactionQueries.activeTransaction("valid-expense").executeAsOneOrNull()
        assertTrue(validItem != null)
        assertEquals(balanceBefore, database.accountQueries.accountBalance("account-a").executeAsOne())
    }

    private fun seed(database: com.omniflow.core.db.OmniFlowDatabase) {
        database.ledgerQueries.insertLedger("ledger-1", "日常账本", null, 1, 1)
        database.ledgerQueries.insertLedger("ledger-2", "旅游账本", null, 1, 1)
        database.accountQueries.insertAccount(
            id = "account-a",
            name = "现金账户",
            type = "CASH",
            icon_key = "wallet",
            card_number = null,
            note = null,
            balance_minor = 0,
            include_in_total_assets = 1L,
            created_at = 1,
            updated_at = 1,
        )
        database.accountQueries.insertAccount(
            id = "account-b",
            name = "银行卡账户",
            type = "DEBIT_CARD",
            icon_key = "landmark",
            card_number = null,
            note = null,
            balance_minor = 0,
            include_in_total_assets = 1L,
            created_at = 1,
            updated_at = 1,
        )
        database.categoryQueries.insertCategory("cat-expense-1", "ledger-1", null, "餐饮", "utensils", "EXPENSE", 1, 1)
        database.categoryQueries.insertCategory("cat-expense-2", "ledger-1", null, "交通", "bus", "EXPENSE", 2, 1)
        database.categoryQueries.insertCategory("cat-income", "ledger-1", null, "工资", "banknote", "INCOME", 1, 1)
        database.categoryQueries.insertCategory("cat-l2-expense", "ledger-2", null, "餐饮", "utensils", "EXPENSE", 1, 1)
    }

    private fun transaction(
        id: String,
        type: TransactionType,
        amount: Money,
        categoryId: String = if (type == TransactionType.INCOME) "cat-income" else "cat-expense-1",
    ) = Transaction(
        id = id,
        ledgerId = "ledger-1",
        accountId = "account-a",
        categoryId = categoryId,
        amount = amount,
        type = type,
        occurredAt = Instant.fromEpochMilliseconds(1_000),
        note = null,
        isExcluded = false,
        source = null,
        externalId = null,
    )
}
