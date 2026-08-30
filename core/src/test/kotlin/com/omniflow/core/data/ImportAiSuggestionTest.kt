package com.omniflow.core.data

import com.omniflow.core.data.facade.SqlDelightImportWorkflow
import com.omniflow.core.data.local.createJvmDatabase
import com.omniflow.core.data.repository.SqlDelightAccountRepository
import com.omniflow.core.data.repository.SqlDelightCategoryMemoryRepository
import com.omniflow.core.data.repository.SqlDelightCategoryRepository
import com.omniflow.core.data.repository.SqlDelightImportSessionRepository
import com.omniflow.core.data.repository.SqlDelightRuleRepository
import com.omniflow.core.data.repository.SqlDelightTransactionDedupeRepository
import com.omniflow.core.data.repository.SqlDelightTransactionRepository
import com.omniflow.core.db.OmniFlowDatabase
import com.omniflow.core.domain.ai.CategorySuggester
import com.omniflow.core.domain.ai.CategorySuggestionRequest
import com.omniflow.core.domain.model.CategoryId
import com.omniflow.core.domain.model.DateRange
import com.omniflow.core.domain.model.ImportCategoryOrigin
import com.omniflow.core.domain.model.ImportPreviewPhase
import com.omniflow.core.domain.model.ImportPreviewState
import com.omniflow.core.domain.model.ImportRequest
import com.omniflow.core.parser.ImportFormat
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * 导入流程里的 AI 建议。全部用桩 suggester，不联网。
 * 重点验证三件事：只问空缺、同一来源键只问一次、任何失败都不能影响导入本身。
 */
class ImportAiSuggestionTest {
    @Test
    fun dateRangeKeepsOnlyTransactionsInsideInclusiveDates() = runBlocking {
        val range = DateRange(
            startInclusive = Instant.parse("2026-01-06T00:00:00+08:00"),
            endExclusive = Instant.parse("2026-01-07T00:00:00+08:00"),
        )
        val state = workflow(seed(), RecordingSuggester(emptyMap(), configured = false))
            .preview(
                ImportRequest(
                    ledgerId = "ledger",
                    fileName = "美团.csv",
                    bytes = BILL.encodeToByteArray(),
                    selectedFormat = ImportFormat.MEITUAN,
                    dateRange = range,
                ),
            )
            .toList()
            .last()
            .getOrThrow()

        assertEquals(listOf("M2"), state.items.map { it.raw.externalId })
    }

    @Test
    fun asksOnlyForGapsAndDeduplicatesByMemoryKey() = runBlocking {
        val suggester = RecordingSuggester(mapOf("MEITUAN:party:星巴克咖啡" to "c-food"))
        val state = preview(suggester)

        // 星巴克出现两笔、滴滴一笔、内置映射一条都没命中，去重后只该问两个键
        val asked = suggester.requests.single().entries
        assertEquals(2, asked.size, "同一个商户的多笔应该合并成一次提问")
        assertEquals(setOf("MEITUAN:party:星巴克咖啡", "MEITUAN:party:滴滴出行"), asked.mapTo(mutableSetOf()) { it.key })

        // 一次回答回填到同键的全部条目
        val food = state.items.filter { it.categoryId == "c-food" }
        assertEquals(2, food.size)
        assertTrue(food.all { it.categoryOrigin == ImportCategoryOrigin.AI })
        // 没被回答的键保持待分类，不能瞎填
        assertTrue(state.items.filter { it.raw.counterparty == "滴滴出行" }.all { it.categoryId == null })
        assertNull(state.suggestionError)
    }

    @Test
    fun keepsUserAndRuleAssignmentsOutOfTheRequest() = runBlocking {
        val database = seed()
        // 教一次「滴滴出行」，它就不该再出现在 AI 的提问里
        SqlDelightCategoryMemoryRepository(database)
            .remember("ledger", "MEITUAN:party:滴滴出行", "c-transit")
        val suggester = RecordingSuggester(emptyMap())
        val state = preview(suggester, database)

        assertEquals(
            setOf("MEITUAN:party:星巴克咖啡"),
            suggester.requests.single().entries.mapTo(mutableSetOf()) { it.key },
            "已经有分类记忆的条目不该再问 AI",
        )
        val transit = state.items.filter { it.categoryId == "c-transit" }
        assertEquals(1, transit.size)
        assertEquals(ImportCategoryOrigin.MEMORY, transit.single().categoryOrigin)
    }

    @Test
    fun failureLeavesImportUsable() = runBlocking {
        val suggester = object : CategorySuggester {
            override suspend fun isConfigured() = true
            override suspend fun suggest(request: CategorySuggestionRequest) =
                Result.failure<Map<String, CategoryId>>(IllegalStateException("401 Unauthorized"))
        }
        val state = preview(suggester)

        assertTrue(state.suggestionError!!.contains("401"), "失败原因要带到界面上")
        assertEquals(ImportPreviewPhase.READY, state.phase, "AI 挂了不能把导入卡在中间相位")
        assertEquals(3, state.items.size, "条目一条都不能少")
        assertTrue(state.items.all { it.categoryId == null }, "失败时保持待分类，等用户手动归类")
    }

