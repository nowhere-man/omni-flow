package com.omniflow.shared.domain.model

data class CategoryTemplate(
    val name: String,
    val iconKey: String,
    val type: TransactionType,
)

data class AccountTemplate(
    val name: String,
    val iconKey: String,
)

object SystemDefaults {
    val categoryTemplates = listOf(
        CategoryTemplate("餐饮", "utensils", TransactionType.EXPENSE),
        CategoryTemplate("交通", "bus", TransactionType.EXPENSE),
        CategoryTemplate("日用", "shopping-bag", TransactionType.EXPENSE),
        CategoryTemplate("服务", "wrench", TransactionType.EXPENSE),
        CategoryTemplate("娱乐", "film", TransactionType.EXPENSE),
        CategoryTemplate("医疗", "heart-pulse", TransactionType.EXPENSE),
        CategoryTemplate("旅游", "plane", TransactionType.EXPENSE),
        CategoryTemplate("汽车", "car", TransactionType.EXPENSE),
        CategoryTemplate("住房", "house", TransactionType.EXPENSE),
        CategoryTemplate("数码", "smartphone", TransactionType.EXPENSE),
        CategoryTemplate("服饰", "shirt", TransactionType.EXPENSE),
        CategoryTemplate("工资", "banknote", TransactionType.INCOME),
        CategoryTemplate("理财", "chart-line", TransactionType.INCOME),
        CategoryTemplate("副业", "briefcase-business", TransactionType.INCOME),
        CategoryTemplate("奖金", "trophy", TransactionType.INCOME),
        CategoryTemplate("红包", "gift", TransactionType.INCOME),
    )

    val accountTemplates = listOf(
        AccountTemplate("现金", "banknote"),
        AccountTemplate("支付宝", "wallet-cards"),
        AccountTemplate("微信", "wallet-cards"),
        AccountTemplate("京东", "shopping-bag"),
        AccountTemplate("美团", "shopping-bag"),
        AccountTemplate("抖音", "play"),
        AccountTemplate("建设银行", "landmark"),
    )
}
