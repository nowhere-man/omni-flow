package com.omniflow.shared.domain.usecase

import com.omniflow.shared.domain.model.Rule
import com.omniflow.shared.domain.model.RuleId
import com.omniflow.shared.domain.model.LedgerId
import com.omniflow.shared.domain.repository.RuleRepository

class CreateRuleUseCase(private val rules: RuleRepository) {
    suspend operator fun invoke(rule: Rule): Result<Unit> = runCatching { rules.create(rule) }
}

class UpdateRuleUseCase(private val rules: RuleRepository) {
    suspend operator fun invoke(rule: Rule): Result<Unit> = runCatching { rules.update(rule) }
}

class ReorderRulesUseCase(private val rules: RuleRepository) {
    suspend operator fun invoke(ledgerId: LedgerId, orderedIds: List<RuleId>): Result<Unit> =
        runCatching { rules.reorder(ledgerId, orderedIds) }
}

class DeleteRuleUseCase(private val rules: RuleRepository) {
    suspend operator fun invoke(ruleId: RuleId): Result<Unit> = runCatching { rules.archive(ruleId) }
}
