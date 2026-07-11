package com.omniflow.android

import android.app.Application
import com.omniflow.shared.SharedApp
import com.omniflow.shared.createAndroidSharedApp
import com.omniflow.shared.domain.model.SyncTarget

class OmniFlowApplication : Application() {
    val sharedApp: SharedApp by lazy {
        createAndroidSharedApp(this, mapOf(SyncTarget.WEBDAV to WebDavSyncAdapter(this)))
    }
}
