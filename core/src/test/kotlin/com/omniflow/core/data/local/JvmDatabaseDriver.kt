package com.omniflow.core.data.local

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.omniflow.core.db.OmniFlowDatabase

fun createJvmDatabase(): OmniFlowDatabase {
    val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
    OmniFlowDatabase.Schema.create(driver)
    return createDatabase(driver)
}
