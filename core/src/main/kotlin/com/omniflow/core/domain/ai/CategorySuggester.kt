package com.omniflow.core.domain.ai

import com.omniflow.core.domain.model.CategoryId
import com.omniflow.core.domain.model.Money
import com.omniflow.core.domain.model.TransactionType

/** 候选的一级分类；只把名字交给模型，回来再按名字换回 id。 */
data class CategoryOption(val id: CategoryId, val name: String)

/**
 * 一个待判定的来源键。同一份账单里同一个键只出现一次——
 * 一个商户消费十笔也只问一次，请求量和记忆表的粒度就此对齐。
 */
data class CategorySuggestionEntry(
    val key: String,
    val type: TransactionType,
    val counterparty: String?,
    val description: String?,
    val sourceCategory: String?,
    val amount: Money,
)

data class CategorySuggestionRequest(
    val entries: List<CategorySuggestionEntry>,
    val expenseOptions: List<CategoryOption>,
    val incomeOptions: List<CategoryOption>,
) {
    fun optionsFor(type: TransactionType): List<CategoryOption> = when (type) {
        TransactionType.EXPENSE -> expenseOptions
        TransactionType.INCOME -> incomeOptions
    }
}

/**
 * 导入时给条目建议一级分类。实现放在 app 模块（需要平台的 HTTP 和密钥存储），
 * core 只依赖这个接口，没配置或调用失败时导入按原样继续。
 */
interface CategorySuggester {
    /** 用户没填服务地址或没开开关时返回 false，导入流程会整段跳过，不产生任何网络请求。 */
    suspend fun isConfigured(): Boolean

    /** 返回 key -> categoryId。判断不了的键不返回，宁可漏也不要错。 */
    suspend fun suggest(request: CategorySuggestionRequest): Result<Map<String, CategoryId>>
}
