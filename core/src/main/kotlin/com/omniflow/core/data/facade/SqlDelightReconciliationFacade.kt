package com.omniflow.core.data.facade

import com.omniflow.core.domain.facade.ReconciliationFacade
import com.omniflow.core.domain.model.AccountId
import com.omniflow.core.domain.model.AccountRole
import com.omniflow.core.domain.model.AccountType
import com.omniflow.core.domain.model.Money
import com.omniflow.core.domain.model.MonthlyReconciliationMeta
import com.omniflow.core.domain.model.NetWorthPoint
import com.omniflow.core.domain.model.PeriodReconciliationSnapshot
import com.omniflow.core.domain.model.ReconciliationAnnualReport
import com.omniflow.core.domain.model.ReconciliationEntry
import com.omniflow.core.domain.model.RoleReconciliationSummary
import com.omniflow.core.domain.repository.AccountRepository
import com.omniflow.core.domain.repository.ReconciliationRepository
import com.omniflow.core.domain.util.UuidGenerator
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlin.math.max

class SqlDelightReconciliationFacade(
    private val reconciliationRepository: ReconciliationRepository,
    private val accountRepository: AccountRepository,
    private val ids: UuidGenerator = UuidGenerator(),
    private val now: () -> Instant = { Clock.System.now() },
) : ReconciliationFacade {

    override fun observeAnnualReport(year: Int, role: AccountRole?): Flow<Result<ReconciliationAnnualReport>> = flow {
        val result = runCatching {
            val allEntries: List<ReconciliationEntry> = reconciliationRepository.allEntries()
            val allMetas: Map<String, MonthlyReconciliationMeta> = reconciliationRepository.allMetas().associateBy { m -> m.period }
            val accounts = accountRepository.activeAccounts().associateBy { a -> a.id }

            // Group entries by period
            val entriesByPeriod: Map<String, List<ReconciliationEntry>> = allEntries.groupBy { e -> e.period }
            val allPeriodsSorted: List<String> = (entriesByPeriod.keys + allMetas.keys).toSet().toList().sorted()

            // Pre-calculate snapshots across all chronological periods to compute accurate net savings
            var previousNetAssets = Money.Zero
            val periodSnapshots = mutableMapOf<String, PeriodReconciliationSnapshot>()

            for (p in allPeriodsSorted) {
                val entries: List<ReconciliationEntry> = entriesByPeriod[p] ?: emptyList()
                val meta: MonthlyReconciliationMeta = allMetas[p] ?: MonthlyReconciliationMeta(
                    period = p,
                    husbandIncome = Money.Zero,
                    wifeIncome = Money.Zero,
                    notes = "",
                    updatedAt = now(),
                )

                // Accumulate per role
                var husbandAssetsMinor = 0L
                var husbandLiabMinor = 0L
                var husbandInvestMinor = 0L
                var husbandProfitLossMinor = 0L
                var husbandLimitMinor = 0L

                var wifeAssetsMinor = 0L
                var wifeLiabMinor = 0L
                var wifeInvestMinor = 0L
                var wifeProfitLossMinor = 0L
                var wifeLimitMinor = 0L

                for (e in entries) {
                    val effectiveRole = e.ownerRole
                    val limit = e.creditLimit.minor
                    val debt = e.debt.minor
                    val deposit = e.deposit.minor
                    val invest = e.investment.minor
                    val pl = e.profitLoss.minor

                    if (effectiveRole == AccountRole.WIFE) {
                        wifeAssetsMinor += deposit + invest
                        wifeLiabMinor += debt
                        wifeInvestMinor += invest
                        wifeProfitLossMinor += pl
                        wifeLimitMinor += limit
                    } else {
                        // Husband or Family
                        husbandAssetsMinor += deposit + invest
                        husbandLiabMinor += debt
                        husbandInvestMinor += invest
                        husbandProfitLossMinor += pl
                        husbandLimitMinor += limit
                    }
                }

                val husbandNet = Money(husbandAssetsMinor - husbandLiabMinor)
                val wifeNet = Money(wifeAssetsMinor - wifeLiabMinor)

                val husbandSummary = RoleReconciliationSummary(
                    role = AccountRole.HUSBAND,
                    totalAssets = Money(husbandAssetsMinor),
                    totalLiabilities = Money(husbandLiabMinor),
                    netAssets = husbandNet,
                    income = meta.husbandIncome,
                    investmentValue = Money(husbandInvestMinor),
                    profitLoss = Money(husbandProfitLossMinor),
                    creditLimit = Money(husbandLimitMinor),
                )

                val wifeSummary = RoleReconciliationSummary(
                    role = AccountRole.WIFE,
                    totalAssets = Money(wifeAssetsMinor),
                    totalLiabilities = Money(wifeLiabMinor),
                    netAssets = wifeNet,
                    income = meta.wifeIncome,
                    investmentValue = Money(wifeInvestMinor),
                    profitLoss = Money(wifeProfitLossMinor),
                    creditLimit = Money(wifeLimitMinor),
                )

                val familyTotalAssets = Money(husbandAssetsMinor + wifeAssetsMinor)
                val familyTotalLiab = Money(husbandLiabMinor + wifeLiabMinor)
                val familyNet = familyTotalAssets - familyTotalLiab

                val netSavings = if (previousNetAssets != Money.Zero) {
                    familyNet - previousNetAssets
                } else {
                    meta.totalIncome
                }
                previousNetAssets = familyNet

                val derivedExpense = if (meta.totalIncome.minor > 0 || netSavings.minor > 0) {
                    val diff = meta.totalIncome.minor - netSavings.minor
                    Money(max(0L, diff))
                } else {
                    Money.Zero
                }

                val savingsRate = if (meta.totalIncome.minor > 0) {
                    (netSavings.minor.toDouble() / meta.totalIncome.minor.toDouble()) * 100.0
                } else 0.0

                periodSnapshots[p] = PeriodReconciliationSnapshot(
                    period = p,
                    totalAssets = familyTotalAssets,
                    totalLiabilities = familyTotalLiab,
                    netAssets = familyNet,
                    totalCreditLimit = Money(husbandLimitMinor + wifeLimitMinor),
                    husbandSummary = husbandSummary,
                    wifeSummary = wifeSummary,
                    meta = meta,
                    derivedExpense = derivedExpense,
                    netSavings = netSavings,
                    savingsRatePercent = savingsRate,
                    accountEntries = entries,
                )
            }

            // Target year months 1..12
            val yearPrefix = "$year-"
            val monthlySnapshots = (1..12).map { monthNum ->
                val monthStr = monthNum.toString().padStart(2, '0')
                val periodKey = "$yearPrefix$monthStr"
                periodSnapshots[periodKey] ?: PeriodReconciliationSnapshot(
                    period = periodKey,
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
                    meta = MonthlyReconciliationMeta(
                        period = periodKey,
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
            }

            // Build NetWorthPoints (include year-1-12 as starting anchor if available)
            val prevYearEndKey = "${year - 1}-12"
            val netWorthPoints = mutableListOf<NetWorthPoint>()
            periodSnapshots[prevYearEndKey]?.let { prevEnd ->
                if (prevEnd.netAssets.minor > 0) {
                    val (pts, ptl, ptn) = when (role) {
                        AccountRole.HUSBAND -> Triple(prevEnd.husbandSummary.totalAssets, prevEnd.husbandSummary.totalLiabilities, prevEnd.husbandSummary.netAssets)
                        AccountRole.WIFE -> Triple(prevEnd.wifeSummary.totalAssets, prevEnd.wifeSummary.totalLiabilities, prevEnd.wifeSummary.netAssets)
                        else -> Triple(prevEnd.totalAssets, prevEnd.totalLiabilities, prevEnd.netAssets)
                    }
                    netWorthPoints.add(NetWorthPoint(prevYearEndKey, "${(year - 1) % 100}-12", pts, ptl, ptn))
                }
            }

            for (s in monthlySnapshots) {
                val (sAssets, sLiab, sNet) = when (role) {
                    AccountRole.HUSBAND -> Triple(s.husbandSummary.totalAssets, s.husbandSummary.totalLiabilities, s.husbandSummary.netAssets)
                    AccountRole.WIFE -> Triple(s.wifeSummary.totalAssets, s.wifeSummary.totalLiabilities, s.wifeSummary.netAssets)
                    else -> Triple(s.totalAssets, s.totalLiabilities, s.netAssets)
                }
                val shortLabel = s.period.removePrefix("$year-") + "月"
                netWorthPoints.add(NetWorthPoint(s.period, shortLabel, sAssets, sLiab, sNet))
            }

            // Annual Totals
            val totalAnnualIncome = Money(monthlySnapshots.sumOf { s ->
                when (role) {
                    AccountRole.HUSBAND -> s.husbandSummary.income.minor
                    AccountRole.WIFE -> s.wifeSummary.income.minor
                    else -> s.meta.totalIncome.minor
                }
            })

            val totalAnnualExpense = Money(monthlySnapshots.sumOf { s -> s.derivedExpense.minor })
            val totalAnnualSavings = Money(monthlySnapshots.sumOf { s -> s.netSavings.minor })

            val latestRecordedSnapshot = monthlySnapshots.lastOrNull { s -> s.netAssets.minor > 0 } ?: monthlySnapshots.last()
            val yearEndNetAssets = when (role) {
                AccountRole.HUSBAND -> latestRecordedSnapshot.husbandSummary.netAssets
                AccountRole.WIFE -> latestRecordedSnapshot.wifeSummary.netAssets
                else -> latestRecordedSnapshot.netAssets
            }

            val totalCreditLimit = when (role) {
                AccountRole.HUSBAND -> latestRecordedSnapshot.husbandSummary.creditLimit
                AccountRole.WIFE -> latestRecordedSnapshot.wifeSummary.creditLimit
                else -> latestRecordedSnapshot.totalCreditLimit
            }

            val averageSavingsRate = if (totalAnnualIncome.minor > 0) {
                (totalAnnualSavings.minor.toDouble() / totalAnnualIncome.minor.toDouble()) * 100.0
            } else 0.0

            ReconciliationAnnualReport(
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
        emit(result)
    }

    override fun observePeriodSnapshot(period: String, role: AccountRole?): Flow<Result<PeriodReconciliationSnapshot?>> = flow {
        val result = runCatching {
            val entries = reconciliationRepository.entriesForPeriod(period)
            val meta = reconciliationRepository.metaForPeriod(period) ?: MonthlyReconciliationMeta(
                period = period,
                husbandIncome = Money.Zero,
                wifeIncome = Money.Zero,
                notes = "",
                updatedAt = now(),
            )
            val accounts = accountRepository.activeAccounts().associateBy { it.id }

            var husbandAssetsMinor = 0L
            var husbandLiabMinor = 0L
            var husbandInvestMinor = 0L
            var husbandProfitLossMinor = 0L
            var husbandLimitMinor = 0L

            var wifeAssetsMinor = 0L
            var wifeLiabMinor = 0L
            var wifeInvestMinor = 0L
            var wifeProfitLossMinor = 0L
            var wifeLimitMinor = 0L

            for (e in entries) {
                val effectiveRole = e.ownerRole
                val limit = e.creditLimit.minor
                val debt = e.debt.minor
                val deposit = e.deposit.minor
                val invest = e.investment.minor
                val pl = e.profitLoss.minor

                if (effectiveRole == AccountRole.WIFE) {
                    wifeAssetsMinor += deposit + invest
                    wifeLiabMinor += debt
                    wifeInvestMinor += invest
                    wifeProfitLossMinor += pl
                    wifeLimitMinor += limit
                } else {
                    husbandAssetsMinor += deposit + invest
                    husbandLiabMinor += debt
                    husbandInvestMinor += invest
                    husbandProfitLossMinor += pl
                    husbandLimitMinor += limit
                }
            }

            val husbandSummary = RoleReconciliationSummary(
                role = AccountRole.HUSBAND,
                totalAssets = Money(husbandAssetsMinor),
                totalLiabilities = Money(husbandLiabMinor),
                netAssets = Money(husbandAssetsMinor - husbandLiabMinor),
                income = meta.husbandIncome,
                investmentValue = Money(husbandInvestMinor),
                profitLoss = Money(husbandProfitLossMinor),
                creditLimit = Money(husbandLimitMinor),
            )

            val wifeSummary = RoleReconciliationSummary(
                role = AccountRole.WIFE,
                totalAssets = Money(wifeAssetsMinor),
                totalLiabilities = Money(wifeLiabMinor),
                netAssets = Money(wifeAssetsMinor - wifeLiabMinor),
                income = meta.wifeIncome,
                investmentValue = Money(wifeInvestMinor),
                profitLoss = Money(wifeProfitLossMinor),
                creditLimit = Money(wifeLimitMinor),
            )

            val familyTotalAssets = Money(husbandAssetsMinor + wifeAssetsMinor)
            val familyTotalLiab = Money(husbandLiabMinor + wifeLiabMinor)
            val familyNet = familyTotalAssets - familyTotalLiab

            PeriodReconciliationSnapshot(
                period = period,
                totalAssets = familyTotalAssets,
                totalLiabilities = familyTotalLiab,
                netAssets = familyNet,
                totalCreditLimit = Money(husbandLimitMinor + wifeLimitMinor),
                husbandSummary = husbandSummary,
                wifeSummary = wifeSummary,
                meta = meta,
                derivedExpense = Money.Zero,
                netSavings = Money.Zero,
                savingsRatePercent = 0.0,
                accountEntries = entries,
            )
        }
        emit(result)
    }

    override fun observeAllPeriods(): Flow<List<String>> = flow {
        val periods = reconciliationRepository.allDistinctPeriods()
        emit(periods)
    }

    override suspend fun recordAccountUpdate(
        accountId: AccountId,
        period: String,
        balance: Money,
        availableLimit: Money?,
        profitLoss: Money,
    ): Result<Unit> = runCatching {
        val account = accountRepository.activeAccounts().find { it.id == accountId }
            ?: error("账户不存在")

        // 1. Update live account
        accountRepository.quickUpdateFinance(
            accountId = accountId,
            balance = balance,
            availableLimit = availableLimit,
            profitLoss = profitLoss,
        )

        // 2. Insert audit entry for reconciliation history
        val debt = if (account.type == AccountType.CREDIT_CARD && availableLimit != null) {
            val d = account.creditLimit.minor - availableLimit.minor
            Money(if (d > 0) d else 0L)
        } else if (balance.minor < 0) {
            Money(-balance.minor)
        } else {
            Money.Zero
        }

        val deposit = if (account.type != AccountType.CREDIT_CARD && account.type != AccountType.INVESTMENT && balance.minor > 0) {
            balance
        } else Money.Zero

        val investment = if (account.type == AccountType.INVESTMENT) {
            balance
        } else Money.Zero

        val entry = ReconciliationEntry(
            id = ids.next(),
            accountId = accountId,
            period = period,
            date = now(),
            creditLimit = account.creditLimit,
            availableLimit = availableLimit,
            debt = debt,
            deposit = deposit,
            investment = investment,
            profitLoss = profitLoss,
            ownerRole = account.ownerRole,
            createdAt = now(),
        )

        reconciliationRepository.recordEntry(entry)
    }

    override suspend fun updateMonthlyMeta(
        period: String,
        husbandIncome: Money,
        wifeIncome: Money,
        notes: String,
    ): Result<Unit> = runCatching {
        val meta = MonthlyReconciliationMeta(
            period = period,
            husbandIncome = husbandIncome,
            wifeIncome = wifeIncome,
            notes = notes,
            updatedAt = now(),
        )
        reconciliationRepository.upsertMeta(meta)
    }
}
