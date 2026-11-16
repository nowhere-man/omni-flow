package com.omniflow.core.domain.model

/**
 * 账户按类型分组聚合结果。
 */
data class AccountGroup(
    val type: AccountType,
    val accounts: List<Account>,
    val subtotal: Money,
)

/**
 * 单一角色的授信额度与占比份额。
 */
data class RoleCreditPortion(
    val role: AccountRole,
    val limit: Money,
    val ratio: Float,
    val percentage: Int,
)

/**
 * 家庭授信总储备及三方构成汇总。
 */
data class CreditBufferSummary(
    val totalLimit: Money,
    val husband: RoleCreditPortion,
    val wife: RoleCreditPortion,
    val family: RoleCreditPortion,
) {
    fun portionFor(role: AccountRole): RoleCreditPortion = when (role) {
        AccountRole.HUSBAND -> husband
        AccountRole.WIFE -> wife
        AccountRole.FAMILY -> family
    }

    val portions: List<RoleCreditPortion> get() = listOf(husband, wife, family)

    companion object {
        val EMPTY = CreditBufferSummary(
            totalLimit = Money.Zero,
            husband = RoleCreditPortion(AccountRole.HUSBAND, Money.Zero, 0f, 0),
            wife = RoleCreditPortion(AccountRole.WIFE, Money.Zero, 0f, 0),
            family = RoleCreditPortion(AccountRole.FAMILY, Money.Zero, 0f, 0),
        )
    }
}

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

    /**
     * 计算丈夫、妻子、家庭公用三方授信储备及各自占比。
     * 各自占比独立计算（各自额度 / 三方总额），当总额度为 0 时比例与百分比均为 0。
     */
    fun calculateCreditPortions(
        husbandLimit: Money,
        wifeLimit: Money,
        familyLimit: Money,
    ): CreditBufferSummary {
        val totalMinor = husbandLimit.minor + wifeLimit.minor + familyLimit.minor
        val (hRatio, wRatio, fRatio) = if (totalMinor > 0) {
            val totalDouble = totalMinor.toDouble()
            Triple(
                (husbandLimit.minor / totalDouble).toFloat(),
                (wifeLimit.minor / totalDouble).toFloat(),
                (familyLimit.minor / totalDouble).toFloat(),
            )
        } else {
            Triple(0f, 0f, 0f)
        }

        val hPercent = kotlin.math.round(hRatio * 100).toInt()
        val wPercent = kotlin.math.round(wRatio * 100).toInt()
        val fPercent = kotlin.math.round(fRatio * 100).toInt()

        return CreditBufferSummary(
            totalLimit = Money(totalMinor),
            husband = RoleCreditPortion(AccountRole.HUSBAND, husbandLimit, hRatio, hPercent),
            wife = RoleCreditPortion(AccountRole.WIFE, wifeLimit, wRatio, wPercent),
            family = RoleCreditPortion(AccountRole.FAMILY, familyLimit, fRatio, fPercent),
        )
    }

    /**
     * 从账户列表提取信用卡并聚合三方授信储备及占比。
     * 仅统计 type == CREDIT_CARD 且未软删除的账户。
     */
    fun calculateCreditBuffer(accounts: List<Account>): CreditBufferSummary {
        val activeCreditCards = accounts.filter { it.type == AccountType.CREDIT_CARD && it.deletedAt == null }
        var hMinor = 0L
        var wMinor = 0L
        var fMinor = 0L
        for (a in activeCreditCards) {
            val limit = a.creditLimit.minor
            when (a.ownerRole) {
                AccountRole.HUSBAND -> hMinor += limit
                AccountRole.WIFE -> wMinor += limit
                AccountRole.FAMILY -> fMinor += limit
            }
        }
        return calculateCreditPortions(
            husbandLimit = Money(hMinor),
            wifeLimit = Money(wMinor),
            familyLimit = Money(fMinor),
        )
    }
}
