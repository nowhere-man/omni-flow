package com.omniflow.android.ui.feature.more

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.omniflow.android.ui.ImportPreviewSection
import com.omniflow.android.ui.LedgerPickerBar
import com.omniflow.android.ui.MoreUiState
import com.omniflow.android.ui.OmniFlowViewModel
import com.omniflow.android.ui.OmniText
import com.omniflow.android.ui.designsystem.components.OmniSubPageScaffold
import com.omniflow.android.ui.mutedContent
import com.omniflow.core.domain.model.ImportPreviewPhase
import com.omniflow.core.parser.ImportFormat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 导入账单：三步向导 Stepper 工作台。
 * 严格收束来源为现存三家：微信支付 / 支付宝 / 通用 CSV。
 * 坚决取缔「自动智能识别」伪卡与未支持平台。
 */
@Composable
fun ImportPage(
    state: MoreUiState,
    viewModel: OmniFlowViewModel,
    onBack: () -> Unit,
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

    val currentStep = when {
        state.importPreview?.phase == ImportPreviewPhase.READY -> ImportStep.PREVIEW
        state.importMessage != null && state.importPreview == null -> ImportStep.DONE
        else -> ImportStep.SOURCE
    }

    OmniSubPageScaffold(
        title = "导入账单",
        onBack = onBack,
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // 顶部常驻 Stepper 步进指示器
            ImportStepperHeader(currentStep = currentStep)

            when (currentStep) {
                ImportStep.PREVIEW -> {
                    state.importPreview?.let { preview ->
                        ImportPreviewSection(
                            preview = preview,
                            state = state,
                            viewModel = viewModel,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
                ImportStep.DONE -> {
                    state.importMessage?.let { msg ->
                        Column(
                            modifier = Modifier.fillMaxWidth().weight(1f),
                            verticalArrangement = Arrangement.Center,
                        ) {
                            RecentImportReceiptCard(
                                message = msg,
                                onContinue = viewModel::cancelImport,
                            )
                        }
                    }
                }
                ImportStep.SOURCE -> {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        item {
                            LedgerPickerBar(state.ledgers, ledgerId) {
                                ledgerId = it
                                viewModel.selectMoreLedger(it)
                            }
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

                        item {
                            Text(
                                "选择导入来源",
                                style = OmniText.caption,
                                color = mutedContent(),
                                modifier = Modifier.padding(start = 4.dp),
                            )
                        }

                        // 来源 1：微信支付
                        item {
                            ImportSourceCard(
                                format = ImportFormat.WECHAT,
                                title = "微信支付",
                                hint = "微信 App > 我 > 服务 > 钱包 > 账单 > 常见问题 > 下载账单",
                                enabled = ledgerId != null && state.accounts.isNotEmpty() && !state.isImporting,
                                onClick = {
                                    selectedFormat = ImportFormat.WECHAT
                                    fileLauncher.launch("*/*")
                                },
                            )
                        }

                        // 来源 2：支付宝
                        item {
                            ImportSourceCard(
                                format = ImportFormat.ALIPAY,
                                title = "支付宝",
                                hint = "支付宝 App > 我的 > 账单 > 更多选项 > 开具交易流水证明",
                                enabled = ledgerId != null && state.accounts.isNotEmpty() && !state.isImporting,
                                onClick = {
                                    selectedFormat = ImportFormat.ALIPAY
                                    fileLauncher.launch("*/*")
                                },
                            )
                        }

                        // 来源 3：通用 CSV
                        item {
                            ImportSourceCard(
                                format = null,
                                title = "通用 CSV",
                                hint = "支持标准 UTF-8 / GB18030 编码的 CSV 交易明细文件",
                                enabled = ledgerId != null && state.accounts.isNotEmpty() && !state.isImporting,
                                onClick = {
                                    selectedFormat = null
                                    fileLauncher.launch("*/*")
                                },
                            )
                        }

                        if (state.isImporting) {
                            item {
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                                    Text(
                                        when (state.importPreview?.phase) {
                                            ImportPreviewPhase.DETECTING -> "正在识别账单格式…"
                                            ImportPreviewPhase.PARSING -> "正在解析交易明细…"
                                            ImportPreviewPhase.ENRICHING -> "正在匹配规则、记忆与去重…"
                                            ImportPreviewPhase.SUGGESTING -> "正在请求 AI 分类建议…"
                                            else -> "正在处理账单文件…"
                                        },
                                        style = OmniText.caption,
                                        color = mutedContent(),
                                    )
                                }
                            }
                        }

                        state.error?.let {
                            item {
                                Text(it, color = MaterialTheme.colorScheme.error, style = OmniText.caption)
                            }
                        }

                        item { Spacer(Modifier.height(16.dp)) }
                    }
                }
            }
        }
    }
}

internal fun Context.fileName(uri: Uri): String {
    contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
        if (cursor.moveToFirst()) return cursor.getString(0)
    }
    return uri.lastPathSegment ?: "import"
}
