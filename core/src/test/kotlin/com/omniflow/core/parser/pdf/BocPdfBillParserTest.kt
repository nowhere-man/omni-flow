package com.omniflow.core.parser.pdf

import com.omniflow.core.domain.model.TransactionType
import java.io.File
import org.junit.Assume.assumeTrue
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class BocPdfBillParserTest {
    @Test
    fun extractsPositionedTextForEveryPage() {
        val pages = PdfTextExtractor.extract(fixture().readBytes())
        assertEquals(16, pages.size)
        assertTrue(pages.all { page -> page.any { it.text == "记账日期" } }, "每页都应该解析出表头")
        // 宽度取自 /W 里的真实字宽，四个汉字在 9pt 下正好 36pt；列归属靠它算中心
        val header = pages.first().first { it.text == "记账日期" }
        assertEquals(36f, header.width, 0.5f)
    }

    @Test
    fun detectsFormatFromContent() {
        assertTrue(BocPdfBillParser.matches(fixture().readBytes()))
    }

    /**
     * 清单每页表头自带「行数 / 借方发生数 / 贷方发生数」，拿它当期望值，
     * 既能逐页校验一行都没漏、金额正负判断也没错，又不用把真实账单金额写进测试源码。
     */
    @Test
    fun perPageTotalsMatchTheStatementHeader() {
        val bytes = fixture().readBytes()
        val pages = PdfTextExtractor.extract(bytes)
        val transactions = BocPdfBillParser.parse(bytes).getOrThrow()
        var offset = 0
        pages.forEachIndexed { index, runs ->
            val header = runs.joinToString(" ") { it.text }
            val rows = ROWS.find(header)?.groupValues?.get(1)?.toInt()
            val debit = minorOf(header, "借方发生数")
            val credit = minorOf(header, "贷方发生数")
            assertTrue(rows != null && debit != null && credit != null, "第${index + 1}页表头缺少统计字段")
            val page = transactions.drop(offset).take(rows!!)
            assertEquals(rows, page.size, "第${index + 1}页行数不符")
            assertEquals(
                debit,
                page.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount.minor },
                "第${index + 1}页支出合计与借方发生数不符",
            )
            assertEquals(
                credit,
                page.filter { it.type == TransactionType.INCOME }.sumOf { it.amount.minor },
                "第${index + 1}页收入合计与贷方发生数不符",
            )
            offset += rows
        }
        assertEquals(offset, transactions.size, "解析出的条数多于各页行数之和")
    }

    @Test
    fun keepsWrappedCellsAndSourceCategory() {
        val transactions = BocPdfBillParser.parse(fixture().readBytes()).getOrThrow()
        assertTrue(transactions.all { it.amount.minor > 0 }, "金额必须取绝对值")
        assertTrue(transactions.all { it.sourceCategory != null }, "交易名称是中行版的来源分类，不能为空")
        // 单元格换行的对方账户名要拼回完整公司名，占位横线要当成空值丢掉
        assertTrue(
            transactions.any { (it.counterparty?.length ?: 0) > 10 },
            "跨行的对方账户名没有拼接完整",
        )
        assertTrue(transactions.none { it.note?.contains("-----") == true }, "占位横线不该进备注")
    }

    private fun minorOf(header: String, label: String): Long? = Regex("$label[：:]?\\s*([\\d,]+\\.\\d{2})")
        .find(header)
        ?.groupValues
        ?.get(1)
        ?.replace(",", "")
        ?.split('.')
        ?.let { it[0].toLong() * 100 + it[1].toLong() }

    private fun fixture(): File {
        val name = "中国银行.pdf"
        val file = File("../examples/$name").takeIf(File::exists) ?: File("examples/$name")
        assumeTrue("本地真实账单样例不存在：$name", file.exists())
        return file
    }

    private companion object {
        val ROWS = Regex("""行数[：:]?\s*(\d+)""")
    }
}
