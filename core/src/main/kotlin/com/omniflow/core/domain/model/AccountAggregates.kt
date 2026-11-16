package com.omniflow.core.domain.model

/**
 * 账户按类型分组聚合结果。
 */
data class AccountGroup(
    val type: AccountType,
    val accounts: List<Account>,
    val subtotal: Money,
)

object AccountAggregator {

    /**
     * 将账户按类型分组，并计算每组的余额小计。
     * 过滤已软删除的账户；没有账户的分组不返回。
     */
    fun groupByType(accounts: List<Account>): List<AccountGroup> {
        val active = accounts.filter { it.deletedAt == null }
        return AccountType.entries.mapNotNull { type ->
            val matching = active.filter { it.type == type }
            if (matching.isEmpty()) {
                null
            } else {
                val subtotal = matching.fold(Money.Zero) { acc, a -> acc + a.balance }
                AccountGroup(type = type, accounts = matching, subtotal = subtotal)
            }
        }
    }

    /**
     * 计算净资产汇总（资产、负债、净资产）。
     * 仅统计 includeInTotalAssets 为 true 且未软删除的账户。
     *
     * 全应用唯一口径：
     * - 负债：统一取 account.debt（信用卡基于 creditLimit - availableLimit 或负余额透支；其他账户取负余额透支）。
     * - 资产：信用卡不计入资产（避免授信额度或溢缴款虚增资产），其他账户（储蓄卡、现金、钱包、投资等）正余额计入资产。
     * - 净资产 = 资产 - 负债。
     */
    fun calculateSummary(accounts: List<Account>): AccountSummary {
        val included = accounts.filter { it.includeInTotalAssets && it.deletedAt == null }
        var assetsMinor = 0L
        var liabilitiesMinor = 0L
        for (account in included) {
            val debtMinor = account.debt.minor
            if (debtMinor > 0) {
                liabilitiesMinor += debtMinor
            }
            if (account.type != AccountType.CREDIT_CARD && account.balance.minor > 0) {
                assetsMinor += account.balance.minor
            }
        }
        return AccountSummary(
            assets = Money(assetsMinor),
            liabilities = Money(liabilitiesMinor),
        )
    }

    /**
     * 隐私遮罩：当启用了隐藏金额时，将金额格式化为星号遮罩，否则原样返回 RMB 格式。
     */
    fun formatAmount(amount: Money, masked: Boolean): String {
        return if (masked) "••••••" else amount.asFormattedRmb()
    }
}
