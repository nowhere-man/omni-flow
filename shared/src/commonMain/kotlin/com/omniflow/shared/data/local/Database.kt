package com.omniflow.shared.data.local

import app.cash.sqldelight.db.SqlDriver
import com.omniflow.shared.db.OmniFlowDatabase

fun createDatabase(driver: SqlDriver): OmniFlowDatabase = OmniFlowDatabase(driver)
