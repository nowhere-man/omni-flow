package com.omniflow.shared.parser.csv

import com.omniflow.shared.domain.model.Money
import com.omniflow.shared.domain.model.TransactionType
import com.omniflow.shared.parser.ImportFormat
import com.omniflow.shared.parser.RawTransaction
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant

class CsvBillParser {
    fun parse(format: ImportFormat, text: String): Result<List<RawTransaction>> = runCatching {
        when (format) {
            ImportFormat.ALIPAY -> parseAlipay(text)
            ImportFormat.JD -> parseJd(text)
            ImportFormat.MEITUAN -> parseMeituan(text)
            else -> error("该文件格式不是 CSV 账单")
        }
    }

    private fun parseAlipay(text: String): List<RawTransaction> {
        val table = CsvTableParser.tableAfterHeader(text, "交易时间")
        return table.rows.map { row ->
            val direction = table.value(row, "收/支")
            RawTransaction(
                format = ImportFormat.ALIPAY,
                occurredAt = parseDateTime(table.value(row, "交易时间") ?: error("缺少交易时间")),
                amount = parseMoney(table.value(row, "金额") ?: error("缺少金额")),
                type = transactionType(direction),
                isExcluded = direction == "不计收支",
                accountName = table.value(row, "收/付款方式"),
                note = joinNote(
                    table.value(row, "交易对方"),
                    table.value(row, "商品说明"),
                    table.value(row, "备注"),
                ),
                externalId = table.value(row, "交易订单号"),
                sourceCategory = table.value(row, "交易分类"),
            )
        }
    }

    private fun parseJd(text: String): List<RawTransaction> {
        val table = CsvTableParser.tableAfterHeader(text, "交易时间")
        return table.rows.map { row ->
            val direction = table.value(row, "收/支")
            RawTransaction(
                format = ImportFormat.JD,
                occurredAt = parseDateTime(table.value(row, "交易时间") ?: error("缺少交易时间")),
                amount = parseMoney(table.value(row, "金额") ?: error("缺少金额")),
                type = transactionType(direction),
                isExcluded = direction == "不计收支",
                accountName = table.value(row, "收/付款方式"),
                note = joinNote(
                    table.value(row, "商户名称"),
                    table.value(row, "交易说明"),
                    table.value(row, "备注"),
                ),
                externalId = table.value(row, "交易订单号"),
                sourceCategory = table.value(row, "交易分类"),
            )
        }
    }

    private fun parseMeituan(text: String): List<RawTransaction> {
        val table = CsvTableParser.tableAfterHeader(text, "交易创建时间")
        return table.rows.map { row ->
            val direction = table.value(row, "收/支")
            RawTransaction(
                format = ImportFormat.MEITUAN,
                occurredAt = parseDateTime(
                    table.value(row, "交易成功时间") ?: table.value(row, "交易创建时间") ?: error("缺少交易时间"),
                ),
                amount = parseMoney(table.value(row, "实付金额") ?: error("缺少实付金额")),
                type = transactionType(direction),
                isExcluded = direction == "不计收支",
                accountName = table.value(row, "支付方式"),
                note = joinNote(table.value(row, "订单标题"), table.value(row, "备注")),
                externalId = table.value(row, "交易单号"),
                sourceCategory = null,
            )
        }
    }

    private fun transactionType(direction: String?): TransactionType? = when (direction) {
        "收入" -> TransactionType.INCOME
        "支出" -> TransactionType.EXPENSE
        "不计收支", "中性交易" -> null
        else -> error("无法识别收支类型：${direction.orEmpty()}")
    }

    private fun parseDateTime(value: String): Instant {
        val parts = value.trim().split(' ')
        val date = parts.first().split('-').map(String::toInt)
        val time = parts.getOrElse(1) { "00:00:00" }.split(':').map(String::toInt)
        return LocalDateTime(
            year = date[0],
            monthNumber = date[1],
            dayOfMonth = date[2],
            hour = time[0],
            minute = time[1],
            second = time.getOrElse(2) { 0 },
        ).toInstant(TimeZone.of("Asia/Shanghai"))
    }

    private fun parseMoney(value: String): Money {
        val normalized = value.trim().removePrefix("¥").replace(",", "")
        val negative = normalized.startsWith('-')
        val unsigned = normalized.removePrefix("-")
        val parts = unsigned.split('.', limit = 2)
        val yuan = parts[0].toLong()
        val cents = parts.getOrElse(1) { "" }.padEnd(2, '0').take(2).toLong()
        return Money((yuan * 100 + cents) * if (negative) -1 else 1)
    }

    private fun joinNote(vararg values: String?): String? = values
        .mapNotNull { it?.trim()?.takeIf(String::isNotEmpty)?.takeUnless { value -> value == "/" } }
        .distinct()
        .joinToString(" | ")
        .takeIf(String::isNotEmpty)
}
