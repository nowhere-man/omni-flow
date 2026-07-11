package com.omniflow.shared.domain.facade

import com.omniflow.shared.domain.model.Account
import com.omniflow.shared.domain.model.AccountSummary
import com.omniflow.shared.domain.model.Category
import com.omniflow.shared.domain.model.Ledger
import com.omniflow.shared.domain.model.LedgerId
import com.omniflow.shared.domain.model.Rule
import com.omniflow.shared.domain.model.Tag
import kotlinx.coroutines.flow.Flow

interface ManagementFacade {
    fun observeLedgers(): Flow<Result<List<Ledger>>>
    fun observeDefaultLedgerId(): Flow<Result<LedgerId?>>
    fun observeAccounts(): Flow<Result<List<Account>>>
    fun observeAccountSummary(): Flow<Result<AccountSummary>>
    fun observeCategories(ledgerId: LedgerId): Flow<Result<List<Category>>>
    fun observeTags(ledgerId: LedgerId): Flow<Result<List<Tag>>>
    fun observeRules(ledgerId: LedgerId): Flow<Result<List<Rule>>>
}
