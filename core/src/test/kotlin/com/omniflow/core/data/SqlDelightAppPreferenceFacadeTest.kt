package com.omniflow.core.data

import com.omniflow.core.data.facade.SqlDelightAppPreferenceFacade
import com.omniflow.core.data.local.createJvmDatabase
import com.omniflow.core.domain.facade.AppPreferenceFacade
import com.omniflow.core.domain.model.AppPreferences
import com.omniflow.core.domain.model.AppearanceMode
import com.omniflow.core.domain.model.LedgerScope
import com.omniflow.core.domain.model.SyncTarget
import com.omniflow.core.domain.model.ThemeColor
import com.omniflow.core.domain.model.TransactionDetailDisplayMode
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals

class SqlDelightAppPreferenceFacadeTest {
    @Test
    fun persistsRestorablePreferencesAsOneState() = runBlocking {
        val facade: AppPreferenceFacade = SqlDelightAppPreferenceFacade(createJvmDatabase())
        assertEquals(AppPreferences(), facade.observe().first().getOrThrow())

        val expected = AppPreferences(
            homeLedgerScope = LedgerScope.Single("home-ledger"),
            analyticsLedgerScope = LedgerScope.Single("analytics-ledger"),
            transactionDetailDisplayMode = TransactionDetailDisplayMode.CARD,
            appearanceMode = AppearanceMode.DARK,
            themeColor = ThemeColor.LAVENDER,
            appLockEnabled = true,
            syncTarget = SyncTarget.WEBDAV,
            backupRetention = 3,
        )

        facade.save(expected).getOrThrow()

        assertEquals(expected, facade.observe().first().getOrThrow())
    }
}
