package com.omniflow.android

import android.os.Bundle
import android.content.Intent
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.viewmodel.compose.viewModel
import com.omniflow.android.ui.OmniFlowApp
import com.omniflow.android.ui.OmniFlowViewModel
import com.omniflow.android.ui.OmniFlowViewModelFactory
import androidx.compose.runtime.mutableStateOf

class MainActivity : FragmentActivity() {
    private var deepLinkNonce = 0L
    private val deepLinkTransaction = mutableStateOf<Pair<String, Long>?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        intent.transactionRouteOrNull()?.let { deepLinkTransaction.value = it to ++deepLinkNonce }
        val sharedApp = (application as OmniFlowApplication).sharedApp
        setContent {
            val viewModel: OmniFlowViewModel = viewModel(factory = OmniFlowViewModelFactory(sharedApp))
            OmniFlowApp(viewModel, deepLinkTransaction.value)
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        intent.transactionRouteOrNull()?.let { deepLinkTransaction.value = it to ++deepLinkNonce }
    }
}

private fun Intent.transactionRouteOrNull(): String? = data
    ?.takeIf { it.scheme == "omniflow" && it.host == "transaction" }
    ?.pathSegments
    ?.singleOrNull()
