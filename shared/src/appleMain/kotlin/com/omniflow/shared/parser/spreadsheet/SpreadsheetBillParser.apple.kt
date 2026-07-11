package com.omniflow.shared.parser.spreadsheet

import com.omniflow.shared.parser.ImportFormat
import com.omniflow.shared.parser.RawTransaction
import com.omniflow.shared.domain.model.Money
import com.omniflow.shared.domain.model.TransactionType
import kotlinx.datetime.Instant

interface AppleSpreadsheetParser {
    fun parse(format: ImportFormat, bytes: ByteArray): AppleSpreadsheetParseResult
}

data class AppleSpreadsheetParseResult(
    val rows: List<RawTransaction>,
    val error: String? = null,
)

object AppleSpreadsheetParserBridge {
    private var parser: AppleSpreadsheetParser? = null

    fun install(parser: AppleSpreadsheetParser) {
        this.parser = parser
    }

    fun uninstall() {
        parser = null
    }

    internal fun parse(format: ImportFormat, bytes: ByteArray): List<RawTransaction> =
        parser?.parse(format, bytes)?.let { result ->
            result.error?.let(::error)
            result.rows
        } ?: error("Apple 表格解析器尚未安装")
}

object AppleRawTransactionFactory {
    fun create(
        formatName: String,
        occurredAtMillis: Long,
        amountMinor: Long,
        typeName: String?,
        excluded: Boolean,
        accountName: String?,
        note: String?,
        externalId: String?,
        sourceCategory: String?,
    ): RawTransaction = RawTransaction(
        format = ImportFormat.valueOf(formatName),
        occurredAt = Instant.fromEpochMilliseconds(occurredAtMillis),
        amount = Money(amountMinor),
        type = typeName?.let(TransactionType::valueOf),
        isExcluded = excluded,
        accountName = accountName,
        note = note,
        externalId = externalId,
        sourceCategory = sourceCategory,
    )
}

actual object SpreadsheetBillParser {
    actual fun parse(format: ImportFormat, bytes: ByteArray): Result<List<RawTransaction>> = runCatching {
        AppleSpreadsheetParserBridge.parse(format, bytes)
    }
}
