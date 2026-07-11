package com.omniflow.shared

import android.content.Context
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import com.omniflow.shared.data.sync.SyncAdapter
import com.omniflow.shared.db.OmniFlowDatabase
import com.omniflow.shared.domain.model.SyncTarget

fun createAndroidSharedApp(
    context: Context,
    syncAdapters: Map<SyncTarget, SyncAdapter> = emptyMap(),
): SharedApp = SharedApp(
    AndroidSqliteDriver(OmniFlowDatabase.Schema, context, "omniflow.db"),
    syncAdapters,
)
