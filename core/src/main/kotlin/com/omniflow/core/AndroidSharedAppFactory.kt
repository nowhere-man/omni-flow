package com.omniflow.core

import android.content.Context
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import com.omniflow.core.data.sync.SyncAdapter
import com.omniflow.core.domain.ai.CategorySuggester
import com.omniflow.core.db.OmniFlowDatabase
import com.omniflow.core.domain.model.SyncTarget

fun createAndroidSharedApp(
    context: Context,
    syncAdapters: Map<SyncTarget, SyncAdapter> = emptyMap(),
    categorySuggester: CategorySuggester? = null,
): SharedApp = SharedApp(
    AndroidSqliteDriver(OmniFlowDatabase.Schema, context, "omniflow.db"),
    syncAdapters,
    categorySuggester,
)
