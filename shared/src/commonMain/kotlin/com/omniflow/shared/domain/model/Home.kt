package com.omniflow.shared.domain.model

import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate

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
)

data class TransactionListItem(
    val id: TransactionId,
    val ledgerId: LedgerId,
    val ledgerName: String,
    val accountId: AccountId,
    val accountName: String,
    val categoryId: CategoryId,
    val categoryName: String,
    val categoryIconKey: String?,
    val amount: Money,
    val type: TransactionType,
    val occurredAt: Instant,
    val note: String?,
    val isExcluded: Boolean,
)

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
    val summary: TransactionSummary,
    val items: List<TransactionListItem>,
)
