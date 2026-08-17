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
 * 中国银行「交易流水明细清单」PDF。
 *
 * 表格列宽固定、单元格内容会换行（"陕西易通人力资源股份" / "有限公司"），
 * 所以不能按阅读顺序数字段，得按坐标还原表格：先用 y 分行，再拿表头每一列的水平中心分列。
 * 判断列用文字段的中心而不是左边缘——金额是右对齐的，左边缘会把长金额判到隔壁列。
 */
object BocPdfBillParser {
    fun parse(bytes: ByteArray): Result<List<RawTransaction>> = runCatching {
        val pages = PdfTextExtractor.extract(bytes)
        require(pages.isNotEmpty()) { "无法读取 PDF 内容，请确认文件没有加密" }
        val transactions = pages.flatMap(::parsePage)
        require(transactions.isNotEmpty()) { "未在 PDF 中找到中国银行流水明细，请确认导出的是「交易流水明细清单」" }
        transactions
    }

    /** 文件级识别：表头字段齐了才认，避免把别的银行 PDF 也吞进来。 */
    fun matches(bytes: ByteArray): Boolean {
        val text = PdfTextExtractor.extract(bytes)
            .flatten()
            .joinToString("") { it.text }
        return text.contains("中国银行") && text.contains("记账日期") && text.contains("交易名称")
    }

    private fun parsePage(runs: List<PdfTextRun>): List<RawTransaction> {
        val lines = groupIntoLines(runs)
        val headerIndex = lines.indexOfFirst { line ->
            line.any { it.text.contains(HEADER_DATE) } && line.any { it.text.contains(HEADER_NAME) }
        }
        if (headerIndex < 0) return emptyList()
        val columns = columnCenters(lines[headerIndex])
        if (columns.size < MINIMUM_COLUMNS) return emptyList()
        val records = mutableListOf<MutableList<String>>()
        for (line in lines.drop(headerIndex + 1)) {
            // 每页表格下面还有温馨提示和页码，不截断的话会被当成续行接到最后一条记录上
            if (FOOTERS.any { marker -> line.joinToString("") { it.text }.contains(marker) }) break
            val cells = MutableList(columns.size) { "" }
            for (run in line) {
                val column = nearestColumn(columns, run.centerX)
                cells[column] = (cells[column] + run.text).trim()
            }
            if (DATE.matches(cells[COLUMN_DATE])) {
                records += cells
            } else if (records.isNotEmpty() && cells[COLUMN_DATE].isEmpty() && cells.any(String::isNotEmpty)) {
                // 单元格换行的续行：没有日期，把每一列接到上一条记录后面
                val previous = records.last()
                cells.forEachIndexed { index, value -> if (value.isNotEmpty()) previous[index] += value }
            }
        }
        return records.mapNotNull(::toTransaction)
    }

    private fun toTransaction(cells: List<String>): RawTransaction? {
        val date = cells.getOrNull(COLUMN_DATE)?.takeIf { DATE.matches(it) } ?: return null
        val signed = parseMoney(cells.getOrNull(COLUMN_AMOUNT)) ?: return null
        val name = cells.cell(COLUMN_NAME)
        val memo = cells.cell(COLUMN_MEMO)
        val counterparty = cells.cell(COLUMN_COUNTERPARTY)
        return RawTransaction(
            format = ImportFormat.BOC,
            occurredAt = parseDateTime(date, cells.cell(COLUMN_TIME)),
            amount = Money(kotlin.math.abs(signed)),
            // 借记卡流水没有「收/支」列，靠金额正负判断：负数是扣款
            type = if (signed < 0) TransactionType.EXPENSE else TransactionType.INCOME,
            isExcluded = false,
            accountName = null,
            note = listOfNotNull(counterparty, memo, name).distinct().joinToString(" | ").takeIf(String::isNotEmpty),
            externalId = null,
            // 「工资」「转账收入」「结息」这些交易名称就是中行版的交易分类，拿来分组和套默认分类
            sourceCategory = name,
            counterparty = counterparty ?: name,
        )
    }

    /** 占位横线（`----------`）等于空值，别当成备注塞进去。 */
    private fun List<String>.cell(index: Int): String? = getOrNull(index)
        ?.trim()
        ?.trim('-')
        ?.trim()
        ?.takeIf(String::isNotEmpty)

    /** 同一行的文字段 y 完全相同，容差只用来抵消浮点误差；续行在下方 8pt，不能被并进来。 */
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

    private fun columnCenters(header: List<PdfTextRun>): List<Float> = header
        .map(PdfTextRun::centerX)
        .sorted()

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

    private fun parseMoney(value: String?): Long? {
        val normalized = value?.trim()?.replace(",", "")?.takeIf(String::isNotEmpty) ?: return null
        if (!AMOUNT.matches(normalized)) return null
        val negative = normalized.startsWith('-')
        val parts = normalized.removePrefix("-").removePrefix("+").split('.', limit = 2)
        val yuan = parts[0].toLongOrNull() ?: return null
        val cents = parts.getOrElse(1) { "" }.padEnd(2, '0').take(2).toLongOrNull() ?: return null
        val minor = yuan * 100 + cents
        return if (negative) -minor else minor
    }

    private fun parseDateTime(date: String, time: String?): Instant {
        val day = date.split('-').map(String::toInt)
        val clock = TIME.find(time.orEmpty())?.value?.split(':')?.map(String::toInt) ?: listOf(0, 0, 0)
        return LocalDateTime(
            year = day[0],
            monthNumber = day[1],
            dayOfMonth = day[2],
            hour = clock.getOrElse(0) { 0 },
            minute = clock.getOrElse(1) { 0 },
            second = clock.getOrElse(2) { 0 },
        ).toInstant(TimeZone.of("Asia/Shanghai"))
    }

    private const val HEADER_DATE = "记账日期"
    private const val HEADER_NAME = "交易名称"
    private const val MINIMUM_COLUMNS = 6
    private const val COLUMN_DATE = 0
    private const val COLUMN_TIME = 1
    private const val COLUMN_AMOUNT = 3
    private const val COLUMN_NAME = 5
    private const val COLUMN_MEMO = 8
    private const val COLUMN_COUNTERPARTY = 9

    /** 同一行内文字基线的 y 完全一致，2pt 容差只吸收浮点误差。 */
    private const val LINE_TOLERANCE = 2f
    private val FOOTERS = listOf("温馨提示", "页/共", "END")
    private val DATE = Regex("""\d{4}-\d{2}-\d{2}""")
    private val TIME = Regex("""\d{1,2}:\d{2}(:\d{2})?""")
    private val AMOUNT = Regex("""[+-]?\d+(\.\d{1,2})?""")
}
