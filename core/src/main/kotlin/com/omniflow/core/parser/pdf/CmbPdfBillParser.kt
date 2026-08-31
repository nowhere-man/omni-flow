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

    private fun parsePage(runs: List<PdfTextRun>): List<RawTransaction> {
        // 找到表头行
        val headerIndex = runs.indexOfFirst { run ->
            run.text.contains("记账日期") || run.text.contains("Date")
        }

        if (headerIndex < 0) return emptyList()

        // 根据表头确定列的位置范围
        val columns = determineColumns(runs, headerIndex)
        if (columns.size < MINIMUM_COLUMNS) return emptyList()

        // 解析交易记录 - 更简单的方法：每行交易记录都是一组连续的文本块
        val records = mutableListOf<RawTransaction>()

        // 找到所有交易行（包含日期的行）
        val transactionLines = mutableListOf<List<PdfTextRun>>()
        var currentLine: MutableList<PdfTextRun>? = null

        for (run in runs) {
            // 跳过页脚信息
            if (isFooter(run)) continue

            // 如果是新的交易日期，开始新行
            if (DATE.matches(run.text)) {
                if (currentLine != null) {
                    transactionLines.add(currentLine)
                }
                currentLine = mutableListOf(run)
            }
            // 继续当前行
            else if (currentLine != null) {
                currentLine.add(run)
            }
        }

        // 添加最后一行
        if (currentLine != null) {
            transactionLines.add(currentLine)
        }

        // 解析每行交易记录
        for (line in transactionLines) {
            val transaction = parseTransactionLine(line, columns)
            if (transaction != null) {
                records.add(transaction)
            }
        }

        return records
    }

    private fun determineColumns(runs: List<PdfTextRun>, headerIndex: Int): List<Float> {
        // 找到包含表头的行
        val headerRuns = runs.subList(
            maxOf(0, headerIndex - 2),
            minOf(runs.size, headerIndex + 3)
        )

        // 找到所有表头文本
        val headerText = headerRuns.joinToString(" ") { it.text }

        // 根据表头内容确定列的大致位置
        val columnCenters = mutableListOf<Float>()

        // 记账日期列
        val dateRun = headerRuns.find { it.text.contains("记账日期") || it.text.contains("Date") }
        dateRun?.let { columnCenters.add(it.centerX) }

        // 货币列
        val currencyRun = headerRuns.find { it.text.contains("货币") || it.text.contains("Currency") }
        currencyRun?.let { columnCenters.add(it.centerX) }

        // 交易金额列
        val amountRun = headerRuns.find { it.text.contains("交易金额") || it.text.contains("Amount") }
        amountRun?.let { columnCenters.add(it.centerX) }

        // 联机余额列
        val balanceRun = headerRuns.find { it.text.contains("联机余额") || it.text.contains("Balance") }
        balanceRun?.let { columnCenters.add(it.centerX) }

        // 交易摘要列
        val memoRun = headerRuns.find { it.text.contains("交易摘要") || it.text.contains("Transaction Type") }
        memoRun?.let { columnCenters.add(it.centerX) }

        // 对手信息列
        val counterpartyRun = headerRuns.find { it.text.contains("对手信息") || it.text.contains("Counter Party") }
        counterpartyRun?.let { columnCenters.add(it.centerX) }

        return columnCenters.sorted()
    }

    private fun findColumn(columns: List<Float>, x: Float): Int {
        return columns.minByOrNull { kotlin.math.abs(it - x) }?.let { columns.indexOf(it) } ?: -1
    }

    private fun parseTransactionLine(line: List<PdfTextRun>, columnCenters: List<Float>): RawTransaction? {
        // 按X坐标排序文本块
        val sortedLine = line.sortedBy { it.x }

        // 提取各列数据
        val date = getColumnText(sortedLine, columnCenters, COLUMN_DATE)
        val currency = getColumnText(sortedLine, columnCenters, COLUMN_CURRENCY)
        val amount = getColumnText(sortedLine, columnCenters, COLUMN_AMOUNT)
        val balance = getColumnText(sortedLine, columnCenters, COLUMN_BALANCE)
        val memo = getColumnText(sortedLine, columnCenters, COLUMN_MEMO)
        val counterparty = getColumnText(sortedLine, columnCenters, COLUMN_COUNTERPARTY)

        // 验证必要字段
        val dateText = date?.takeIf { DATE.matches(it) } ?: return null
        val amountText = amount ?: return null

        // 解析金额
        val signed = parseAmount(amountText) ?: return null

        return RawTransaction(
            format = ImportFormat.CMB,
            occurredAt = parseDateTime(dateText),
            amount = Money(kotlin.math.abs(signed)),
            type = if (signed < 0) TransactionType.EXPENSE else TransactionType.INCOME,
            isExcluded = false,
            accountName = null,
            note = listOfNotNull(counterparty, memo).distinct().joinToString(" | ").takeIf { it.isNotEmpty() },
            externalId = null,
            sourceCategory = memo?.takeIf { it.isNotEmpty() },
            counterparty = counterparty?.takeIf { it.isNotEmpty() } ?: memo?.takeIf { it.isNotEmpty() },
        )
    }

    private fun getColumnText(sortedLine: List<PdfTextRun>, columnCenters: List<Float>, columnIndex: Int): String? {
        if (columnIndex >= columnCenters.size) return null

        val columnCenter = columnCenters[columnIndex]
        val columnText = sortedLine
            .filter { kotlin.math.abs(it.centerX - columnCenter) < COLUMN_TOLERANCE }
            .joinToString("") { it.text.trim() }

        return columnText.takeIf(String::isNotEmpty)
    }

    private fun toTransaction(cells: Map<Int, String>, y: Float): RawTransaction? {
        val date = cells[COLUMN_DATE]?.takeIf { DATE.matches(it) } ?: return null
        val amountText = cells[COLUMN_AMOUNT] ?: return null

        // 解析金额 - 招商银行包含货币符号
        val signed = parseAmount(amountText) ?: return null

        val name = cells[COLUMN_MEMO] ?: ""
        val counterparty = cells[COLUMN_COUNTERPARTY] ?: ""

        return RawTransaction(
            format = ImportFormat.CMB,
            occurredAt = parseDateTime(date),
            amount = Money(kotlin.math.abs(signed)),
            // 负数是支出，正数是收入
            type = if (signed < 0) TransactionType.EXPENSE else TransactionType.INCOME,
            isExcluded = false,
            accountName = null,
            note = listOfNotNull(counterparty, name).distinct().joinToString(" | ").takeIf(String::isNotEmpty),
            externalId = null,
            sourceCategory = name.takeIf(String::isNotEmpty),
            counterparty = counterparty.takeIf(String::isNotEmpty) ?: name,
        )
    }

    private fun parseAmount(value: String): Long? {
        val normalized = value.trim()
            .removePrefix("CNY")  // 移除货币符号
            .removePrefix("¥")    // 移除人民币符号
            .trim()
            .takeIf(String::isNotEmpty) ?: return null

        // 移除千位分隔符
        val clean = normalized.replace(",", "")

        if (!AMOUNT.matches(clean)) return null

        val negative = clean.startsWith('-')
        val parts = clean.removePrefix("-").removePrefix("+").split('.', limit = 2)
        val yuan = parts[0].toLongOrNull() ?: return null
        val cents = parts.getOrElse(1) { "" }.padEnd(2, '0').take(2).toLongOrNull() ?: return null
        val minor = yuan * 100 + cents

        return if (negative) -minor else minor
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
    private const val COLUMN_TOLERANCE = 30f  // 30像素的容差

    private val DATE = Regex("""\d{4}-\d{2}-\d{2}""")
    private val AMOUNT = Regex("""[+-]?\d+(,\d{3})*(\.\d{1,2})?""")
}