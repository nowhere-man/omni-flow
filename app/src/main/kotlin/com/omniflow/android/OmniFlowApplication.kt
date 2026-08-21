package com.omniflow.android

import android.app.Application
import com.omniflow.core.SharedApp
import com.omniflow.core.createAndroidSharedApp
import com.omniflow.core.domain.model.SyncTarget

class OmniFlowApplication : Application() {
    val sharedApp: SharedApp by lazy {
        createAndroidSharedApp(
            this,
            mapOf(SyncTarget.WEBDAV to WebDavSyncAdapter(this)),
            AiCategoryAdapter(this),
        )
    }
}
