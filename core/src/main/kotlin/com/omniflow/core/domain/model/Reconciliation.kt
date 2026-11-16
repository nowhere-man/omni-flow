package com.omniflow.core.domain.model

import kotlinx.datetime.Instant

enum class AccountRole(val label: String) {
    HUSBAND("丈夫"),
    WIFE("妻子"),
    FAMILY("家庭公用");

    companion object {
        fun fromString(value: String?): AccountRole = when (value?.trim()?.uppercase()) {
            "WIFE", "妻子", "女主人" -> WIFE
            "FAMILY", "家庭", "家庭公用" -> FAMILY
            else -> HUSBAND
        }
    }
}

data class ReconciliationEntry(
    val id: String,
    val accountId: AccountId,
    val period: String, // "YYYY-MM"
    val date: Instant,
    val creditLimit: Money,
    val availableLimit: Money?,
    val debt: Money,
    val deposit: Money,
    val investment: Money,
    val profitLoss: Money,
    val ownerRole: AccountRole,
    val createdAt: Instant,
)

data class MonthlyReconciliationMeta(
    val period: String, // "YYYY-MM"
    val husbandIncome: Money,
    val wifeIncome: Money,
    val notes: String,
    val updatedAt: Instant,
) {
    val totalIncome: Money get() = husbandIncome + wifeIncome
}

data class RoleReconciliationSummary(
    val role: AccountRole,
    val totalAssets: Money,
    val totalLiabilities: Money,
    val netAssets: Money,
    val income: Money,
    val investmentValue: Money,
    val profitLoss: Money,
    val creditLimit: Money,
)

data class PeriodReconciliationSnapshot(
    val period: String,
    val totalAssets: Money,
    val totalLiabilities: Money,
    val netAssets: Money,
    val totalCreditLimit: Money,
    val husbandSummary: RoleReconciliationSummary,
    val wifeSummary: RoleReconciliationSummary,
    val familySummary: RoleReconciliationSummary = RoleReconciliationSummary(
        role = AccountRole.FAMILY,
        totalAssets = Money.Zero,
        totalLiabilities = Money.Zero,
        netAssets = Money.Zero,
        income = Money.Zero,
        investmentValue = Money.Zero,
        profitLoss = Money.Zero,
        creditLimit = Money.Zero,
    ),
    val meta: MonthlyReconciliationMeta,
    val derivedExpense: Money,
    val netSavings: Money,
    val savingsRatePercent: Double,
    val accountEntries: List<ReconciliationEntry> = emptyList(),
)

data class NetWorthPoint(
    val period: String, // "2025-01"
    val label: String,  // "25-01"
    val totalAssets: Money,
    val totalLiabilities: Money,
    val netAssets: Money,
)

data class ReconciliationAnnualReport(
    val year: Int,
    val monthlySnapshots: List<PeriodReconciliationSnapshot>,
    val netWorthPoints: List<NetWorthPoint>,
    val totalAnnualIncome: Money,
    val totalAnnualExpense: Money,
    val totalAnnualSavings: Money,
    val yearEndNetAssets: Money,
    val totalCreditLimit: Money,
    val averageSavingsRate: Double,
)
