package com.omniflow.shared.data.repository

import com.omniflow.shared.db.OmniFlowDatabase
import com.omniflow.shared.domain.model.ImportDuplicateStatus
import com.omniflow.shared.domain.model.ImportCategoryOrigin
import com.omniflow.shared.domain.model.ImportPreviewEdit
import com.omniflow.shared.domain.model.ImportPreviewItem
import com.omniflow.shared.domain.model.ImportSessionId
import com.omniflow.shared.domain.repository.ImportPreviewSession
import com.omniflow.shared.domain.model.Money
import com.omniflow.shared.domain.model.TransactionType
import com.omniflow.shared.domain.repository.ImportSessionRepository
import com.omniflow.shared.parser.ImportFormat
import com.omniflow.shared.parser.RawTransaction
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

class SqlDelightImportSessionRepository(
    private val database: OmniFlowDatabase,
    private val now: () -> Instant = { Clock.System.now() },
    private val json: Json = Json,
) : ImportSessionRepository {
    override suspend fun create(
        sessionId: ImportSessionId,
        ledgerId: String,
        format: ImportFormat,
        items: List<ImportPreviewItem>,
    ) {
        val timestamp = now().toEpochMilliseconds()
        database.transaction {
            database.importSessionQueries.insertImportSession(
                id = sessionId,
                ledger_id = ledgerId,
                source = format.name,
                created_at = timestamp,
                updated_at = timestamp,
            )
            items.forEach { item ->
                database.importSessionQueries.insertImportPreviewItem(
                    id = item.id,
                    session_id = sessionId,
                    payload = encode(item),
                    is_skipped = if (item.isSkipped) 1L else 0L,
                    created_at = timestamp,
                    updated_at = timestamp,
                )
            }
        }
    }

    override suspend fun state(sessionId: ImportSessionId): ImportPreviewSession? {
        val session = database.importSessionQueries.importSession(sessionId).executeAsOneOrNull() ?: return null
        return ImportPreviewSession(
            id = session.id,
            ledgerId = session.ledger_id,
            format = ImportFormat.valueOf(session.source ?: error("导入会话缺少来源")),
            items = database.importSessionQueries.previewItemsForSession(sessionId)
                .executeAsList()
                .map { row -> decode(row.id, row.payload, row.is_skipped != 0L) },
        )
    }

    override suspend fun updateItem(sessionId: ImportSessionId, edit: ImportPreviewEdit) {
        require(edit.sessionId == sessionId) { "导入会话不匹配" }
        val row = database.importSessionQueries.previewItem(edit.itemId, sessionId).executeAsOneOrNull()
            ?: error("导入预览项不存在")
        val existing = decode(row.id, row.payload, row.is_skipped != 0L)
        require(existing.duplicateStatus != ImportDuplicateStatus.CONFIRMED || edit.isSkipped) {
            "已确认重复的明细不能入账"
        }
        val updated = existing.copy(
            type = edit.type,
            categoryId = edit.categoryId,
            categoryOrigin = if (edit.categoryId != existing.categoryId) {
                ImportCategoryOrigin.USER
            } else {
                existing.categoryOrigin
            },
            accountId = edit.accountId,
            note = edit.note?.trim()?.takeIf(String::isNotEmpty),
            tags = edit.tags.map(String::trim).filter(String::isNotEmpty).distinct(),
            isExcluded = edit.isExcluded,
            isSkipped = edit.isSkipped,
        )
        database.importSessionQueries.updateImportPreviewItem(
            payload = encode(updated),
            is_skipped = if (updated.isSkipped) 1L else 0L,
            updated_at = now().toEpochMilliseconds(),
            id = updated.id,
            session_id = sessionId,
        )
    }

    override suspend fun updateCategory(sessionId: ImportSessionId, itemId: String, categoryId: String?) {
        val row = database.importSessionQueries.previewItem(itemId, sessionId).executeAsOneOrNull()
            ?: error("导入预览项不存在")
        val updated = decode(row.id, row.payload, row.is_skipped != 0L).copy(
            categoryId = categoryId,
            categoryOrigin = ImportCategoryOrigin.USER,
        )
        database.importSessionQueries.updateImportPreviewItem(
            payload = encode(updated),
            is_skipped = if (updated.isSkipped) 1L else 0L,
            updated_at = now().toEpochMilliseconds(),
            id = updated.id,
            session_id = sessionId,
        )
    }

    override suspend fun updateSkipped(sessionId: ImportSessionId, itemId: String, isSkipped: Boolean) {
        val row = database.importSessionQueries.previewItem(itemId, sessionId).executeAsOneOrNull()
            ?: error("导入预览项不存在")
        val existing = decode(row.id, row.payload, row.is_skipped != 0L)
        require(existing.duplicateStatus != ImportDuplicateStatus.CONFIRMED || isSkipped) {
            "已确认重复的明细不能入账"
        }
        val updated = existing.copy(isSkipped = isSkipped)
        database.importSessionQueries.updateImportPreviewItem(
            payload = encode(updated),
            is_skipped = if (updated.isSkipped) 1L else 0L,
            updated_at = now().toEpochMilliseconds(),
            id = updated.id,
            session_id = sessionId,
        )
    }

    override suspend fun delete(sessionId: ImportSessionId) {
        database.transaction {
            database.importSessionQueries.deleteImportPreviewItems(sessionId)
            database.importSessionQueries.deleteImportSession(sessionId)
        }
    }

    private fun encode(item: ImportPreviewItem): String = JsonObject(buildMap<String, JsonElement> {
        put("format", JsonPrimitive(item.raw.format.name))
        put("occurredAt", JsonPrimitive(item.raw.occurredAt.toEpochMilliseconds()))
        put("amountMinor", JsonPrimitive(item.raw.amount.minor))
        item.raw.type?.let { put("rawType", JsonPrimitive(it.name)) }
        put("rawExcluded", JsonPrimitive(item.raw.isExcluded))
        item.raw.accountName?.let { put("accountName", JsonPrimitive(it)) }
        item.raw.note?.let { put("rawNote", JsonPrimitive(it)) }
        item.raw.externalId?.let { put("externalId", JsonPrimitive(it)) }
        item.raw.sourceCategory?.let { put("sourceCategory", JsonPrimitive(it)) }
        item.raw.sourceLedgerName?.let { put("sourceLedgerName", JsonPrimitive(it)) }
        put("rawTags", JsonArray(item.raw.tags.map(::JsonPrimitive)))
        item.type?.let { put("type", JsonPrimitive(it.name)) }
        item.categoryId?.let { put("categoryId", JsonPrimitive(it)) }
        put("categoryOrigin", JsonPrimitive(item.categoryOrigin.name))
        item.accountId?.let { put("accountId", JsonPrimitive(it)) }
        item.note?.let { put("note", JsonPrimitive(it)) }
        put("tags", JsonArray(item.tags.map(::JsonPrimitive)))
        put("isExcluded", JsonPrimitive(item.isExcluded))
        put("duplicateStatus", JsonPrimitive(item.duplicateStatus.name))
    }).toString()

    private fun decode(id: String, payload: String, isSkipped: Boolean): ImportPreviewItem {
        val objectValue = json.parseToJsonElement(payload).jsonObject
        fun text(name: String): String? = objectValue[name]?.jsonPrimitive?.contentOrNull
        fun textList(name: String): List<String> = objectValue[name]?.jsonArray
            ?.map { it.jsonPrimitive.content }
            .orEmpty()
        return ImportPreviewItem(
            id = id,
            raw = RawTransaction(
                format = ImportFormat.valueOf(text("format") ?: error("导入预览项缺少格式")),
                occurredAt = Instant.fromEpochMilliseconds(text("occurredAt")?.toLong() ?: error("导入预览项缺少时间")),
                amount = Money(text("amountMinor")?.toLong() ?: error("导入预览项缺少金额")),
                type = text("rawType")?.let(TransactionType::valueOf),
                isExcluded = objectValue["rawExcluded"]?.jsonPrimitive?.boolean ?: false,
                accountName = text("accountName"),
                note = text("rawNote"),
                externalId = text("externalId"),
                sourceCategory = text("sourceCategory"),
                sourceLedgerName = text("sourceLedgerName"),
                tags = textList("rawTags"),
            ),
            type = text("type")?.let(TransactionType::valueOf),
            categoryId = text("categoryId"),
            categoryOrigin = text("categoryOrigin")?.let(ImportCategoryOrigin::valueOf)
                ?: ImportCategoryOrigin.NONE,
            accountId = text("accountId"),
            note = text("note"),
            tags = textList("tags"),
            isExcluded = objectValue["isExcluded"]?.jsonPrimitive?.boolean ?: false,
            isSkipped = isSkipped,
            duplicateStatus = text("duplicateStatus")?.let(ImportDuplicateStatus::valueOf)
                ?: ImportDuplicateStatus.NONE,
        )
    }
}