    @Test
    fun rejectsCategoriesFromAnotherLedgerOrDirection() = runBlocking {
        // c-salary 是收入分类，拿它去填支出条目必须被拦下
        val state = preview(RecordingSuggester(mapOf("MEITUAN:party:星巴克咖啡" to "c-salary")))
        assertTrue(state.items.all { it.categoryId == null }, "收支方向对不上的分类不能落进条目")
    }

    @Test
    fun skipsEntirelyWhenNotConfigured() = runBlocking {
        val suggester = RecordingSuggester(mapOf("MEITUAN:party:星巴克咖啡" to "c-food"), configured = false)
        val states = previewStates(suggester)

        assertTrue(suggester.requests.isEmpty(), "没配置时一个请求都不能发")
        assertTrue(states.none { it.phase == ImportPreviewPhase.SUGGESTING }, "没配置时不该出现 AI 相位")
        assertTrue(states.last().items.all { it.categoryId == null })
    }

    @Test
    fun commitRemembersAiAndUserAssignments() = runBlocking {
        val database = seed()
        val state = preview(RecordingSuggester(mapOf("MEITUAN:party:星巴克咖啡" to "c-food")), database)
        val sessions = SqlDelightImportSessionRepository(database)
        // 剩下那条手动归类，模拟用户在预览页改分类
        sessions.updateCategories(
            state.sessionId,
            state.items.filter { it.categoryId == null }.mapTo(mutableSetOf()) { it.id },
            "c-transit",
        )
        assertTrue(workflow(database, RecordingSuggester(emptyMap())).commit(state.sessionId).isSuccess)

        val memory = SqlDelightCategoryMemoryRepository(database)
        assertEquals("c-food", memory.categoryId("ledger", "MEITUAN:party:星巴克咖啡"), "AI 的判断要写进记忆")
        assertEquals("c-transit", memory.categoryId("ledger", "MEITUAN:party:滴滴出行"), "用户的修改要写进记忆")
    }

    private class RecordingSuggester(
        private val answers: Map<String, String>,
        private val configured: Boolean = true,
    ) : CategorySuggester {
        val requests = mutableListOf<CategorySuggestionRequest>()
        override suspend fun isConfigured() = configured
        override suspend fun suggest(request: CategorySuggestionRequest): Result<Map<String, CategoryId>> {
            requests += request
            return Result.success(request.entries.mapNotNull { entry -> answers[entry.key]?.let { entry.key to it } }.toMap())
        }
    }

    private suspend fun preview(
        suggester: CategorySuggester,
        database: OmniFlowDatabase = seed(),
    ): ImportPreviewState = previewStates(suggester, database).last()

    private suspend fun previewStates(
        suggester: CategorySuggester,
        database: OmniFlowDatabase = seed(),
    ): List<ImportPreviewState> = workflow(database, suggester)
        .preview(ImportRequest("ledger", "美团.csv", BILL.encodeToByteArray(), ImportFormat.MEITUAN))
        .toList()
        .map { it.getOrThrow() }

    private fun workflow(database: OmniFlowDatabase, suggester: CategorySuggester) = SqlDelightImportWorkflow(
        sessions = SqlDelightImportSessionRepository(database),
        commits = SqlDelightTransactionRepository(database),
        accounts = SqlDelightAccountRepository(database),
        categories = SqlDelightCategoryRepository(database),
        rules = SqlDelightRuleRepository(database),
        categoryMemory = SqlDelightCategoryMemoryRepository(database),
        dedupe = SqlDelightTransactionDedupeRepository(database),
        suggester = suggester,
    )

    private fun seed(): OmniFlowDatabase = createJvmDatabase().apply {
        ledgerQueries.insertLedger("ledger", "日常", null, 1, 1)
        accountQueries.insertAccount("account", "现金", "CASH", "banknote", null, null, 0, 1, 1, 1)
        categoryQueries.insertCategory("c-food", "ledger", null, "餐饮", "utensils", "EXPENSE", 1, 1)
        categoryQueries.insertCategory("c-transit", "ledger", null, "交通", "bus", "EXPENSE", 1, 1)
        categoryQueries.insertCategory("c-salary", "ledger", null, "工资", "banknote", "INCOME", 1, 1)
    }

    private companion object {
        // 美团账单没有平台分类字段，内置关键词映射一条都命中不了——正好是 AI 要补的场景
        val BILL = """
            交易创建时间,交易成功时间,实付金额,收/支,支付方式,订单标题,交易单号,备注
            2026-01-05 12:00:00,2026-01-05 12:00:05,35.00,支出,微信,星巴克咖啡,M1,
            2026-01-06 09:10:00,2026-01-06 09:10:05,42.00,支出,微信,星巴克咖啡,M2,
            2026-01-07 08:00:00,2026-01-07 08:00:05,12.00,支出,微信,滴滴出行,M3,
        """.trimIndent()
    }
}
