package com.omniflow.shared.data.repository

import com.omniflow.shared.db.OmniFlowDatabase
import com.omniflow.shared.domain.model.Tag
import com.omniflow.shared.domain.model.TagId
import com.omniflow.shared.domain.repository.TagRepository
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

class SqlDelightTagRepository(
    private val database: OmniFlowDatabase,
    private val now: () -> Instant = { Clock.System.now() },
) : TagRepository {
    override suspend fun activeTags(ledgerId: String): List<Tag> = database.tagQueries.activeTagsForLedger(ledgerId)
        .executeAsList()
        .map {
            Tag(
                id = it.id,
                ledgerId = it.ledger_id,
                name = it.name,
                deletedAt = it.deleted_at?.let(Instant::fromEpochMilliseconds),
            )
        }

    override suspend fun create(tag: Tag) {
        validateTag(tag)
        val timestamp = now().toEpochMilliseconds()
        database.tagQueries.insertTag(tag.id, tag.ledgerId, tag.name.trim(), timestamp, timestamp)
    }

    override suspend fun update(tag: Tag) {
        validateTag(tag)
        require(database.tagQueries.activeTagIdForLedger(tag.id, tag.ledgerId).executeAsOneOrNull() != null) {
            "标签不存在或已删除"
        }
        database.tagQueries.updateTag(
            name = tag.name.trim(),
            updated_at = now().toEpochMilliseconds(),
            id = tag.id,
            ledger_id = tag.ledgerId,
        )
    }

    override suspend fun archive(tagId: TagId) {
        require(database.tagQueries.activeTag(tagId).executeAsOneOrNull() != null) {
            "标签不存在或已删除"
        }
        database.tagQueries.archiveTag(now().toEpochMilliseconds(), tagId)
    }

    private fun validateTag(tag: Tag) {
        require(tag.name.isNotBlank()) { "标签名称不能为空" }
        require(database.ledgerQueries.activeLedgerId(tag.ledgerId).executeAsOneOrNull() != null) {
            "账本不存在或已删除"
        }
    }
}
