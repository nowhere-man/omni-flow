package com.omniflow.android.ui.feature.more

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.omniflow.android.ui.ExportDateButton
import com.omniflow.android.ui.LedgerScopePill
import com.omniflow.android.ui.MoreUiState
import com.omniflow.android.ui.OmniFlowViewModel
import com.omniflow.android.ui.OmniRadius
import com.omniflow.android.ui.OmniSegmented
import com.omniflow.android.ui.OmniText
import com.omniflow.android.ui.designsystem.components.OmniExpressiveSwitch
import com.omniflow.android.ui.exportRange
import com.omniflow.android.ui.mutedContent
import com.omniflow.android.ui.surfaceCard
import com.omniflow.core.data.export.JsonToCsvExporter
import com.omniflow.core.domain.model.AppTimeZone
import com.omniflow.core.domain.model.DateRange
import com.omniflow.core.domain.model.LedgerScope
import com.omniflow.core.domain.model.TransactionType
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.toLocalDateTime

internal enum class ExportFormatChoice(val label: String, val extension: String, val mimeType: String) {
    CSV("CSV 表格", "csv", "text/csv"),
    JSON("JSON 备份", "json", "application/json"),
}

@Composable
fun ExportPage(
    state: MoreUiState,
    viewModel: OmniFlowViewModel,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var payload by remember { mutableStateOf<String?>(null) }
    var formatChoice by remember { mutableStateOf(ExportFormatChoice.CSV) }
    val today = remember { Clock.System.now().toLocalDateTime(AppTimeZone).date }
    var ledgerScope by remember { mutableStateOf<LedgerScope>(LedgerScope.All) }
    var incremental by remember { mutableStateOf(false) }
    var startDate by remember { mutableStateOf(today) }
    var endDate by remember { mutableStateOf(today) }
    var exportType by remember { mutableStateOf<TransactionType?>(null) }
    val exportDateRange = if (incremental) exportRange(startDate, endDate) else null
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument(formatChoice.mimeType)) { uri ->
        if (uri != null && payload != null) {
            context.contentResolver.openOutputStream(uri)?.use { stream ->
                val content = if (formatChoice == ExportFormatChoice.CSV) {
                    JsonToCsvExporter.export(payload!!)
                } else {
                    payload!!
                }
                stream.write(content.toByteArray(Charsets.UTF_8))
            }
        }
        payload = null
        viewModel.consumeExportPayload()
    }
    LaunchedEffect(state.exportPayload) {
        state.exportPayload?.let {
            payload = it
            launcher.launch("OmniFlow-Export-${today}.${formatChoice.extension}")
        }
    }
    LaunchedEffect(ledgerScope, incremental, startDate, endDate, exportType, state.ledgers) {
        viewModel.observeExportPreview(ledgerScope, exportDateRange, exportType)
    }

    Column(modifier.fillMaxSize()) {
        LazyColumn(
            Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Text("导出格式", style = OmniText.caption, color = mutedContent())
            }
            item {
                OmniSegmented(
                    options = ExportFormatChoice.entries,
                    selected = formatChoice,
                    label = { it.label },
                    onSelected = { formatChoice = it },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            item {
                Text("账本", style = OmniText.caption, color = mutedContent())
            }
            item {
                LedgerScopePill(
                    scope = ledgerScope,
                    ledgers = state.ledgers,
                    onScope = { ledgerScope = it },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            item {
                Text("日期", style = OmniText.caption, color = mutedContent())
            }
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                ) {
                    Column(
                        Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Row(
                            Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(
                                Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(2.dp),
                            ) {
                                Text("仅导出日期范围", style = OmniText.bodyRow)
                                Text(
                                    if (incremental) "只导出所选日期内的交易" else "默认导出全部日期",
                                    style = OmniText.caption,
                                    color = mutedContent(),
                                )
                            }
                            OmniExpressiveSwitch(checked = incremental, onCheckedChange = { incremental = it })
                        }
                        if (incremental) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                ExportDateButton("开始", startDate, Modifier.weight(1f)) { startDate = it }
                                ExportDateButton("结束", endDate, Modifier.weight(1f)) { endDate = it }
                            }
                        }
                    }
                }
            }
            item {
                Text("类别", style = OmniText.caption, color = mutedContent())
            }
            item {
                OmniSegmented(
                    options = listOf<TransactionType?>(null, TransactionType.INCOME, TransactionType.EXPENSE, TransactionType.TRANSFER),
                    selected = exportType,
                    label = { type ->
                        when (type) {
                            null -> "全部"
                            TransactionType.INCOME -> "收入"
                            TransactionType.EXPENSE -> "支出"
                            TransactionType.TRANSFER -> "转账"
                        }
                    },
                    onSelected = { exportType = it },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                ) {
                    Row(
                        Modifier.fillMaxWidth().padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("导出预览", style = OmniText.caption)
                            Text(
                                when {
                                    state.isLoadingExportPreview -> "正在计算…"
                                    state.exportPreviewCount != null -> "${state.exportPreviewCount} 条记录"
                                    else -> "—"
                                },
                                style = OmniText.amountPrimary,
                            )
                            Text("当前筛选条件下的交易", style = OmniText.caption)
                        }
                        Icon(Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(32.dp))
                    }
                }
            }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    state.exportWarnings.forEach { Text(it, color = mutedContent(), style = OmniText.caption) }
                    state.error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = OmniText.bodyRow) }
                }
            }
        }
        Button(
            onClick = { viewModel.prepareExport(ledgerScope, exportDateRange, exportType) },
            enabled = !state.isExporting,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 12.dp).height(52.dp),
        ) {
            Icon(Icons.Default.FileDownload, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text(if (state.isExporting) "导出中…" else "导出")
        }
    }
}

