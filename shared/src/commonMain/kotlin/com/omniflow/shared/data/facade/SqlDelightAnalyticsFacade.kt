package com.omniflow.shared.data.facade

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.omniflow.shared.db.OmniFlowDatabase
import com.omniflow.shared.db.TransactionRowsInRange
import com.omniflow.shared.domain.facade.AnalyticsFacade
import com.omniflow.shared.domain.model.AnalyticsDashboardState
import com.omniflow.shared.domain.model.AnalyticsQuery
import com.omniflow.shared.domain.model.CategoryBreakdownItem
import com.omniflow.shared.domain.model.CategoryRankingItem
import com.omniflow.shared.domain.model.CategoryShareItem
import com.omniflow.shared.domain.model.ChartData
import com.omniflow.shared.domain.model.ChartPoint
import com.omniflow.shared.domain.model.DateRange
import com.omniflow.shared.domain.model.LedgerScope
import com.omniflow.shared.domain.model.Money
import com.omniflow.shared.domain.model.StatementMonth
import com.omniflow.shared.domain.model.StatementTable
import com.omniflow.shared.domain.model.TimeGranularity
import com.omniflow.shared.domain.model.TransactionSummary
import com.omniflow.shared.domain.model.TransactionType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.minus
import kotlinx.datetime.toLocalDateTime

