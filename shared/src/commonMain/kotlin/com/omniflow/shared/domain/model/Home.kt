package com.omniflow.shared.domain.model

import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

enum class CalendarTransactionFilter { ALL, INCOME, EXPENSE }

data class DateRange(
    val startInclusive: Instant,
    val endExclusive: Instant,
)

data class HomeQuery(
    val scope: LedgerScope,
    val month: DateRange,
    val calendarFilter: CalendarTransactionFilter = CalendarTransactionFilter.ALL,
)

data class TransactionDetailQuery(
    val scope: LedgerScope,
    val date: DateRange,
    val type: TransactionType? = null,
)

data class TransactionListItem(
    val id: TransactionId,
    val ledgerId: LedgerId,
    val ledgerName: String,
    val accountId: AccountId,
    val accountName: String,
    val categoryId: CategoryId,
    val categoryName: String,
    val primaryCategoryName: String,
    val categoryIconKey: String?,
    val amount: Money,
    val type: TransactionType,
    val occurredAt: Instant,
    val note: String?,
    val isExcluded: Boolean,
    val source: TransactionSource?,
) {
    val categoryDisplayName: String
        get() = if (primaryCategoryName == categoryName) categoryName else "$primaryCategoryName-$categoryName"
}

data class TransactionRecordDetail(
    val transaction: Transaction,
    val ledgerName: String,
    val accountName: String,
    val primaryCategoryName: String,
    val secondaryCategoryName: String?,
    val categoryIconKey: String?,
    val tagNames: List<String>,
)

fun Transaction.toRecordDetail(
    ledgers: List<Ledger>,
    accounts: List<Account>,
    categories: List<Category>,
    tags: List<Tag>,
): TransactionRecordDetail {
    val selectedCategory = categories.firstOrNull { it.id == categoryId }
    val primaryCategory = selectedCategory?.parentId
        ?.let { parentId -> categories.firstOrNull { it.id == parentId } }
        ?: selectedCategory
    return TransactionRecordDetail(
        transaction = this,
        ledgerName = ledgers.firstOrNull { it.id == ledgerId }?.name ?: "未知账本",
        accountName = accounts.firstOrNull { it.id == accountId }?.name ?: "未知账户",
        primaryCategoryName = primaryCategory?.name ?: "未分类",
        secondaryCategoryName = selectedCategory?.takeIf { it.parentId != null }?.name,
        categoryIconKey = primaryCategory?.iconKey ?: selectedCategory?.iconKey,
        tagNames = tags.filter { it.id in tagIds }.map { it.name },
    )
}

data class DayTransactionGroup(
    val date: LocalDate,
    val items: List<TransactionListItem>,
    val expenseTotal: Money,
    val incomeTotal: Money,
)

data class CalendarDaySummary(
    val date: LocalDate,
    val expenseTotal: Money,
    val incomeTotal: Money,
)

data class CalendarDisplayAmount(
    val amount: Money,
    val isIncome: Boolean,
)

fun CalendarDaySummary.displayAmount(filter: CalendarTransactionFilter): CalendarDisplayAmount? {
    val display = when (filter) {
        CalendarTransactionFilter.INCOME -> CalendarDisplayAmount(incomeTotal, isIncome = true)
        CalendarTransactionFilter.EXPENSE -> CalendarDisplayAmount(expenseTotal, isIncome = false)
        CalendarTransactionFilter.ALL -> if (incomeTotal >= expenseTotal) {
            CalendarDisplayAmount(incomeTotal - expenseTotal, isIncome = true)
        } else {
            CalendarDisplayAmount(expenseTotal - incomeTotal, isIncome = false)
        }
    }
    return display.takeUnless { it.amount == Money.Zero }
}

fun Money.calendarAmountText(): String {
    val absoluteMinor = kotlin.math.abs(minor)
    val prefix = if (minor < 0) "-" else ""
    if (absoluteMinor <= 100_000) return "$prefix${absoluteMinor / 100}"
    val tenths = absoluteMinor / 10_000
    return "$prefix${tenths / 10}.${tenths % 10}k"
}

fun LocalDate.yearMonthText(): String = "${year.toString().padStart(4, '0')}年${monthNumber.twoDigits()}月"

fun Instant.hourMinuteText(timeZone: TimeZone = TimeZone.currentSystemDefault()): String {
    val value = toLocalDateTime(timeZone)
    return "${value.hour.twoDigits()}:${value.minute.twoDigits()}"
}

fun Instant.transactionDateTimeText(timeZone: TimeZone = TimeZone.currentSystemDefault()): String {
    val value = toLocalDateTime(timeZone)
    return "${value.year.toString().padStart(4, '0')}-${value.monthNumber.twoDigits()}-${value.dayOfMonth.twoDigits()} " +
        "${value.hour.twoDigits()}:${value.minute.twoDigits()}"
}

private fun Int.twoDigits(): String = toString().padStart(2, '0')

data class TransactionSummary(
    val expenseTotal: Money,
    val incomeTotal: Money,
) {
    val netIncome: Money get() = incomeTotal - expenseTotal
}

data class HomeState(
    val scope: LedgerScope,
    val month: DateRange,
    val summary: TransactionSummary,
    val calendar: List<CalendarDaySummary>,
    val groups: List<DayTransactionGroup>,
)

data class TransactionDetailState(
    val scope: LedgerScope,
    val date: DateRange,
    val type: TransactionType?,
    val summary: TransactionSummary,
    val items: List<TransactionListItem>,
)
