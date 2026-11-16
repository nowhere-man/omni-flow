package com.omniflow.core.data.facade

import com.omniflow.core.domain.model.AccountRole
import com.omniflow.core.domain.model.Money
import com.omniflow.core.domain.model.MonthlyReconciliationMeta
import com.omniflow.core.domain.model.NetWorthPoint
import com.omniflow.core.domain.model.PeriodReconciliationSnapshot
import com.omniflow.core.domain.model.ReconciliationAnnualReport
import com.omniflow.core.domain.model.ReconciliationEntry
import com.omniflow.core.domain.model.RoleReconciliationSummary
import kotlinx.datetime.Instant
import kotlin.math.max

internal object ReconciliationCalculator {

    fun computeChronologicalSnapshots(
        allEntries: List<ReconciliationEntry>,
        allMetas: Map<String, MonthlyReconciliationMeta>,
        role: AccountRole?,
        now: () -> Instant,
    ): Map<String, PeriodReconciliationSnapshot> {
        val entriesByPeriod: Map<String, List<ReconciliationEntry>> = allEntries.groupBy { it.period }
        val allPeriodsSorted: List<String> = (entriesByPeriod.keys + allMetas.keys).toSet().toList().sorted()

        var previousNetAssets: Money? = null
        val periodSnapshots = mutableMapOf<String, PeriodReconciliationSnapshot>()

        for (p in allPeriodsSorted) {
            val rawEntries = entriesByPeriod[p] ?: emptyList()
            // A1: Deduplicate entries per account within each period, keeping the latest by date
            val deduplicatedEntries = rawEntries.groupBy { it.accountId }.values.map { list ->
                list.maxBy { it.date }
            }

            val meta = allMetas[p] ?: MonthlyReconciliationMeta(
                period = p,
                husbandIncome = Money.Zero,
                wifeIncome = Money.Zero,
                notes = "",
                updatedAt = now(),
            )

            // A4: Explicitly accumulate HUSBAND, WIFE, and FAMILY separately
            var hAssets = 0L
            var hLiab = 0L
            var hInvest = 0L
            var hPl = 0L
            var hLimit = 0L

            var wAssets = 0L
            var wLiab = 0L
            var wInvest = 0L
            var wPl = 0L
            var wLimit = 0L

            var fAssets = 0L
            var fLiab = 0L
            var fInvest = 0L
            var fPl = 0L
            var fLimit = 0L

            for (e in deduplicatedEntries) {
                val limit = e.creditLimit.minor
                val debt = e.debt.minor
                val deposit = e.deposit.minor
                val invest = e.investment.minor
                val pl = e.profitLoss.minor

                when (e.ownerRole) {
                    AccountRole.HUSBAND -> {
                        hAssets += deposit + invest
                        hLiab += debt
                        hInvest += invest
                        hPl += pl
                        hLimit += limit
                    }
                    AccountRole.WIFE -> {
                        wAssets += deposit + invest
                        wLiab += debt
                        wInvest += invest
                        wPl += pl
                        wLimit += limit
                    }
                    AccountRole.FAMILY -> {
                        fAssets += deposit + invest
                        fLiab += debt
                        fInvest += invest
                        fPl += pl
                        fLimit += limit
                    }
                }
            }

            val husbandSummary = RoleReconciliationSummary(
                role = AccountRole.HUSBAND,
                totalAssets = Money(hAssets),
                totalLiabilities = Money(hLiab),
                netAssets = Money(hAssets - hLiab),
                income = meta.husbandIncome,
                investmentValue = Money(hInvest),
                profitLoss = Money(hPl),
                creditLimit = Money(hLimit),
            )

            val wifeSummary = RoleReconciliationSummary(
                role = AccountRole.WIFE,
                totalAssets = Money(wAssets),
                totalLiabilities = Money(wLiab),
                netAssets = Money(wAssets - wLiab),
                income = meta.wifeIncome,
                investmentValue = Money(wInvest),
                profitLoss = Money(wPl),
                creditLimit = Money(wLimit),
            )

            val familySummary = RoleReconciliationSummary(
                role = AccountRole.FAMILY,
                totalAssets = Money(fAssets),
                totalLiabilities = Money(fLiab),
                netAssets = Money(fAssets - fLiab),
                income = Money.Zero,
                investmentValue = Money(fInvest),
                profitLoss = Money(fPl),
                creditLimit = Money(fLimit),
            )

            // Calculate effective metrics based on filtered role
            val (effectiveAssets, effectiveLiab, effectiveNet, effectiveLimit, effectiveIncome, effectiveEntries) = when (role) {
                AccountRole.HUSBAND -> SnapshotScopeMetrics(
                    assets = husbandSummary.totalAssets,
                    liabilities = husbandSummary.totalLiabilities,
                    netAssets = husbandSummary.netAssets,
                    creditLimit = husbandSummary.creditLimit,
                    income = husbandSummary.income,
                    entries = deduplicatedEntries.filter { it.ownerRole == AccountRole.HUSBAND },
                )
                AccountRole.WIFE -> SnapshotScopeMetrics(
                    assets = wifeSummary.totalAssets,
                    liabilities = wifeSummary.totalLiabilities,
                    netAssets = wifeSummary.netAssets,
                    creditLimit = wifeSummary.creditLimit,
                    income = wifeSummary.income,
                    entries = deduplicatedEntries.filter { it.ownerRole == AccountRole.WIFE },
                )
                AccountRole.FAMILY -> SnapshotScopeMetrics(
                    assets = familySummary.totalAssets,
                    liabilities = familySummary.totalLiabilities,
                    netAssets = familySummary.netAssets,
                    creditLimit = familySummary.creditLimit,
                    income = Money.Zero,
                    entries = deduplicatedEntries.filter { it.ownerRole == AccountRole.FAMILY },
                )
                null -> SnapshotScopeMetrics(
                    assets = Money(hAssets + wAssets + fAssets),
                    liabilities = Money(hLiab + wLiab + fLiab),
                    netAssets = Money((hAssets + wAssets + fAssets) - (hLiab + wLiab + fLiab)),
                    creditLimit = Money(hLimit + wLimit + fLimit),
                    income = meta.totalIncome,
                    entries = deduplicatedEntries,
                )
            }

            // A2: Real derived savings & expenses calculation
            val netSavings = if (previousNetAssets != null) {
                effectiveNet - previousNetAssets!!
            } else {
                effectiveIncome
            }
            previousNetAssets = effectiveNet

            val derivedExpense = if (effectiveIncome.minor > 0 || netSavings.minor > 0) {
                val diff = effectiveIncome.minor - netSavings.minor
                Money(max(0L, diff))
            } else {
                Money.Zero
            }

            val savingsRate = if (effectiveIncome.minor > 0) {
                (netSavings.minor.toDouble() / effectiveIncome.minor.toDouble()) * 100.0
            } else 0.0

            periodSnapshots[p] = PeriodReconciliationSnapshot(
                period = p,
                totalAssets = effectiveAssets,
                totalLiabilities = effectiveLiab,
                netAssets = effectiveNet,
                totalCreditLimit = effectiveLimit,
                husbandSummary = husbandSummary,
                wifeSummary = wifeSummary,
                familySummary = familySummary,
                meta = meta,
                derivedExpense = derivedExpense,
                netSavings = netSavings,
                savingsRatePercent = savingsRate,
                accountEntries = effectiveEntries,
            )
        }

        return periodSnapshots
    }

