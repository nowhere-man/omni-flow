package com.omniflow.shared.parser.qingzi

import com.omniflow.shared.domain.model.Money
import com.omniflow.shared.domain.model.TransactionType
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals

class QingziBillParserTest {
    @Test
    fun parsesCoreEntitiesAndAllTransactionsFromFixture() {
        val result = QingziBillParser().parse(fixture("青子记账.json").readText()).getOrThrow()

        assertEquals(2, result.ledgers.size)
        assertEquals(2, result.accounts.size)
        assertEquals(12, result.categories.size)
        assertEquals(2, result.tags.size)
        assertEquals(4_436, result.transactions.size)
        assertEquals(Money(400), result.transactions.first().amount)
        assertEquals(TransactionType.EXPENSE, result.transactions.first().type)
        assertEquals("现金", result.transactions.first().accountName)
    }

    @Test
    fun roundsFloatingPointTailToFen() {
        val import = QingziBillParser().parse(
            """
            {
              "bookJsonString": [],
              "accountJsonString": [],
              "categoryJsonString": [{"identifier":"category","name":"餐饮","type":1}],
              "markJsonString": [],
              "entryJsonString": [{"categoryID":"category","createDate":1,"content":"午餐","value":16.399999999999999,"excludeFromBudget":false}]
            }
            """.trimIndent(),
        ).getOrThrow()

        assertEquals(Money(1_640), import.transactions.single().amount)
    }

    private fun fixture(name: String): File = File("../examples/$name").takeIf(File::exists)
        ?: File("examples/$name")
}
