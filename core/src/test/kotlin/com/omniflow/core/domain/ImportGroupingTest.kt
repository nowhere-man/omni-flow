package com.omniflow.core.domain

import com.omniflow.core.domain.model.Category
import com.omniflow.core.domain.model.DateRange
import com.omniflow.core.domain.model.ImportCategoryDefaults
import com.omniflow.core.domain.model.ImportDuplicateStatus
import com.omniflow.core.domain.model.ImportGroupMode
import com.omniflow.core.domain.model.ImportItemBucket
import com.omniflow.core.domain.model.ImportPreviewItem
import com.omniflow.core.domain.model.ImportPreviewState
import com.omniflow.core.domain.model.ImportCategoryOrigin
import com.omniflow.core.domain.model.Money
import com.omniflow.core.domain.model.TransactionType
import com.omniflow.core.domain.model.bucket
import com.omniflow.core.domain.model.groupingKey
import com.omniflow.core.domain.model.groups
import com.omniflow.core.domain.model.itemsIn
import com.omniflow.core.domain.model.occurredDateBounds
import com.omniflow.core.domain.model.supportsSourceGrouping
import com.omniflow.core.parser.ImportFormat
import com.omniflow.core.parser.RawTransaction
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ImportGroupingTest {
    @Test
    fun alipayAndJdGroupBySourceCategoryWhileWechatFallsBackToCounterparty() {
        // 支付宝/京东的「交易分类」是真消费分类；微信的「交易类型」是支付方式语义
        // （样例 86 条里 62 条都是「商户消费」），必须退回交易对方。
        assertEquals(
            "餐饮美食",
            raw(ImportFormat.ALIPAY, sourceCategory = "餐饮美食", counterparty = "满座儿餐饮").groupingKey,
        )
        assertEquals(
            "食品酒饮",
            raw(ImportFormat.JD, sourceCategory = "食品酒饮", counterparty = "京东超市").groupingKey,
        )
        assertEquals(
            "江边城外烤全鱼",
            raw(ImportFormat.WECHAT, sourceCategory = "商户消费", counterparty = "江边城外烤全鱼").groupingKey,
        )
        assertEquals(
            "美团外卖",
            raw(ImportFormat.MEITUAN, sourceCategory = null, counterparty = "美团外卖").groupingKey,
        )
        assertNull(raw(ImportFormat.CCB, sourceCategory = null, counterparty = null).groupingKey)
    }

    @Test
    fun groupsCollapseItemsAndSkipSourceGroupingWhenNoKeyAvailable() {
        val state = stateOf(
            item("a", raw(ImportFormat.ALIPAY, sourceCategory = "餐饮美食"), amount = 1000),
            item("b", raw(ImportFormat.ALIPAY, sourceCategory = "餐饮美食"), amount = 2000),
            item("c", raw(ImportFormat.ALIPAY, sourceCategory = "交通出行"), amount = 500),
        )

        val groups = state.groups(ImportGroupMode.SOURCE)
        assertEquals(listOf("餐饮美食", "交通出行"), groups.map { it.label })
        assertEquals(Money(3000), groups.first().expenseTotal)
        assertTrue(state.supportsSourceGrouping)

        val keyless = stateOf(item("d", raw(ImportFormat.CCB, sourceCategory = null, counterparty = null)))
        assertFalse(keyless.supportsSourceGrouping)
        assertEquals(listOf("未归类"), keyless.groups(ImportGroupMode.SOURCE).map { it.label })
    }

    @Test
    fun dateGroupingSortsNewestFirst() {
        val state = stateOf(
            item("a", raw(ImportFormat.CCB, occurredAt = "2026-06-01T02:00:00Z")),
            item("b", raw(ImportFormat.CCB, occurredAt = "2026-06-03T02:00:00Z")),
        )
        val groups = state.groups(ImportGroupMode.DATE, timeZone = TimeZone.UTC)
        assertEquals(listOf("2026-06-03", "2026-06-01"), groups.map { it.key })
    }

    @Test
    fun bucketsSeparateDuplicatesNeutralAndPendingItems() {
        val existing = item("a", raw(ImportFormat.ALIPAY), duplicateStatus = ImportDuplicateStatus.CONFIRMED)
        val suspected = item("b", raw(ImportFormat.ALIPAY), duplicateStatus = ImportDuplicateStatus.SUSPECTED)
        // 「不计收支」在解析后 raw.type 为 null、isExcluded 为 true，enrich 会补上兜底类型
        val neutral = item(
            "c",
            raw(ImportFormat.ALIPAY, type = null, isExcluded = true),
            type = TransactionType.EXPENSE,
            isExcluded = true,
        )
        val pending = item("d", raw(ImportFormat.ALIPAY), categoryId = null)
        val ready = item("e", raw(ImportFormat.ALIPAY), categoryId = "category-1")

        assertEquals(ImportItemBucket.EXISTING, existing.bucket())
        assertEquals(ImportItemBucket.SUSPECTED_DUPLICATE, suspected.bucket())
        assertEquals(ImportItemBucket.NEUTRAL, neutral.bucket())
        assertEquals(ImportItemBucket.PENDING, pending.bucket())
        assertEquals(ImportItemBucket.READY, ready.bucket())
    }

    @Test
    fun totalsIgnoreSkippedAndNonCountableItems() {
        val state = stateOf(
            item("a", raw(ImportFormat.ALIPAY), amount = 1000, categoryId = "category-1"),
            item("b", raw(ImportFormat.ALIPAY), amount = 2000, categoryId = "category-1", isSkipped = true),
            item("c", raw(ImportFormat.ALIPAY), amount = 4000, categoryId = "category-1", isExcluded = true),
        )
        assertEquals(Money(1000), state.expenseTotal)
        assertEquals(Money.Zero, state.incomeTotal)
        assertEquals(1, state.skippedCount)
    }

    @Test
    fun itemsInFiltersByOccurredAtWithInclusiveStartAndExclusiveEnd() {
        val state = stateOf(
            item("before", raw(ImportFormat.CCB, occurredAt = "2026-01-05T00:00:00Z")),
            item("atStart", raw(ImportFormat.CCB, occurredAt = "2026-01-06T00:00:00Z")),
            item("inside", raw(ImportFormat.CCB, occurredAt = "2026-01-06T12:00:00Z")),
            item("atEnd", raw(ImportFormat.CCB, occurredAt = "2026-01-07T00:00:00Z")),
        )
        val range = DateRange(
            startInclusive = Instant.parse("2026-01-06T00:00:00Z"),
            endExclusive = Instant.parse("2026-01-07T00:00:00Z"),
        )

        assertEquals(listOf("atStart", "inside"), state.itemsIn(range).map { it.id })
        assertEquals(listOf("before", "atStart", "inside", "atEnd"), state.itemsIn(null).map { it.id })
    }

    @Test
    fun occurredDateBoundsReturnsMinAndMaxDatesOrNullWhenEmpty() {
        val bounds = stateOf(
            item("a", raw(ImportFormat.CCB, occurredAt = "2026-01-06T12:00:00Z")),
            item("b", raw(ImportFormat.CCB, occurredAt = "2026-01-04T22:00:00Z")),
        ).occurredDateBounds(TimeZone.UTC)
        assertEquals(LocalDate(2026, 1, 4) to LocalDate(2026, 1, 6), bounds)

        assertNull(stateOf().occurredDateBounds(TimeZone.UTC))
    }

    @Test
    fun builtInMappingResolvesCommonSourceCategories() {
        val categories = listOf(
            category("cat-food", "餐饮", TransactionType.EXPENSE),
            category("cat-transit", "交通", TransactionType.EXPENSE),
            category("cat-daily", "日用", TransactionType.EXPENSE),
            category("cat-redpack", "红包", TransactionType.INCOME),
        )

        assertEquals(
            "cat-food",
            ImportCategoryDefaults.defaultCategoryId(
                raw(ImportFormat.ALIPAY, sourceCategory = "餐饮美食"),
                TransactionType.EXPENSE,
                categories,
            ),
        )
        // 京东的「数码电器 生活服务」这种多值分类走包含匹配
        assertEquals(
            "cat-daily",
            ImportCategoryDefaults.defaultCategoryId(
                raw(ImportFormat.JD, sourceCategory = "其他网购"),
                TransactionType.EXPENSE,
                categories,
            ),
        )
        assertEquals(
            "cat-redpack",
            ImportCategoryDefaults.defaultCategoryId(
                raw(ImportFormat.WECHAT, sourceCategory = "微信红包"),
                TransactionType.INCOME,
                categories,
            ),
        )
        // 账本里没有对应分类时不硬塞
        assertNull(
            ImportCategoryDefaults.defaultCategoryId(
                raw(ImportFormat.ALIPAY, sourceCategory = "文化休闲"),
                TransactionType.EXPENSE,
                categories,
            ),
        )
        // 投资理财/信用借还刻意不映射，它们绝大多数是不计收支
        assertNull(
            ImportCategoryDefaults.defaultCategoryName(
                raw(ImportFormat.ALIPAY, sourceCategory = "信用借还"),
                TransactionType.EXPENSE,
            ),
        )
    }

    private fun raw(
        format: ImportFormat,
        sourceCategory: String? = null,
        counterparty: String? = null,
        type: TransactionType? = TransactionType.EXPENSE,
        isExcluded: Boolean = false,
        occurredAt: String = "2026-06-30T02:00:00Z",
    ) = RawTransaction(
        format = format,
        occurredAt = Instant.parse(occurredAt),
        amount = Money(100),
        type = type,
        isExcluded = isExcluded,
        accountName = null,
        note = "备注",
        externalId = null,
        sourceCategory = sourceCategory,
        counterparty = counterparty,
    )

    private fun item(
        id: String,
        raw: RawTransaction,
        amount: Long = 100,
        type: TransactionType? = TransactionType.EXPENSE,
        categoryId: String? = "category-1",
        isExcluded: Boolean = false,
        isSkipped: Boolean = false,
        duplicateStatus: ImportDuplicateStatus = ImportDuplicateStatus.NONE,
    ) = ImportPreviewItem(
        id = id,
        raw = raw.copy(amount = Money(amount)),
        type = type,
        categoryId = categoryId,
        categoryOrigin = ImportCategoryOrigin.NONE,
        accountId = "account-1",
        note = raw.note,
        tags = emptyList(),
        isExcluded = isExcluded,
        isSkipped = isSkipped,
        duplicateStatus = duplicateStatus,
    )

    private fun stateOf(vararg items: ImportPreviewItem) = ImportPreviewState(
        sessionId = "session-1",
        ledgerId = "ledger-1",
        format = ImportFormat.ALIPAY,
        items = items.toList(),
    )

    private fun category(id: String, name: String, type: TransactionType) = Category(
        id = id,
        ledgerId = "ledger-1",
        parentId = null,
        name = name,
        iconKey = null,
        type = type,
    )
}
