package com.omniflow.android

import android.app.Application
import com.omniflow.shared.SharedApp
import com.omniflow.shared.createAndroidSharedApp

class OmniFlowApplication : Application() {
    val sharedApp: SharedApp by lazy { createAndroidSharedApp(this) }
}
