package com.omniflow.core.data.ai

import com.omniflow.core.domain.ai.CategoryOption
import com.omniflow.core.domain.ai.CategorySuggestionEntry
import com.omniflow.core.domain.model.Money
import com.omniflow.core.domain.model.TransactionType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class OpenAiChatProtocolTest {
    private val options = listOf(
        CategoryOption("c-food", "餐饮"),
        CategoryOption("c-transit", "交通"),
        CategoryOption("c-daily", "日用"),
    )
    private val entries = listOf(
        entry("wechat:party:星巴克", "星巴克"),
        entry("wechat:party:滴滴", "滴滴出行"),
        entry("wechat:party:某超市", "某超市"),
    )

    @Test
    fun appendsChatCompletionsPathOnlyOnce() {
        assertEquals("https://api.deepseek.com/v1/chat/completions", OpenAiChatProtocol.endpoint("https://api.deepseek.com/v1"))
        assertEquals("https://api.deepseek.com/v1/chat/completions", OpenAiChatProtocol.endpoint("https://api.deepseek.com/v1/"))
        assertEquals(
            "https://api.deepseek.com/v1/chat/completions",
            OpenAiChatProtocol.endpoint(" https://api.deepseek.com/v1/chat/completions "),
        )
        assertFailsWith<IllegalArgumentException> { OpenAiChatProtocol.endpoint("api.deepseek.com") }
    }

    @Test
    fun requestCarriesModelCategoriesAndEntriesButNoIds() {
        val body = OpenAiChatProtocol.requestBody("deepseek-chat", entries, options)
        assertTrue(body.contains("\"model\":\"deepseek-chat\""))
        assertTrue(body.contains("\"temperature\":0"))
        assertTrue(body.contains("餐饮"), "候选分类名要进提示词")
        assertTrue(body.contains("星巴克"), "交易对方要进提示词")
        // 分类 id 是本地主键，送出去既没用又是多余的信息泄漏
        assertTrue(!body.contains("c-food"), "分类 id 不应出现在请求里")
    }

    @Test
    fun readsContentAndSurfacesServerError() {
        val ok = """{"choices":[{"message":{"role":"assistant","content":"{\"1\":\"餐饮\"}"}}]}"""
        assertEquals("""{"1":"餐饮"}""", OpenAiChatProtocol.parseContent(ok))

        val failure = assertFailsWith<IllegalStateException> {
            OpenAiChatProtocol.parseContent("""{"error":{"message":"Incorrect API key provided"}}""")
        }
        assertTrue(failure.message!!.contains("Incorrect API key"))

        assertFailsWith<IllegalStateException> { OpenAiChatProtocol.parseContent("<html>502</html>") }
    }

    @Test
    fun parsesPlainFencedAndChattyResponses() {
        val expected = mapOf("wechat:party:星巴克" to "c-food", "wechat:party:滴滴" to "c-transit")
        listOf(
            """{"1":"餐饮","2":"交通"}""",
            "```json\n{\"1\":\"餐饮\",\"2\":\"交通\"}\n```",
            "好的，结果如下：\n{\"1\":\"餐饮\",\"2\":\"交通\"}\n希望有帮助。",
        ).forEach { content ->
            assertEquals(expected, OpenAiChatProtocol.parseSuggestions(content, entries, options), content)
        }
    }

    @Test
    fun dropsInventedCategoriesAndOutOfRangeIndexes() {
        // 「宠物」不在候选表里，7 号条目不存在——两者都必须丢掉，
        // 否则模型编的分类会直接落进用户账本
        val content = """{"1":"宠物","2":"交通","7":"餐饮","x":"日用"}"""
        assertEquals(
            mapOf("wechat:party:滴滴" to "c-transit"),
            OpenAiChatProtocol.parseSuggestions(content, entries, options),
        )
    }

    @Test
    fun toleratesWhitespaceAndCaseInCategoryNames() {
        assertEquals(
            mapOf("wechat:party:星巴克" to "c-food"),
            OpenAiChatProtocol.parseSuggestions("""{"1":" 餐饮 "}""", entries, options),
        )
    }

    @Test
    fun returnsNothingWhenResponseHasNoJson() {
        assertEquals(emptyMap(), OpenAiChatProtocol.parseSuggestions("我无法判断这些交易。", entries, options))
    }

    private fun entry(key: String, counterparty: String) = CategorySuggestionEntry(
        key = key,
        type = TransactionType.EXPENSE,
        counterparty = counterparty,
        description = null,
        sourceCategory = null,
        amount = Money(3_500),
    )
}
