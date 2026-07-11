package com.omniflow.shared.data

import com.omniflow.shared.data.facade.SqlDelightManagementFacade
import com.omniflow.shared.data.local.createJvmDatabase
import com.omniflow.shared.data.repository.SqlDelightLedgerRepository
import com.omniflow.shared.data.repository.SqlDelightRuleRepository
import com.omniflow.shared.domain.model.Rule
import com.omniflow.shared.domain.model.RuleActionType
import com.omniflow.shared.domain.model.RuleConditionType
import com.omniflow.shared.domain.usecase.CreateRuleUseCase
import com.omniflow.shared.domain.usecase.ReorderRulesUseCase
import com.omniflow.shared.domain.usecase.DeleteLedgerUseCase
import com.omniflow.shared.domain.usecase.SetDefaultLedgerUseCase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class SqlDelightManagementFacadeTest {
    @Test
    fun exposesDefaultLedgerAndLedgerScopedRules() = runBlocking {
        val database = createJvmDatabase()
        database.ledgerQueries.insertLedger("ledger", "日常", null, 1, 1)
        val ledgers = SqlDelightLedgerRepository(database)
        val rules = SqlDelightRuleRepository(database)
        val facade = SqlDelightManagementFacade(database)

        assertNull(facade.observeDefaultLedgerId().first().getOrThrow())
        SetDefaultLedgerUseCase(ledgers)("ledger").getOrThrow()
        assertEquals("ledger", facade.observeDefaultLedgerId().first().getOrThrow())

        CreateRuleUseCase(rules)(
            Rule(
                id = "rule",
                ledgerId = "ledger",
                name = "餐饮",
                conditionType = RuleConditionType.NOTE_CONTAINS,
                conditionValue = "餐厅",
                actionType = RuleActionType.SET_EXCLUDED,
                actionValue = "true",
                priority = 1,
            ),
        ).getOrThrow()
        CreateRuleUseCase(rules)(
            Rule("rule-2", "ledger", "工资", RuleConditionType.NOTE_CONTAINS, "工资", RuleActionType.SET_EXCLUDED, "true", 2),
        ).getOrThrow()
        ReorderRulesUseCase(rules)("ledger", listOf("rule-2", "rule")).getOrThrow()
        assertEquals(listOf("rule-2", "rule"), facade.observeRules("ledger").first().getOrThrow().map(Rule::id))

        DeleteLedgerUseCase(ledgers)("ledger").getOrThrow()
        assertNull(facade.observeDefaultLedgerId().first().getOrThrow())
        assertEquals(emptyList(), facade.observeRules("ledger").first().getOrThrow())
    }
}