    fun buildAnnualReport(
        year: Int,
        role: AccountRole?,
        periodSnapshots: Map<String, PeriodReconciliationSnapshot>,
        now: () -> Instant,
    ): ReconciliationAnnualReport {
        val yearPrefix = "$year-"
        val monthlySnapshots = (1..12).map { monthNum ->
            val monthStr = monthNum.toString().padStart(2, '0')
            val periodKey = "$yearPrefix$monthStr"
            periodSnapshots[periodKey] ?: emptySnapshot(periodKey, now)
        }

        val netWorthPoints = mutableListOf<NetWorthPoint>()
        val prevYearEndKey = "${year - 1}-12"
        periodSnapshots[prevYearEndKey]?.let { prevEnd ->
            // A5: Check recorded data rather than positive net assets
            if (isRecordedSnapshot(prevEnd)) {
                netWorthPoints.add(
                    NetWorthPoint(
                        period = prevYearEndKey,
                        label = "${(year - 1) % 100}-12",
                        totalAssets = prevEnd.totalAssets,
                        totalLiabilities = prevEnd.totalLiabilities,
                        netAssets = prevEnd.netAssets,
                    ),
                )
            }
        }

        for (s in monthlySnapshots) {
            val shortLabel = s.period.removePrefix("$year-") + "月"
            netWorthPoints.add(
                NetWorthPoint(
                    period = s.period,
                    label = shortLabel,
                    totalAssets = s.totalAssets,
                    totalLiabilities = s.totalLiabilities,
                    netAssets = s.netAssets,
                ),
            )
        }

        val totalAnnualIncome = Money(monthlySnapshots.sumOf { s ->
            when (role) {
                AccountRole.HUSBAND -> s.husbandSummary.income.minor
                AccountRole.WIFE -> s.wifeSummary.income.minor
                AccountRole.FAMILY -> Money.Zero.minor
                null -> s.meta.totalIncome.minor
            }
        })

        val totalAnnualExpense = Money(monthlySnapshots.sumOf { it.derivedExpense.minor })
        val totalAnnualSavings = Money(monthlySnapshots.sumOf { it.netSavings.minor })

        // A5: Check for recorded snapshot instead of netAssets > 0
        val latestRecordedSnapshot = monthlySnapshots.lastOrNull { isRecordedSnapshot(it) } ?: monthlySnapshots.last()
        val yearEndNetAssets = latestRecordedSnapshot.netAssets
        val totalCreditLimit = latestRecordedSnapshot.totalCreditLimit

        val averageSavingsRate = if (totalAnnualIncome.minor > 0) {
            (totalAnnualSavings.minor.toDouble() / totalAnnualIncome.minor.toDouble()) * 100.0
        } else 0.0

        return ReconciliationAnnualReport(
            year = year,
            monthlySnapshots = monthlySnapshots,
            netWorthPoints = netWorthPoints,
            totalAnnualIncome = totalAnnualIncome,
            totalAnnualExpense = totalAnnualExpense,
            totalAnnualSavings = totalAnnualSavings,
            yearEndNetAssets = yearEndNetAssets,
            totalCreditLimit = totalCreditLimit,
            averageSavingsRate = averageSavingsRate,
        )
    }

