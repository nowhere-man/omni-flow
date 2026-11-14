package com.omniflow.android.ui.feature.more

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.omniflow.android.ui.ImportPreviewSection
import com.omniflow.android.ui.ImportSourceIcon
import com.omniflow.android.ui.LedgerPickerBar
import com.omniflow.android.ui.MoreUiState
import com.omniflow.android.ui.OmniFlowViewModel
import com.omniflow.android.ui.OmniRadius
import com.omniflow.android.ui.OmniText
import com.omniflow.android.ui.label
import com.omniflow.android.ui.mutedContent
import com.omniflow.android.ui.surfaceCard
import com.omniflow.core.domain.model.ImportPreviewPhase
import com.omniflow.core.parser.ImportFormat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun ImportPage(
    state: MoreUiState,
    viewModel: OmniFlowViewModel,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var ledgerId by remember(state.selectedLedgerId) { mutableStateOf(state.selectedLedgerId) }
    var selectedFormat by remember { mutableStateOf<ImportFormat?>(null) }
    val fileLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null && ledgerId != null) {
            scope.launch {
                val bytes = withContext(Dispatchers.IO) {
                    context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                }
                if (bytes != null) {
                    viewModel.importFile(
                        ledgerId = ledgerId!!,
                        fileName = context.fileName(uri),
                        bytes = bytes,
                        selectedFormat = selectedFormat,
                    )
                }
            }
        }
    }

    Column(modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (state.importPreview?.phase == ImportPreviewPhase.READY) {
            ImportPreviewSection(
                preview = state.importPreview,
                state = state,
                viewModel = viewModel,
                modifier = Modifier.weight(1f),
            )
            return@Column
        }
        LazyColumn(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                LedgerPickerBar(state.ledgers, ledgerId) { ledgerId = it; viewModel.selectMoreLedger(it) }
            }
            if (state.accounts.isEmpty()) {
                item {
                    Text(
                        "请先在「账户」里创建至少一个账户，导入的交易需要落到账户上。",
                        color = MaterialTheme.colorScheme.error,
                        style = OmniText.caption,
                    )
                }
            }
            item { Text("选择账单来源", style = OmniText.caption, color = mutedContent()) }
            val importOptions: List<ImportFormat?> = listOf(null) + ImportFormat.entries
            items(importOptions.chunked(3)) { row ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    for (format in row) {
                        if (format == null) {
                            ImportSourceTile(
                                label = "自动识别",
                                icon = {
                                    Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(44.dp))
                                },
                                enabled = ledgerId != null && state.accounts.isNotEmpty() && !state.isImporting,
                                modifier = Modifier.weight(1f),
                            ) {
                                selectedFormat = null
                                fileLauncher.launch("*/*")
                            }
                        } else {
                            ImportSourceTile(
                                label = format.label,
                                icon = { ImportSourceIcon(format, Modifier.size(44.dp)) },
                                enabled = ledgerId != null && state.accounts.isNotEmpty() && !state.isImporting,
                                modifier = Modifier.weight(1f),
                            ) {
                                selectedFormat = format
                                fileLauncher.launch("*/*")
                            }
                        }
                    }
                    repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
                }
            }
            state.importPreview?.let { preview ->
                item {
                    Text(
                        when (preview.phase) {
                            ImportPreviewPhase.DETECTING -> "正在识别格式"
                            ImportPreviewPhase.PARSING -> "正在解析账单"
                            ImportPreviewPhase.ENRICHING -> "正在应用规则、记忆和去重"
                            ImportPreviewPhase.SUGGESTING -> "正在请求 AI 建议分类"
                            ImportPreviewPhase.READY -> "准备就绪"
                        },
                        style = OmniText.caption,
                        color = mutedContent(),
                    )
                }
            }
            if (state.isImporting) item { LinearProgressIndicator(modifier = Modifier.fillMaxWidth()) }
            state.importMessage?.let { item { Text(it, color = MaterialTheme.colorScheme.primary, style = OmniText.caption) } }
            state.error?.let { item { Text(it, color = MaterialTheme.colorScheme.error, style = OmniText.caption) } }
            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}

@Composable
private fun ImportSourceTile(
    label: String,
    icon: @Composable () -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Column(
            Modifier.padding(vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            icon()
            Text(label, style = OmniText.caption, maxLines = 1)
        }
    }
}

internal fun Context.fileName(uri: Uri): String {
    contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
        if (cursor.moveToFirst()) return cursor.getString(0)
    }
    return uri.lastPathSegment ?: "import"
}
