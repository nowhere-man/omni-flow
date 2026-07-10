package com.omniflow.shared

import android.content.Context
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import com.omniflow.shared.db.OmniFlowDatabase

fun createAndroidSharedApp(context: Context): SharedApp = SharedApp(
    AndroidSqliteDriver(OmniFlowDatabase.Schema, context, "omniflow.db"),
)
