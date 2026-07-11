package com.omniflow.shared

import app.cash.sqldelight.driver.native.NativeSqliteDriver
import com.omniflow.shared.data.sync.SyncAdapter
import com.omniflow.shared.data.sync.AppleFileSyncAdapter
import com.omniflow.shared.data.sync.AppleWebDavSyncAdapter
import com.omniflow.shared.db.OmniFlowDatabase
import com.omniflow.shared.domain.model.SyncTarget

object AppleSharedAppFactory {
    fun create(
        databaseName: String,
        iCloudDirectory: String? = null,
        iCloudAdapter: SyncAdapter? = null,
        webDavAdapter: SyncAdapter? = null,
    ): SharedApp = SharedApp(
        driver = NativeSqliteDriver(OmniFlowDatabase.Schema, databaseName),
        syncAdapters = buildMap {
            (iCloudAdapter ?: iCloudDirectory?.let(::AppleFileSyncAdapter))?.let { put(SyncTarget.ICLOUD, it) }
            put(SyncTarget.WEBDAV, webDavAdapter ?: AppleWebDavSyncAdapter())
        },
    )
}
