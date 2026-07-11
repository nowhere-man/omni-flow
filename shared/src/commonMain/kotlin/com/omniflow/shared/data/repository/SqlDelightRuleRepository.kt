package com.omniflow.shared.data.repository

import com.omniflow.shared.db.OmniFlowDatabase
import com.omniflow.shared.domain.model.Rule
import com.omniflow.shared.domain.model.RuleActionType
import com.omniflow.shared.domain.model.RuleConditionType
import com.omniflow.shared.domain.model.RuleId
import com.omniflow.shared.domain.repository.RuleRepository
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

class SqlDelightRuleRepository(
    private val database: OmniFlowDatabase,
    private val now: () -> Instant = { Clock.System.now() },
) : RuleRepository {
    override suspend fun activeRules(ledgerId: String): List<Rule> = database.ruleQueries.activeRulesForLedger(ledgerId)
        .executeAsList()
        .map(::toRule)

    override suspend fun create(rule: Rule) {
        validateRule(rule)
        val timestamp = now().toEpochMilliseconds()
        database.ruleQueries.insertRule(
            id = rule.id,
            ledger_id = rule.ledgerId,
            name = rule.name.trim(),
            condition_type = rule.conditionType.name,
            condition_value = rule.conditionValue.trim(),
            action_type = rule.actionType.name,
            action_value = rule.actionValue.trim(),
            priority = rule.priority.toLong(),
            created_at = timestamp,
            updated_at = timestamp,
        )
    }

    override suspend fun update(rule: Rule) {
        validateRule(rule)
        require(database.ruleQueries.activeRuleIdForLedger(rule.id, rule.ledgerId).executeAsOneOrNull() != null) {
            "规则不存在或已删除"
        }
        database.ruleQueries.updateRule(
            name = rule.name.trim(),
            condition_type = rule.conditionType.name,
            condition_value = rule.conditionValue.trim(),
            action_type = rule.actionType.name,
            action_value = rule.actionValue.trim(),
            priority = rule.priority.toLong(),
            updated_at = now().toEpochMilliseconds(),
            id = rule.id,
            ledger_id = rule.ledgerId,
        )
    }

    override suspend fun reorder(ledgerId: String, orderedIds: List<RuleId>) {
        val active = activeRules(ledgerId).associateBy(Rule::id)
        require(orderedIds.distinct().size == orderedIds.size && orderedIds.toSet() == active.keys) {
            "规则排序列表不完整"
        }
        val timestamp = now().toEpochMilliseconds()
        database.transaction {
            orderedIds.forEachIndexed { priority, id ->
                val rule = active.getValue(id)
                database.ruleQueries.updateRule(
                    name = rule.name,
                    condition_type = rule.conditionType.name,
                    condition_value = rule.conditionValue,
                    action_type = rule.actionType.name,
                    action_value = rule.actionValue,
                    priority = priority.toLong(),
                    updated_at = timestamp,
                    id = id,
                    ledger_id = ledgerId,
                )
            }
        }
    }

    override suspend fun archive(ruleId: RuleId) {
        require(database.ruleQueries.activeRule(ruleId).executeAsOneOrNull() != null) {
            "规则不存在或已删除"
        }
        database.ruleQueries.archiveRule(now().toEpochMilliseconds(), ruleId)
    }

    private fun validateRule(rule: Rule) {
        require(rule.name.isNotBlank()) { "规则名称不能为空" }
        require(rule.conditionValue.isNotBlank()) { "规则条件不能为空" }
        require(rule.priority >= 0) { "规则优先级不能为负数" }
        require(database.ledgerQueries.activeLedgerId(rule.ledgerId).executeAsOneOrNull() != null) {
            "账本不存在或已删除"
        }
        if (rule.actionType == RuleActionType.SET_CATEGORY) {
            require(database.categoryQueries.activeCategoryTypeForLedger(
                id = rule.actionValue,
                ledger_id = rule.ledgerId,
            ).executeAsOneOrNull() != null) {
                "规则分类不存在或已删除"
            }
        }
    }

    private fun toRule(row: com.omniflow.shared.db.Rule) = Rule(
        id = row.id,
        ledgerId = row.ledger_id,
        name = row.name,
        conditionType = RuleConditionType.valueOf(row.condition_type),
        conditionValue = row.condition_value,
        actionType = RuleActionType.valueOf(row.action_type),
        actionValue = row.action_value,
        priority = row.priority.toInt(),
        deletedAt = row.deleted_at?.let(Instant::fromEpochMilliseconds),
    )
}