    fun isRecordedSnapshot(s: PeriodReconciliationSnapshot): Boolean =
        s.accountEntries.isNotEmpty() || s.meta.husbandIncome.minor > 0 || s.meta.wifeIncome.minor > 0

    fun emptySnapshot(period: String, now: () -> Instant): PeriodReconciliationSnapshot =
        PeriodReconciliationSnapshot(
            period = period,
            totalAssets = Money.Zero,
            totalLiabilities = Money.Zero,
            netAssets = Money.Zero,
            totalCreditLimit = Money.Zero,
            husbandSummary = RoleReconciliationSummary(
                role = AccountRole.HUSBAND,
                totalAssets = Money.Zero,
                totalLiabilities = Money.Zero,
                netAssets = Money.Zero,
                income = Money.Zero,
                investmentValue = Money.Zero,
                profitLoss = Money.Zero,
                creditLimit = Money.Zero,
            ),
            wifeSummary = RoleReconciliationSummary(
                role = AccountRole.WIFE,
                totalAssets = Money.Zero,
                totalLiabilities = Money.Zero,
                netAssets = Money.Zero,
                income = Money.Zero,
                investmentValue = Money.Zero,
                profitLoss = Money.Zero,
                creditLimit = Money.Zero,
            ),
            familySummary = RoleReconciliationSummary(
                role = AccountRole.FAMILY,
                totalAssets = Money.Zero,
                totalLiabilities = Money.Zero,
                netAssets = Money.Zero,
                income = Money.Zero,
                investmentValue = Money.Zero,
                profitLoss = Money.Zero,
                creditLimit = Money.Zero,
            ),
            meta = MonthlyReconciliationMeta(
                period = period,
                husbandIncome = Money.Zero,
                wifeIncome = Money.Zero,
                notes = "",
                updatedAt = now(),
            ),
            derivedExpense = Money.Zero,
            netSavings = Money.Zero,
            savingsRatePercent = 0.0,
            accountEntries = emptyList(),
        )

    private data class SnapshotScopeMetrics(
        val assets: Money,
        val liabilities: Money,
        val netAssets: Money,
        val creditLimit: Money,
        val income: Money,
        val entries: List<ReconciliationEntry>,
    )
}
