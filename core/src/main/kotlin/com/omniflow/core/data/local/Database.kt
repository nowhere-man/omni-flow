package com.omniflow.core.data.local

import app.cash.sqldelight.db.SqlDriver
import com.omniflow.core.db.OmniFlowDatabase

fun createDatabase(driver: SqlDriver): OmniFlowDatabase = OmniFlowDatabase(driver)
