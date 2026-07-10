package com.omniflow.shared.parser.spreadsheet

import com.omniflow.shared.domain.model.Money
import com.omniflow.shared.domain.model.TransactionType
import com.omniflow.shared.parser.ImportFormat
import com.omniflow.shared.parser.RawTransaction
import org.apache.poi.ss.usermodel.DataFormatter
import org.apache.poi.ss.usermodel.Row
import org.apache.poi.ss.usermodel.WorkbookFactory
import java.io.ByteArrayInputStream
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDateTime
import java.time.ZoneId

actual object SpreadsheetBillParser {
    actual fun parse(format: ImportFormat, bytes: ByteArray): Result<List<RawTransaction>> = runCatching {
        WorkbookFactory.create(ByteArrayInputStream(bytes)).use { workbook ->
            val rows = workbook.getSheetAt(0).map { row -> values(row) }
            when (format) {
                ImportFormat.WECHAT -> parseWechat(rows)
                ImportFormat.CCB -> parseCcb(rows)
                else -> error("该文件格式不是表格账单")
            }
        }
    }

    private fun parseWechat(rows: List<List<String>>): List<RawTransaction> {
        val headerIndex = rows.indexOfFirst { it.firstOrNull() == "交易时间" }
        require(headerIndex >= 0) { "未找到微信账单明细表头" }
        val header = rows[headerIndex]
        return rows.drop(headerIndex + 1)
            .filter { row -> row.any(String::isNotBlank) }
            .map { row ->
                val direction = value(row, header, "收/支")
                RawTransaction(
                    format = ImportFormat.WECHAT,
                    occurredAt = parseDateTime(value(row, header, "交易时间") ?: error("缺少交易时间")),
                    amount = parseMoney(value(row, header, "金额(元)") ?: error("缺少金额")),
                    type = transactionType(direction),
                    isExcluded = direction == "中性交易" || direction == null,
                    accountName = value(row, header, "支付方式"),
                    note = joinNote(
                        value(row, header, "交易对方"),
                        value(row, header, "商品"),
                        value(row, header, "备注"),
                    ),
                    externalId = value(row, header, "交易单号"),
                    sourceCategory = value(row, header, "交易类型"),
                )
            }
    }

    private fun parseCcb(rows: List<List<String>>): List<RawTransaction> {
        val headerIndex = rows.indexOfFirst { row ->
            row.any { it == "交易日期" } && row.any { it == "交易金额" }
        }
        require(headerIndex >= 0) { "未找到建设银行账单明细表头" }
        val header = rows[headerIndex]
        return rows.drop(headerIndex + 1)
            .filter { row -> row.any(String::isNotBlank) }
            .map { row ->
                val signedAmount = parseMoney(value(row, header, "交易金额") ?: error("缺少交易金额"))
                RawTransaction(
                    format = ImportFormat.CCB,
                    occurredAt = parseCcbDate(value(row, header, "交易日期") ?: error("缺少交易日期")),
                    amount = Money(kotlin.math.abs(signedAmount.minor)),
                    type = if (signedAmount.minor >= 0) TransactionType.INCOME else TransactionType.EXPENSE,
                    isExcluded = false,
                    accountName = null,
                    note = joinNote(
                        value(row, header, "摘要"),
                        value(row, header, "交易地点/附言"),
                        value(row, header, "对方账号与户名"),
                    ),
                    externalId = null,
                    sourceCategory = null,
                )
            }
    }

    private fun values(row: Row): List<String> {
        val formatter = DataFormatter()
        return (0 until row.lastCellNum.coerceAtLeast(0)).map { index ->
            formatter.formatCellValue(row.getCell(index, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL)).trim()
        }
    }

    private fun value(row: List<String>, header: List<String>, name: String): String? = row.getOrNull(header.indexOf(name))
        ?.trim()
        ?.takeIf(String::isNotEmpty)
        ?.takeUnless { it == "/" }

    private fun transactionType(direction: String?): TransactionType? = when (direction) {
        "收入" -> TransactionType.INCOME
        "支出" -> TransactionType.EXPENSE
        "中性交易", null -> null
        else -> error("无法识别微信收支类型：${direction.orEmpty()}")
    }

    private fun parseDateTime(value: String) = LocalDateTime.parse(value.replace(' ', 'T'))
        .atZone(CHINA_TIME_ZONE)
        .toInstant()
        .toEpochMilli()
        .let(kotlinx.datetime.Instant::fromEpochMilliseconds)

    private fun parseCcbDate(value: String) = LocalDateTime.parse("${value.trim()}T00:00:00", CCB_DATE_TIME)
        .atZone(CHINA_TIME_ZONE)
        .toInstant()
        .toEpochMilli()
        .let(kotlinx.datetime.Instant::fromEpochMilliseconds)

    private fun parseMoney(value: String): Money = Money(
        BigDecimal(value.trim().removePrefix("¥").replace(",", ""))
            .movePointRight(2)
            .setScale(0, RoundingMode.HALF_UP)
            .longValueExact(),
    )

    private fun joinNote(vararg values: String?): String? = values
        .mapNotNull { it?.trim()?.takeIf(String::isNotEmpty)?.takeUnless { value -> value == "/" } }
        .distinct()
        .joinToString(" | ")
        .takeIf(String::isNotEmpty)

    private val CHINA_TIME_ZONE: ZoneId = ZoneId.of("Asia/Shanghai")
    private val CCB_DATE_TIME = java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd'T'HH:mm:ss")
}
