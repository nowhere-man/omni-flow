package com.omniflow.core.data.ai

import com.omniflow.core.domain.ai.CategoryOption
import com.omniflow.core.domain.ai.CategorySuggestionEntry
import com.omniflow.core.domain.model.CategoryId
import com.omniflow.core.domain.model.TransactionType
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * OpenAI `/chat/completions` 的请求拼装和响应解析。
 *
 * 放在 core 而不是 app：这里是唯一需要防模型乱来的地方（编分类、加解释、套代码块），
 * 必须能在 JVM 单测里覆盖。app 那边只剩「把 body POST 出去」这一件事。
 * 用 kotlinx.serialization 而不是 `org.json`——后者在单测里是空壳桩，测不了。
 */
object OpenAiChatProtocol {
    /** 单批条目数。批次太大模型容易漏编号，太小请求次数上去了。 */
    const val BATCH_SIZE = 40

    private val json = Json { ignoreUnknownKeys = true }

    fun endpoint(baseUrl: String): String {
        val trimmed = baseUrl.trim().trimEnd('/')
        require(trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
            "服务地址需要以 http:// 或 https:// 开头"
        }
        return if (trimmed.endsWith(PATH)) trimmed else trimmed + PATH
    }

    fun requestBody(
        model: String,
        entries: List<CategorySuggestionEntry>,
        options: List<CategoryOption>,
    ): String = JsonObject(
        mapOf(
            "model" to JsonPrimitive(model),
            // 分类判断要的是稳定而不是发挥，同一份账单重导两次结果应该一致
            "temperature" to JsonPrimitive(0),
            "messages" to JsonArray(
                listOf(
                    message("system", SYSTEM_PROMPT),
                    message("user", userPrompt(entries, options)),
                ),
            ),
        ),
    ).toString()

    /** 「测试连接」用的最小请求：一次就能验证地址、密钥和模型名三件事。 */
    fun pingBody(model: String): String = JsonObject(
        mapOf(
            "model" to JsonPrimitive(model),
            "temperature" to JsonPrimitive(0),
            "max_tokens" to JsonPrimitive(8),
            "messages" to JsonArray(listOf(message("user", "回复两个字：可用"))),
        ),
    ).toString()

    /** 取出模型回复的正文；服务端返回错误对象时把错误信息抛出来。 */
    fun parseContent(responseBody: String): String {
        val root = runCatching { json.parseToJsonElement(responseBody).jsonObject }
            .getOrElse { error("无法解析响应：${responseBody.take(160)}") }
        root["error"]?.let { failure ->
            val message = runCatching { failure.jsonObject["message"]?.jsonPrimitive?.contentOrNull }.getOrNull()
            error(message ?: failure.toString().take(160))
        }
        return runCatching {
            root.getValue("choices").jsonArray.first().jsonObject
                .getValue("message").jsonObject
                .getValue("content").jsonPrimitive.content
        }.getOrElse { error("响应里没有 choices[0].message.content：${responseBody.take(160)}") }
    }

    /**
     * 把 `{"1":"餐饮","3":"交通"}` 换成 key -> categoryId。
     * 编号越界、分类名不在候选表里的一律丢弃——模型编出来的分类不能落进用户的账本。
     */
    fun parseSuggestions(
        content: String,
        entries: List<CategorySuggestionEntry>,
        options: List<CategoryOption>,
    ): Map<String, CategoryId> {
        val body = jsonObjectOrNull(content) ?: return emptyMap()
        val byName = options.associateBy { it.name.trim().lowercase() }
        return buildMap {
            body.forEach { (index, value) ->
                val entry = index.trim().toIntOrNull()?.let { entries.getOrNull(it - 1) } ?: return@forEach
                val name = (value as? JsonPrimitive)?.contentOrNull?.trim()?.lowercase() ?: return@forEach
                val option = byName[name] ?: return@forEach
                put(entry.key, option.id)
            }
        }
    }

    /** 模型爱套 ```json 围栏，也爱在前后加一句解释；截取最外层的一对花括号。 */
    private fun jsonObjectOrNull(content: String): JsonObject? {
        val start = content.indexOf('{').takeIf { it >= 0 } ?: return null
        val end = content.lastIndexOf('}').takeIf { it > start } ?: return null
        return runCatching { json.parseToJsonElement(content.substring(start, end + 1)).jsonObject }.getOrNull()
    }

    private fun message(role: String, content: String) = JsonObject(
        mapOf("role" to JsonPrimitive(role), "content" to JsonPrimitive(content)),
    )

    private fun userPrompt(entries: List<CategorySuggestionEntry>, options: List<CategoryOption>): String {
        val direction = if (entries.firstOrNull()?.type == TransactionType.INCOME) "收入" else "支出"
        val list = entries.mapIndexed { index, entry ->
            val fields = listOfNotNull(
                entry.counterparty?.trim()?.takeIf(String::isNotEmpty)?.let { "交易对方：$it" },
                entry.description?.trim()?.takeIf(String::isNotEmpty)?.let { "说明：$it" },
                entry.sourceCategory?.trim()?.takeIf(String::isNotEmpty)?.let { "来源分类：$it" },
                "金额：${yuan(entry.amount.minor)}",
            )
            "${index + 1}. ${fields.joinToString("｜")}"
        }
        return buildString {
            appendLine("候选分类：${options.joinToString("、") { it.name }}")
            appendLine()
            appendLine("以下是 ${entries.size} 笔$direction，请为每一笔选择一个候选分类：")
            list.forEach(::appendLine)
        }
    }

    private fun yuan(minor: Long): String {
        val absolute = kotlin.math.abs(minor)
        return "${absolute / 100}.${(absolute % 100).toString().padStart(2, '0')}"
    }

    private const val PATH = "/chat/completions"

    private val SYSTEM_PROMPT = """
        你是记账应用的分类助手。用户会给出候选分类和若干笔交易，你要为每笔交易挑一个最贴切的分类。
        规则：
        1. 只能使用候选分类里出现过的名称，不要创造新分类，不要改写分类名。
        2. 拿不准就不要输出那条编号，宁可漏也不要错。
        3. 只输出一个 JSON 对象，键是交易编号，值是分类名称。不要输出解释、不要用代码块。
        示例输出：{"1":"餐饮","3":"交通"}
    """.trimIndent()
}
