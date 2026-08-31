package com.omniflow.core.domain.model

import com.omniflow.core.parser.ImportFormat
import com.omniflow.core.parser.RawTransaction
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

enum class ImportGroupMode { SOURCE, DATE }

/**
 * 预览条目所属的分桶，决定它出现在导入预览的哪个筛选下。
 * 一个条目只属于一个桶，优先级从上到下。
 */
enum class ImportItemBucket { EXISTING, SUSPECTED_DUPLICATE, NEUTRAL, PENDING, READY }

data class ImportItemGroup(
    val key: String,
    val label: String,
    val items: List<ImportPreviewItem>,
    val expenseTotal: Money,
    val incomeTotal: Money,
) {
    /** 组内出现过的分类；单个元素说明整组已归到同一分类，null 元素代表还有条目没分类。 */
    val categoryIds: Set<CategoryId?> get() = items.mapTo(mutableSetOf()) { it.categoryId }
}

/**
 * 归类键按来源选：支付宝和京东的「交易分类」是真正的消费分类，适合整组套用；
 * 微信的「交易类型」只有商户消费/转账/红包这种支付方式语义（样例里 62/86 都是「商户消费」），
 * 拿来分组没有意义，退回用交易对方。美团和建行没有分类字段，同样用交易对方。
 */
val RawTransaction.groupingKey: String?
    get() = when (format) {
        // 中行的「交易名称」是工资/结息/跨行转账这类真分类，和支付宝的交易分类同一层意思
        ImportFormat.ALIPAY, ImportFormat.JD, ImportFormat.QINGZI, ImportFormat.BOC ->
            sourceCategory?.trim()?.takeIf(String::isNotEmpty)
        ImportFormat.WECHAT, ImportFormat.MEITUAN, ImportFormat.CCB ->
            counterparty?.trim()?.takeIf(String::isNotEmpty)
    }

val RawTransaction.groupingLabel: String get() = groupingKey ?: "未归类"

fun ImportPreviewItem.bucket(): ImportItemBucket = when {
    duplicateStatus == ImportDuplicateStatus.CONFIRMED -> ImportItemBucket.EXISTING
    duplicateStatus == ImportDuplicateStatus.SUSPECTED -> ImportItemBucket.SUSPECTED_DUPLICATE
    isExcluded && raw.type == null -> ImportItemBucket.NEUTRAL
    requiresTypeSelection || requiresCategorySelection -> ImportItemBucket.PENDING
    else -> ImportItemBucket.READY
}

fun ImportPreviewState.countIn(vararg buckets: ImportItemBucket): Int =
    items.count { it.bucket() in buckets }

/** 条目交易时间是否落在 [range] 内（start 含、end 不含），与导入 commit 的过滤口径一致。 */
fun ImportPreviewItem.occurredIn(range: DateRange): Boolean =
    raw.occurredAt >= range.startInclusive && raw.occurredAt < range.endExclusive

/** 区间内的预览条目；[range] 为 null 时返回全量。 */
fun ImportPreviewState.itemsIn(range: DateRange?): List<ImportPreviewItem> =
    if (range == null) items else items.filter { it.occurredIn(range) }

/** 明细里最早/最晚交易日（按本地时区的自然日）；没有明细时返回 null。 */
fun ImportPreviewState.occurredDateBounds(
    timeZone: TimeZone = TimeZone.currentSystemDefault(),
): Pair<LocalDate, LocalDate>? = items
    .map { it.raw.occurredAt.toLocalDateTime(timeZone).date }
    .takeIf(List<LocalDate>::isNotEmpty)
    ?.let { dates -> dates.min() to dates.max() }

/** 该账单是否有可用的归类键；没有的话界面应默认按日期分组。 */
val ImportPreviewState.supportsSourceGrouping: Boolean
    get() = items.any { it.raw.groupingKey != null }

/**
 * 按分组维度切分条目。[buckets] 为 null 时不过滤。
 * SOURCE 按归类键分组、条数多的在前；DATE 按自然日倒序。
 */
fun ImportPreviewState.groups(
    mode: ImportGroupMode,
    buckets: Set<ImportItemBucket>? = null,
    timeZone: TimeZone = TimeZone.currentSystemDefault(),
): List<ImportItemGroup> {
    val scoped = if (buckets == null) items else items.filter { it.bucket() in buckets }
    return when (mode) {
        ImportGroupMode.SOURCE -> scoped
            .groupBy { it.raw.groupingLabel }
            .map { (label, groupItems) -> groupOf(label, label, groupItems) }
            .sortedWith(compareByDescending<ImportItemGroup> { it.items.size }.thenBy { it.label })

        ImportGroupMode.DATE -> scoped
            .groupBy { it.raw.occurredAt.toLocalDateTime(timeZone).date }
            .map { (date, groupItems) -> groupOf(date.toString(), date.toString(), groupItems) }
            .sortedByDescending { it.key }
    }
}

/** 按日期分组时组头需要真实的 [LocalDate]，key 用的是 ISO 文本。 */
fun ImportItemGroup.dateOrNull(): LocalDate? = runCatching { LocalDate.parse(key) }.getOrNull()

private fun groupOf(key: String, label: String, items: List<ImportPreviewItem>) = ImportItemGroup(
    key = key,
    label = label,
    items = items,
    expenseTotal = items.countableTotal(TransactionType.EXPENSE),
    incomeTotal = items.countableTotal(TransactionType.INCOME),
)

internal fun List<ImportPreviewItem>.countableTotal(type: TransactionType): Money = this
    .filterNot { it.isSkipped || it.isExcluded }
    .filter { it.type == type }
    .fold(Money.Zero) { total, item -> total + item.raw.amount }
