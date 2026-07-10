package com.omniflow.shared.domain.model

typealias RuleId = String

enum class RuleConditionType { NOTE_CONTAINS, TRANSACTION_TYPE, TRANSACTION_SOURCE }

enum class RuleActionType { SET_CATEGORY, SET_EXCLUDED, EXCLUDE }

data class Rule(
    val id: RuleId,
    val ledgerId: LedgerId,
    val name: String,
    val conditionType: RuleConditionType,
    val conditionValue: String,
    val actionType: RuleActionType,
    val actionValue: String,
    val priority: Int,
    val deletedAt: kotlinx.datetime.Instant? = null,
)
