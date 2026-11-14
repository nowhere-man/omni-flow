package com.omniflow.core.data

import com.omniflow.core.data.facade.SqlDelightAppPreferenceFacade
import com.omniflow.core.data.local.createJvmDatabase
import com.omniflow.core.domain.facade.AppPreferenceFacade
import com.omniflow.core.domain.model.AppPreferenceKey
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

    @Test
    fun decodesLegacyAndUnknownPreferencesWithSafeFallbacks() = runBlocking {
        val database = createJvmDatabase()
        val facade = SqlDelightAppPreferenceFacade(database)

        database.appPreferenceQueries.upsertPreference(AppPreferenceKey.AppearanceMode, "dark ", 1L)
        database.appPreferenceQueries.upsertPreference(AppPreferenceKey.ThemeColor, " mist_blue", 2L)
        database.appPreferenceQueries.upsertPreference(AppPreferenceKey.TransactionDetailDisplayMode, "UNKNOWN_MODE", 3L)
        database.appPreferenceQueries.upsertPreference(AppPreferenceKey.SyncTarget, "INVALID_TARGET", 4L)
        database.appPreferenceQueries.upsertPreference(AppPreferenceKey.BackupRetention, "-5", 5L)
        database.appPreferenceQueries.upsertPreference(AppPreferenceKey.AppLockEnabled, "invalid_bool", 6L)

        val prefs = facade.observe().first().getOrThrow()
        assertEquals(AppearanceMode.DARK, prefs.appearanceMode)
        assertEquals(ThemeColor.MIST_BLUE, prefs.themeColor)
        assertEquals(TransactionDetailDisplayMode.CARD, prefs.transactionDetailDisplayMode)
        assertEquals(null, prefs.syncTarget)
        assertEquals(10, prefs.backupRetention)
        assertEquals(false, prefs.appLockEnabled)
    }
}
