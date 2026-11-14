package com.omniflow.core.domain.rule

import com.omniflow.core.domain.model.CategoryBreakdownItem
import com.omniflow.core.domain.model.CategoryDiffItem
import com.omniflow.core.domain.model.FinancialHealthSummary
import com.omniflow.core.domain.model.FinancialInsight
import com.omniflow.core.domain.model.InsightType
import com.omniflow.core.domain.model.Money
import com.omniflow.core.domain.model.TransactionRankingItem
import com.omniflow.core.domain.model.TransactionSummary
import java.util.Locale

object FinancialInsightEngine {

    fun evaluate(
        currentSummary: TransactionSummary,
        previousSummary: TransactionSummary,
        healthSummary: FinancialHealthSummary,
        categoryDiffs: List<CategoryDiffItem>,
        ranking: List<TransactionRankingItem>,
        categoryBreakdowns: List<CategoryBreakdownItem>,
    ): FinancialInsight {
        val currentExpense = currentSummary.expenseTotal.minor
        val previousExpense = previousSummary.expenseTotal.minor
        val currentIncome = currentSummary.incomeTotal.minor

        // 规则 1：异动归因规则（超支主力发现）
        if (previousExpense > 0L) {
            val totalDiff = currentExpense - previousExpense
            if (totalDiff >= (previousExpense * 0.15).toLong()) {
                val topIncrease = categoryDiffs
                    .filter { it.isIncrease }
                    .maxByOrNull { it.diffAmount }
                if (topIncrease != null && topIncrease.diffAmount.minor >= (totalDiff * 0.40).toLong()) {
                    return FinancialInsight(
                        type = InsightType.ANOMALY_INCREASE,
                        title = "超支异动预警",
                        message = "本期支出较上期增加 ${Money(totalDiff).asFormattedRmb()}，其中 ${topIncrease.categoryName} 增加了 ${topIncrease.diffAmount.asFormattedRmb()}，是主要增加来源。",
                        highlightCategory = topIncrease.categoryName,
                        highlightAmount = topIncrease.diffAmount,
                    )
                }
            }
        }

        // 规则 2：省钱成就规则（节流表彰）
        if (previousExpense > 0L) {
            val totalDecrease = previousExpense - currentExpense
            val savingsRate = healthSummary.savingsRatePercentage
            if (totalDecrease >= (previousExpense * 0.10).toLong() && savingsRate != null && savingsRate >= 25.0) {
                val topSaved = categoryDiffs
                    .filter { it.isDecrease }
                    .maxByOrNull { it.absoluteDiffAmount }
                val msg = if (topSaved != null) {
                    "控制得力！本期较上期少花 ${Money(totalDecrease).asFormattedRmb()}，${topSaved.categoryName} 支出明显缩减，结余表现优秀。"
                } else {
                    "控制得力！本期较上期少花 ${Money(totalDecrease).asFormattedRmb()}，整体支出明显缩减，结余表现优秀。"
                }
                return FinancialInsight(
                    type = InsightType.SAVINGS_ACHIEVEMENT,
                    title = "省钱成就达成",
                    message = msg,
                    highlightCategory = topSaved?.categoryName,
                    highlightAmount = topSaved?.absoluteDiffAmount,
                )
            }
        }

        // 规则 3：单点失衡规则（极端大额预警）
        if (ranking.isNotEmpty() && currentExpense > 0L) {
            val top1 = ranking.first()
            if (top1.amount.minor >= (currentExpense * 0.35).toLong()) {
                val ratio = (top1.amount.minor.toDouble() / currentExpense.toDouble()) * 100.0
                val ratioStr = String.format(Locale.US, "%.1f", ratio)
                return FinancialInsight(
                    type = InsightType.LARGE_EXPENSE_ALERT,
                    title = "极端大额预警",
                    message = "注意：${top1.categoryDisplayName} 发生了一笔 ${top1.amount.asFormattedRmb()} 的大额开支，占到本期总支出的 ${ratioStr}%。",
                    highlightCategory = top1.categoryDisplayName,
                    highlightAmount = top1.amount,
                )
            }
        }

        // 规则 4：分类集中度规则（单一分类主导）
        if (categoryBreakdowns.isNotEmpty() && currentExpense > 0L) {
            val dominant = categoryBreakdowns.first()
            if (dominant.amount.minor > (currentExpense * 0.50).toLong()) {
                val ratio = (dominant.amount.minor.toDouble() / currentExpense.toDouble()) * 100.0
                val ratioStr = String.format(Locale.US, "%.1f", ratio)
                return FinancialInsight(
                    type = InsightType.CONCENTRATED_CATEGORY,
                    title = "单一分类主导",
                    message = "本期消费高度集中在 ${dominant.primaryCategoryName}，占总支出的 ${ratioStr}%，请关注分类构成是否合理。",
                    highlightCategory = dominant.primaryCategoryName,
                    highlightAmount = dominant.amount,
                )
            }
        }

        // 规则 5：赤字预警规则（收不抵支）
        if (currentExpense > currentIncome && currentIncome > 0L) {
            val deficitAmount = Money(currentExpense - currentIncome)
            val ratio = (currentExpense.toDouble() / currentIncome.toDouble()) * 100.0
            val ratioStr = String.format(Locale.US, "%.1f", ratio)
            return FinancialInsight(
                type = InsightType.DEFICIT_WARNING,
                title = "赤字预警",
                message = "当前支出已超出收入 ${deficitAmount.asFormattedRmb()}，收支比达 ${ratioStr}%，建议适度紧缩非刚性开支。",
                highlightAmount = deficitAmount,
            )
        }

        // 规则 6：基础平衡规则（平稳默认态）
        val defaultMessage = when {
            currentExpense == 0L && currentIncome == 0L -> "当前周期暂无记账数据，记一笔试试吧。"
            healthSummary.savingsRatePercentage != null -> {
                val rateStr = String.format(Locale.US, "%.1f", healthSummary.savingsRatePercentage)
                "收支节奏平稳，当前结余率为 ${rateStr}%，日均支出 ${healthSummary.dailyAverageExpense.asFormattedRmb()}。"
            }
            else -> "收支节奏平稳，当前日均支出 ${healthSummary.dailyAverageExpense.asFormattedRmb()}。"
        }

        return FinancialInsight(
            type = InsightType.BALANCED_DEFAULT,
            title = "收支节奏平稳",
            message = defaultMessage,
        )
    }
}
