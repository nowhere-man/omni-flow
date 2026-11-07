package com.omniflow.core.parser.pdf

import com.omniflow.core.domain.model.Money
import com.omniflow.core.domain.model.TransactionType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CmbPdfBillParserTest {
    @Test
    fun parsePageExtractsTransactionsCorrectly() {
        val runs = listOf(
            // Header at y = 700
            PdfTextRun(x = 50f, y = 700f, width = 40f, text = "记账日期"),
            PdfTextRun(x = 120f, y = 700f, width = 30f, text = "货币"),
            PdfTextRun(x = 180f, y = 700f, width = 40f, text = "交易金额"),
            PdfTextRun(x = 250f, y = 700f, width = 40f, text = "联机余额"),
            PdfTextRun(x = 320f, y = 700f, width = 40f, text = "交易摘要"),
            PdfTextRun(x = 400f, y = 700f, width = 50f, text = "对手信息"),

            // Row 1 (Expense) at y = 680
            PdfTextRun(x = 45f, y = 680f, width = 50f, text = "2024-03-01"),
            PdfTextRun(x = 125f, y = 680f, width = 20f, text = "CNY"),
            PdfTextRun(x = 180f, y = 680f, width = 40f, text = "-150.00"),
            PdfTextRun(x = 250f, y = 680f, width = 40f, text = "5,000.00"),
            PdfTextRun(x = 330f, y = 680f, width = 20f, text = "餐饮"),
            PdfTextRun(x = 410f, y = 680f, width = 30f, text = "麦当劳"),

            // Row 2 (Income) at y = 660
            PdfTextRun(x = 45f, y = 660f, width = 50f, text = "2024-03-02"),
            PdfTextRun(x = 125f, y = 660f, width = 20f, text = "CNY"),
            PdfTextRun(x = 175f, y = 660f, width = 50f, text = "+10,000.00"),
            PdfTextRun(x = 245f, y = 660f, width = 50f, text = "15,000.00"),
            PdfTextRun(x = 330f, y = 660f, width = 20f, text = "工资"),
            PdfTextRun(x = 405f, y = 660f, width = 40f, text = "某某公司"),

            // Footer at y = 640
            PdfTextRun(x = 50f, y = 640f, width = 150f, text = "温馨提示：此证明仅作参考"),
            PdfTextRun(x = 45f, y = 620f, width = 50f, text = "2024-03-03"),
            PdfTextRun(x = 180f, y = 620f, width = 40f, text = "-20.00"),
        )

        val transactions = CmbPdfBillParser.parsePage(runs)
        assertEquals(2, transactions.size)

        val expense = transactions[0]
        assertEquals(Money(15000), expense.amount)
        assertEquals(TransactionType.EXPENSE, expense.type)
        assertEquals("麦当劳 | 餐饮", expense.note)
        assertEquals("麦当劳", expense.counterparty)
        assertEquals("餐饮", expense.sourceCategory)

        val income = transactions[1]
        assertEquals(Money(1000000), income.amount)
        assertEquals(TransactionType.INCOME, income.type)
        assertEquals("某某公司 | 工资", income.note)
        assertEquals("某某公司", income.counterparty)
        assertEquals("工资", income.sourceCategory)
    }

    @Test
    fun returnsEmptyListWhenHeaderMissing() {
        val runs = listOf(
            PdfTextRun(x = 45f, y = 680f, width = 50f, text = "2024-03-01"),
            PdfTextRun(x = 180f, y = 680f, width = 40f, text = "-150.00"),
        )
        val transactions = CmbPdfBillParser.parsePage(runs)
        assertTrue(transactions.isEmpty())
    }
}
