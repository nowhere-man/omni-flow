package com.omniflow.shared.parser.qingzi

import com.omniflow.shared.domain.model.Money
import com.omniflow.shared.domain.model.TransactionType
import com.omniflow.shared.parser.ImportFormat
import com.omniflow.shared.parser.RawTransaction
import kotlinx.datetime.Instant
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

data class QingziImport(
    val ledgers: List<QingziLedger>,
    val accounts: List<QingziAccount>,
    val categories: List<QingziCategory>,
    val tags: List<QingziTag>,
    val transactions: List<RawTransaction>,
)

data class QingziLedger(val id: String, val name: String)

data class QingziAccount(val id: String, val name: String)

data class QingziCategory(val id: String, val name: String, val type: TransactionType)

data class QingziTag(val id: String, val name: String)

class QingziBillParser(
    private val json: Json = Json { ignoreUnknownKeys = true },
) {
    fun parse(text: String): Result<QingziImport> = runCatching {
        val root = json.parseToJsonElement(text).jsonObject
        val ledgers = root.array("bookJsonString").map {
            QingziLedger(it.jsonObject.string("id"), it.jsonObject.string("name"))
        }
        val accounts = root.array("accountJsonString").map {
            QingziAccount(it.jsonObject.string("identifier"), it.jsonObject.string("name"))
        }
        val categories = root.array("categoryJsonString").map {
            QingziCategory(
                it.jsonObject.string("identifier"),
                it.jsonObject.string("name"),
                it.jsonObject.transactionType("type"),
            )
        }
        val tags = root.array("markJsonString").map {
            QingziTag(it.jsonObject.string("id"), it.jsonObject.string("name"))
        }
        val ledgerNames = ledgers.associate { it.id to it.name }
        val accountNames = accounts.associate { it.id to it.name }
        val categoriesById = categories.associateBy(QingziCategory::id)
        val tagNames = tags.associate { it.id to it.name }

        QingziImport(
            ledgers = ledgers,
            accounts = accounts,
            categories = categories,
            tags = tags,
            transactions = root.array("entryJsonString").map { element ->
                val entry = element.jsonObject
                val category = categoriesById[entry.string("categoryID")]
                    ?: error("交易缺少可识别的分类")
                RawTransaction(
                    format = ImportFormat.QINGZI,
                    occurredAt = Instant.fromEpochSeconds(entry.epochSeconds("createDate")),
                    amount = Money.fromDecimal(entry.decimal("value")),
                    type = category.type,
                    isExcluded = entry.boolean("excludeFromBudget") ?: false,
                    accountName = accountNames[entry.stringOrNull("accountID")] ?: "现金",
                    note = entry.stringOrNull("content"),
                    externalId = null,
                    sourceCategory = category.name,
                    sourceLedgerName = ledgerNames[entry.stringOrNull("bookID")] ?: "默认账本",
                    tags = entry.stringOrNull("markIDs")
                        ?.split(',')
                        ?.mapNotNull { tagNames[it.trim()] }
                        .orEmpty(),
                )
            },
        )
    }

    private fun JsonObject.array(name: String): JsonArray = this[name]?.jsonArray
        ?: error("青子记账文件缺少 $name")

    private fun JsonObject.string(name: String): String = stringOrNull(name)
        ?: error("青子记账字段 $name 为空")

    private fun JsonObject.stringOrNull(name: String): String? = this[name]
        ?.jsonPrimitive
        ?.contentOrNull
        ?.trim()
        ?.takeIf(String::isNotEmpty)

    private fun JsonObject.decimal(name: String): String = this[name]
        ?.jsonPrimitive
        ?.content
        ?: error("青子记账字段 $name 为空")

    private fun JsonObject.epochSeconds(name: String): Long = decimal(name)
        .substringBefore('.')
        .toLong()

    private fun JsonObject.boolean(name: String): Boolean? = this[name]
        ?.jsonPrimitive
        ?.content
        ?.toBooleanStrictOrNull()

    private fun JsonObject.transactionType(name: String): TransactionType = when (decimal(name)) {
        "0" -> TransactionType.INCOME
        "1" -> TransactionType.EXPENSE
        else -> error("无法识别青子记账分类类型")
    }
}

private fun Money.Companion.fromDecimal(value: String): Money {
    val normalized = value.trim()
    val negative = normalized.startsWith('-')
    val unsigned = normalized.removePrefix("-")
    val integerAndFraction = unsigned.split('.', limit = 2)
    var yuan = integerAndFraction[0].toLong()
    val fraction = integerAndFraction.getOrElse(1) { "" }.padEnd(3, '0')
    var cents = fraction.take(2).toLong()
    if (fraction[2] >= '5') {
        cents += 1
        if (cents == 100L) {
            yuan += 1
            cents = 0
        }
    }
    val minor = yuan * 100 + cents
    return Money(if (negative) -minor else minor)
}
