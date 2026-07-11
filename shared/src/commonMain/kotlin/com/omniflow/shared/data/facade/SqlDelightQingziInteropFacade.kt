package com.omniflow.shared.data.facade

import com.omniflow.shared.db.OmniFlowDatabase
import com.omniflow.shared.domain.facade.QingziInteropFacade
import com.omniflow.shared.domain.model.QingziExportRequest
import com.omniflow.shared.domain.model.QingziExportResult
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

class SqlDelightQingziInteropFacade(
    private val database: OmniFlowDatabase,
) : QingziInteropFacade {
    override suspend fun export(request: QingziExportRequest): Result<QingziExportResult> = runCatching {
        val queries = database.backupQueries
        val transactions = queries.allTransactionsForBackup().executeAsList()
            .filter { it.deleted_at == null }
            .filter { request.transactionIds.isEmpty() || it.id in request.transactionIds }
            .filter { row -> request.dateRange?.let { range ->
                row.occurred_at >= range.startInclusive.toEpochMilliseconds() &&
                    row.occurred_at < range.endExclusive.toEpochMilliseconds()
            } ?: true }
        val transactionIds = transactions.mapTo(mutableSetOf()) { it.id }
        val ledgerIds = transactions.mapTo(mutableSetOf()) { it.ledger_id }
        val accountIds = transactions.mapTo(mutableSetOf()) { it.account_id }
        val categoryIds = transactions.mapTo(mutableSetOf()) { it.category_id }
        val transactionTags = queries.allTransactionTagsForBackup().executeAsList()
            .filter { it.transaction_id in transactionIds }
        val tagIds = transactionTags.mapTo(mutableSetOf()) { it.tag_id }
        val tagsByTransaction = transactionTags.groupBy { it.transaction_id }

        val ledgers = queries.allLedgersForBackup().executeAsList()
            .filter { it.deleted_at == null || it.id in ledgerIds }
        val accounts = queries.allAccountsForBackup().executeAsList()
            .filter { it.deleted_at == null || it.id in accountIds }
        val categories = queries.allCategoriesForBackup().executeAsList()
            .filter { it.deleted_at == null || it.id in categoryIds }
        val tags = queries.allTagsForBackup().executeAsList()
            .filter { it.deleted_at == null || it.id in tagIds }

        val payload = JsonObject(
            mapOf(
                "bookJsonString" to JsonArray(ledgers.map { row -> JsonObject(mapOf(
                    "id" to JsonPrimitive(row.id),
                    "name" to JsonPrimitive(row.name),
                )) }),
                "accountJsonString" to JsonArray(accounts.map { row -> JsonObject(mapOf(
                    "identifier" to JsonPrimitive(row.id),
                    "name" to JsonPrimitive(row.name),
                )) }),
                "categoryJsonString" to JsonArray(categories.map { row -> JsonObject(mapOf(
                    "identifier" to JsonPrimitive(row.id),
                    "name" to JsonPrimitive(row.name),
                    "type" to JsonPrimitive(if (row.type == "INCOME") 0 else 1),
                )) }),
                "markJsonString" to JsonArray(tags.map { row -> JsonObject(mapOf(
                    "id" to JsonPrimitive(row.id),
                    "name" to JsonPrimitive(row.name),
                )) }),
                "entryJsonString" to JsonArray(transactions.map { row -> JsonObject(buildMap {
                    put("id", JsonPrimitive(row.id))
                    put("bookID", JsonPrimitive(row.ledger_id))
                    put("accountID", JsonPrimitive(row.account_id))
                    put("categoryID", JsonPrimitive(row.category_id))
                    put("markIDs", JsonPrimitive(tagsByTransaction[row.id].orEmpty().joinToString(",") { it.tag_id }))
                    put("value", JsonPrimitive(amount(row.amount_minor)))
                    put("createDate", JsonPrimitive(row.occurred_at / 1_000))
                    row.note?.let { put("content", JsonPrimitive(it)) }
                    put("excludeFromBudget", JsonPrimitive(row.is_excluded != 0L))
                }) }),
            ),
        ).toString()
        val unmappedCount = transactions.count { it.external_source != null || it.external_id != null }
        QingziExportResult(
            payload = payload,
            exportedTransactions = transactions.size,
            warnings = if (unmappedCount == 0) emptyList() else listOf(
                "$unmappedCount 条交易的来源平台或外部订单号在青子格式中无对应字段，已跳过",
            ),
        )
    }

    private fun amount(minor: Long): String {
        val absolute = kotlin.math.abs(minor)
        val sign = if (minor < 0) "-" else ""
        return "$sign${absolute / 100}.${(absolute % 100).toString().padStart(2, '0')}"
    }
}
