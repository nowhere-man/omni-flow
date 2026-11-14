package com.omniflow.core.domain.model

/**
 * 账本周期收支概览聚合结果。
 */
data class LedgerOverview(
    val transactionCount: Int,
    val totalIncome: Money,
    val totalExpense: Money,
) {
    val netBalance: Money get() = totalIncome - totalExpense
}

object LedgerAggregator {

    /**
     * 计算账本的交易笔数与周期收支汇总。
     * 规则：排除已软删除与标记为排除的交易；内部转账（TRANSFER）不计入收入与支出。
     */
    fun calculateOverview(transactions: List<Transaction>): LedgerOverview {
        val active = transactions.filter { it.deletedAt == null && !it.isExcluded }
        var incomeMinor = 0L
        var expenseMinor = 0L
        var count = 0
        for (tx in active) {
            count++
            when (tx.type) {
                TransactionType.INCOME -> incomeMinor += tx.amount.minor
                TransactionType.EXPENSE -> expenseMinor += tx.amount.minor
                TransactionType.TRANSFER -> Unit
            }
        }
        return LedgerOverview(
            transactionCount = count,
            totalIncome = Money(incomeMinor),
            totalExpense = Money(expenseMinor),
        )
    }
}