class SqlDelightAnalyticsFacade(
    private val database: OmniFlowDatabase,
) : AnalyticsFacade {
    override fun observeDashboard(query: AnalyticsQuery): Flow<Result<AnalyticsDashboardState>> {
        val year = query.range.startInclusive.toLocalDateTime(CHINA_TIME_ZONE).year
        return combine(
            observeRows(query.scope, query.range),
            observeRows(query.scope, yearRange(year)),
        ) { currentRows, yearRows ->
            runCatching {
                AnalyticsDashboardState(
                    query = query,
                    summary = summary(currentRows),
                    ranking = ranking(currentRows, query.rankingType),
                    categoryBreakdowns = categoryBreakdowns(currentRows, query.categoryShareType),
                    yearStatement = statement(yearRows, year),
                )
            }
        }
    }

    override suspend fun statementTable(scope: LedgerScope, year: Int): Result<StatementTable> = runCatching {
        statement(rows(scope, yearRange(year)), year)
    }

    override suspend fun trend(
        scope: LedgerScope,
        range: DateRange,
        granularity: TimeGranularity,
    ): Result<ChartData> = runCatching {
        val points = rows(scope, range).asSequence()
            .filterNot { it.is_excluded != 0L }
            .groupBy { pointStart(Instant.fromEpochMilliseconds(it.occurred_at), granularity) }
            .map { (start, entries) ->
                val totals = summary(entries)
                ChartPoint(start, chartLabel(start, granularity), totals.expenseTotal, totals.incomeTotal)
            }
            .sortedBy(ChartPoint::start)
            .toList()
        ChartData(range, granularity, points)
    }

    private fun observeRows(scope: LedgerScope, range: DateRange): Flow<List<TransactionRowsInRange>> =
        database.transactionQueries.transactionRowsInRange(
            start_inclusive = range.startInclusive.toEpochMilliseconds(),
            end_exclusive = range.endExclusive.toEpochMilliseconds(),
            ledger_filter = scope.ledgerIdOrNull(),
        ).asFlow().mapToList(Dispatchers.Default)

    private fun rows(scope: LedgerScope, range: DateRange): List<TransactionRowsInRange> =
        database.transactionQueries.transactionRowsInRange(
            start_inclusive = range.startInclusive.toEpochMilliseconds(),
            end_exclusive = range.endExclusive.toEpochMilliseconds(),
            ledger_filter = scope.ledgerIdOrNull(),
        ).executeAsList()

    private fun summary(rows: List<TransactionRowsInRange>): TransactionSummary = rows.asSequence()
        .filterNot { it.is_excluded != 0L }
        .fold(TransactionSummary(Money.Zero, Money.Zero)) { total, row ->
            when (TransactionType.valueOf(row.type)) {
                TransactionType.EXPENSE -> total.copy(expenseTotal = total.expenseTotal + Money(row.amount_minor))
                TransactionType.INCOME -> total.copy(incomeTotal = total.incomeTotal + Money(row.amount_minor))
            }
        }

    private fun ranking(rows: List<TransactionRowsInRange>, type: TransactionType): List<CategoryRankingItem> = rows
        .asSequence()
        .filterNot { it.is_excluded != 0L }
        .filter { TransactionType.valueOf(it.type) == type }
        .groupBy { row ->
            CategoryPath(
                categoryId = row.category_id,
                primaryName = row.primary_category_name,
                secondaryName = row.category_name.takeIf { row.category_id != row.primary_category_id },
                iconKey = row.primary_category_icon_key,
            )
        }
        .map { (category, entries) ->
            CategoryRankingItem(
                categoryId = category.categoryId,
                primaryCategoryName = category.primaryName,
                secondaryCategoryName = category.secondaryName,
                iconKey = category.iconKey,
                amount = entries.fold(Money.Zero) { total, row -> total + Money(row.amount_minor) },
            )
        }
        .sortedByDescending(CategoryRankingItem::amount)
        .take(RANKING_LIMIT)
        .toList()

    private fun categoryBreakdowns(
        rows: List<TransactionRowsInRange>,
        type: TransactionType,
    ): List<CategoryBreakdownItem> = rows.asSequence()
        .filterNot { it.is_excluded != 0L }
        .filter { TransactionType.valueOf(it.type) == type }
        .groupBy { PrimaryCategory(it.primary_category_id, it.primary_category_name, it.primary_category_icon_key) }
        .map { (primary, entries) ->
            val secondaries = entries.asSequence()
                .filter { it.category_id != it.primary_category_id }
                .groupBy { CategoryKey(it.category_id, it.category_name, it.category_icon_key) }
                .map { (category, categoryRows) ->
                    CategoryShareItem(
                        categoryId = category.id,
                        categoryName = category.name,
                        iconKey = category.iconKey,
                        amount = categoryRows.fold(Money.Zero) { total, row -> total + Money(row.amount_minor) },
                    )
                }
                .sortedByDescending(CategoryShareItem::amount)
                .toList()
            CategoryBreakdownItem(
                primaryCategoryId = primary.id,
                primaryCategoryName = primary.name,
                iconKey = primary.iconKey,
                amount = entries.fold(Money.Zero) { total, row -> total + Money(row.amount_minor) },
                secondaryCategories = secondaries,
            )
        }
        .sortedByDescending(CategoryBreakdownItem::amount)
        .toList()

    private fun statement(rows: List<TransactionRowsInRange>, year: Int): StatementTable {
        val entries = rows.filterNot { it.is_excluded != 0L }
        val now = Clock.System.now().toLocalDateTime(CHINA_TIME_ZONE)
        val lastMonth = if (year == now.year) now.monthNumber else 12
        val months = (1..lastMonth).map { month ->
            val totals = summary(entries.filter {
                Instant.fromEpochMilliseconds(it.occurred_at).toLocalDateTime(CHINA_TIME_ZONE).monthNumber == month
            })
            StatementMonth(month, totals.expenseTotal, totals.incomeTotal)
        }
        return StatementTable(year, months, summary(entries))
    }

    private fun yearRange(year: Int) = DateRange(
        LocalDate(year, 1, 1).atStartOfDayIn(CHINA_TIME_ZONE),
        LocalDate(year + 1, 1, 1).atStartOfDayIn(CHINA_TIME_ZONE),
    )

    private fun pointStart(instant: Instant, granularity: TimeGranularity): Instant {
        val dateTime = instant.toLocalDateTime(CHINA_TIME_ZONE)
        val date = when (granularity) {
            TimeGranularity.DAY -> dateTime.date
            TimeGranularity.WEEK -> dateTime.date.minus(dateTime.date.dayOfWeek.ordinal, DateTimeUnit.DAY)
            TimeGranularity.MONTH -> LocalDate(dateTime.year, dateTime.monthNumber, 1)
        }
        return date.atStartOfDayIn(CHINA_TIME_ZONE)
    }

    private fun chartLabel(start: Instant, granularity: TimeGranularity): String {
        val date = start.toLocalDateTime(CHINA_TIME_ZONE).date
        return when (granularity) {
            TimeGranularity.DAY -> date.toString()
            TimeGranularity.WEEK -> date.toString()
            TimeGranularity.MONTH -> "${date.year}-${date.monthNumber.toString().padStart(2, '0')}"
        }
    }

    private fun LedgerScope.ledgerIdOrNull(): String? = when (this) {
        LedgerScope.All -> null
        is LedgerScope.Single -> ledgerId
    }

    private data class CategoryPath(
        val categoryId: String,
        val primaryName: String,
        val secondaryName: String?,
        val iconKey: String?,
    )
    private data class PrimaryCategory(val id: String, val name: String, val iconKey: String?)
    private data class CategoryKey(val id: String, val name: String, val iconKey: String?)

    private companion object {
        val CHINA_TIME_ZONE: TimeZone = TimeZone.currentSystemDefault()
        const val RANKING_LIMIT = 10
    }
}
