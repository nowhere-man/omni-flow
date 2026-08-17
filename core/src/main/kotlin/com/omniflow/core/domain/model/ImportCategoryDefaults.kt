package com.omniflow.core.domain.model

import com.omniflow.core.parser.ImportFormat
import com.omniflow.core.parser.RawTransaction

/**
 * 账单来源分类到默认分类的内置映射，让首次导入就能预填大部分分类。
 *
 * 映射到 [SystemDefaults.categoryTemplates] 里的分类名而不是分类 id：
 * 分类是按账本创建且用户可改名的，按名字解析命中不了就退回「待分类」，
 * 不会把用户自定义的分类体系覆盖掉。
 *
 * 投资理财、信用借还、余额这类不在表里——它们绝大多数是「不计收支」，
 * 会被 enrichPreview 直接跳过，不需要分类。
 */
object ImportCategoryDefaults {
    private val expenseRules = listOf(
        "餐饮" to listOf("餐饮美食", "food", "食品酒饮", "外卖", "美食", "餐饮"),
        "交通" to listOf("交通出行", "打车", "公共交通", "交通"),
        "日用" to listOf("日用百货", "其他网购", "网购", "超市", "日用"),
        "服饰" to listOf("服饰装扮", "服饰", "鞋包"),
        "娱乐" to listOf("文化休闲", "休闲娱乐", "娱乐"),
        "医疗" to listOf("医疗健康", "医疗", "药"),
        "旅游" to listOf("酒店旅游", "旅游", "旅行"),
        "汽车" to listOf("爱车养车", "汽车"),
        "住房" to listOf("家居家装", "充值缴费", "housing", "住房", "水电"),
        "数码" to listOf("数码电器", "数码", "电器"),
        "服务" to listOf("美容美发", "生活服务", "服务"),
    )

    private val incomeRules = listOf(
        "工资" to listOf("工资", "薪资"),
        "红包" to listOf("微信红包", "红包"),
        "理财" to listOf("投资理财", "理财", "收益", "结息", "利息"),
        "奖金" to listOf("奖金", "报销"),
    )

    /** 返回来源分类对应的默认分类名，命中不了返回 null。 */
    fun defaultCategoryName(raw: RawTransaction, type: TransactionType?): String? {
        val source = raw.sourceCategory?.trim()?.takeIf(String::isNotEmpty) ?: return null
        val rules = when (type) {
            TransactionType.INCOME -> incomeRules
            TransactionType.EXPENSE -> expenseRules
            null -> return null
        }
        // 京东的「数码电器 生活服务」这类多值分类靠包含匹配兜底，所以先做一轮精确匹配。
        rules.firstOrNull { (_, keywords) -> keywords.any { it.equals(source, ignoreCase = true) } }
            ?.let { return it.first }
        return rules.firstOrNull { (_, keywords) -> keywords.any { source.contains(it, ignoreCase = true) } }
            ?.first
    }

    /** 在账本已有分类里解析出默认分类；只认一级分类，避免落到别人的二级分类下。 */
    fun defaultCategoryId(
        raw: RawTransaction,
        type: TransactionType?,
        categories: List<Category>,
    ): CategoryId? {
        if (raw.format == ImportFormat.QINGZI) return null // 青子账单自带分类名，交给记忆和规则处理
        val name = defaultCategoryName(raw, type) ?: return null
        return categories.firstOrNull { it.parentId == null && it.type == type && it.name == name }?.id
    }
}
