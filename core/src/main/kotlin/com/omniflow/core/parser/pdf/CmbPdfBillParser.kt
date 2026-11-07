package com.omniflow.core.parser.pdf

import com.omniflow.core.domain.model.Money
import com.omniflow.core.domain.model.TransactionType
import com.omniflow.core.parser.ImportFormat
import com.omniflow.core.parser.RawTransaction
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant

/**
 * 招商银行「交易流水」PDF。
 *
 * 招商银行的PDF结构相对简单：
 * - 每页都有固定的表头
 * - 交易记录按行排列，没有跨行
 * - 列位置相对固定，可以直接按X坐标划分
 * - 金额格式统一，包含货币符号
 */
object CmbPdfBillParser {
    fun parse(bytes: ByteArray): Result<List<RawTransaction>> = runCatching {
        val pages = PdfTextExtractor.extract(bytes)
        require(pages.isNotEmpty()) { "无法读取 PDF 内容，请确认文件没有加密" }
        val transactions = pages.flatMap(::parsePage)
        require(transactions.isNotEmpty()) { "未在 PDF 中找到招商银行交易流水，请确认导出的是「交易流水」" }
        transactions
    }

    /** 文件级识别：表头字段齐了才认，避免把别的银行 PDF 也吞进来。 */
    fun matches(bytes: ByteArray): Boolean {
        val text = PdfTextExtractor.extract(bytes)
            .flatten()
            .joinToString("") { it.text }
        return text.contains("招商银行") && text.contains("记账日期") && text.contains("交易金额")
    }

    internal fun parsePage(runs: List<PdfTextRun>): List<RawTransaction> {
        val lines = groupIntoLines(runs)
        val headerIndex = lines.indexOfFirst { line ->
            line.any { it.text.contains("记账日期") || it.text.contains("Date") } &&
                line.any { it.text.contains("交易金额") || it.text.contains("Amount") }
        }
        if (headerIndex < 0) return emptyList()

        val columns = determineColumns(lines[headerIndex])
        if (columns.size < MINIMUM_COLUMNS) return emptyList()

        val records = mutableListOf<RawTransaction>()
        for (line in lines.drop(headerIndex + 1)) {
            if (line.any(::isFooter)) break

            val cells = MutableList(columns.size) { "" }
            for (run in line) {
                val col = nearestColumn(columns, run.centerX)
                cells[col] = (cells[col] + run.text).trim()
            }

            val dateText = cells.getOrNull(COLUMN_DATE)?.takeIf { DATE.matches(it) } ?: continue
            val amountText = cells.getOrNull(COLUMN_AMOUNT)?.takeIf(String::isNotBlank) ?: continue
            val signed = parseAmount(amountText) ?: continue

            val memo = cells.getOrNull(COLUMN_MEMO)?.takeIf(String::isNotBlank)
            val counterparty = cells.getOrNull(COLUMN_COUNTERPARTY)?.takeIf(String::isNotBlank)

            records.add(
                RawTransaction(
                    format = ImportFormat.CMB,
                    occurredAt = parseDateTime(dateText),
                    amount = Money(kotlin.math.abs(signed)),
                    type = if (signed < 0) TransactionType.EXPENSE else TransactionType.INCOME,
                    isExcluded = false,
                    accountName = null,
                    note = listOfNotNull(counterparty, memo).distinct().joinToString(" | ").takeIf(String::isNotEmpty),
                    externalId = null,
                    sourceCategory = memo,
                    counterparty = counterparty ?: memo,
                ),
            )
        }

        return records
    }

    private fun groupIntoLines(runs: List<PdfTextRun>): List<List<PdfTextRun>> {
        val sorted = runs.filter { it.text.isNotBlank() }
            .sortedWith(compareByDescending<PdfTextRun> { it.y }.thenBy { it.x })
        val lines = mutableListOf<MutableList<PdfTextRun>>()
        sorted.forEach { run ->
            val current = lines.lastOrNull()
            if (current != null && kotlin.math.abs(current.first().y - run.y) <= LINE_TOLERANCE) {
                current += run
            } else {
                lines += mutableListOf(run)
            }
        }
        return lines.map { line -> line.sortedBy(PdfTextRun::x) }
    }

    private fun determineColumns(headerLine: List<PdfTextRun>): List<Float> {
        val dateCenter = headerLine.find { it.text.contains("记账日期") || it.text.contains("Date") }?.centerX
        val currencyCenter = headerLine.find { it.text.contains("货币") || it.text.contains("Currency") }?.centerX
        val amountCenter = headerLine.find { it.text.contains("交易金额") || it.text.contains("Amount") }?.centerX
        val balanceCenter = headerLine.find { it.text.contains("联机余额") || it.text.contains("Balance") }?.centerX
        val memoCenter = headerLine.find { it.text.contains("交易摘要") || it.text.contains("Transaction Type") }?.centerX
        val counterpartyCenter = headerLine.find { it.text.contains("对手信息") || it.text.contains("Counter Party") }?.centerX

        return listOfNotNull(dateCenter, currencyCenter, amountCenter, balanceCenter, memoCenter, counterpartyCenter).sorted()
    }

    private fun nearestColumn(columns: List<Float>, center: Float): Int {
        var best = 0
        var bestDistance = Float.MAX_VALUE
        columns.forEachIndexed { index, column ->
            val distance = kotlin.math.abs(column - center)
            if (distance < bestDistance) {
                bestDistance = distance
                best = index
            }
        }
        return best
    }

    private fun parseAmount(value: String): Long? {
        val normalized = value.trim()
            .replace("CNY", "", ignoreCase = true)
            .trim()
        return Money.parseOrNull(normalized)?.minor
    }

    private fun parseDateTime(date: String): Instant {
        val day = date.split('-').map(String::toInt)
        return LocalDateTime(
            year = day[0],
            monthNumber = day[1],
            dayOfMonth = day[2],
            hour = 0,
            minute = 0,
            second = 0,
        ).toInstant(TimeZone.of("Asia/Shanghai"))
    }

    private fun isFooter(run: PdfTextRun): Boolean {
        val text = run.text.trim()
        return text.contains("温馨提示") ||
            text.contains("页/共") ||
            text.contains("END") ||
            text.contains("验真")
    }

    private const val MINIMUM_COLUMNS = 5
    private const val COLUMN_DATE = 0
    private const val COLUMN_CURRENCY = 1
    private const val COLUMN_AMOUNT = 2
    private const val COLUMN_BALANCE = 3
    private const val COLUMN_MEMO = 4
    private const val COLUMN_COUNTERPARTY = 5
    private const val LINE_TOLERANCE = 2f

    private val DATE = Regex("""\d{4}-\d{2}-\d{2}""")
}
