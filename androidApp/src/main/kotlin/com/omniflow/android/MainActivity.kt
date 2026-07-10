package com.omniflow.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import com.omniflow.android.ui.OmniFlowApp
import com.omniflow.android.ui.OmniFlowViewModel
import com.omniflow.android.ui.OmniFlowViewModelFactory

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val sharedApp = (application as OmniFlowApplication).sharedApp
        setContent {
            val viewModel: OmniFlowViewModel = viewModel(factory = OmniFlowViewModelFactory(sharedApp))
            OmniFlowApp(viewModel)
        }
    }
}
