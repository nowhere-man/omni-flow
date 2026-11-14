package com.omniflow.android.ui.feature.more

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.omniflow.android.ui.ExportDateButton
import com.omniflow.android.ui.MoreUiState
import com.omniflow.android.ui.OmniFlowViewModel
import com.omniflow.android.ui.OmniText
import com.omniflow.android.ui.designsystem.components.OmniExpressiveSwitch
import com.omniflow.android.ui.designsystem.components.OmniSubPageScaffold
import com.omniflow.android.ui.exportRange
import com.omniflow.android.ui.mutedContent
import com.omniflow.core.data.export.JsonToCsvExporter
import com.omniflow.core.domain.model.AppTimeZone
import com.omniflow.core.domain.model.DateRange
import com.omniflow.core.domain.model.LedgerScope
import com.omniflow.core.domain.model.TransactionType
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.toLocalDateTime

/**
 * 导出账单工作台：
 * 顶部实时预览看板 + 格式与 UTF-8 BOM 控制 + FilterChip 筛选 + 常驻胶囊导出按钮。
 */
@Composable
fun ExportPage(
    state: MoreUiState,
    viewModel: OmniFlowViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var payload by remember { mutableStateOf<String?>(null) }
    var formatChoice by remember { mutableStateOf(ExportFormatChoice.CSV) }
    var useBom by remember { mutableStateOf(true) }
    val today = remember { Clock.System.now().toLocalDateTime(AppTimeZone).date }

    var timeSpan by remember { mutableStateOf(ExportTimeSpan.ALL) }
    var startDate by remember { mutableStateOf(LocalDate(today.year, today.monthNumber, 1)) }
    var endDate by remember { mutableStateOf(today) }

    var ledgerScope by remember { mutableStateOf<LedgerScope>(LedgerScope.All) }
    var exportType by remember { mutableStateOf<TransactionType?>(null) }

    val exportDateRange: DateRange? = when (timeSpan) {
        ExportTimeSpan.ALL -> null
        ExportTimeSpan.THIS_MONTH -> exportRange(LocalDate(today.year, today.monthNumber, 1), today)
        ExportTimeSpan.THIS_YEAR -> exportRange(LocalDate(today.year, 1, 1), today)
        ExportTimeSpan.CUSTOM -> exportRange(startDate, endDate)
    }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument(formatChoice.mimeType)) { uri ->
        if (uri != null && payload != null) {
            context.contentResolver.openOutputStream(uri)?.use { stream ->
                val raw = payload!!
                val contentBytes = if (formatChoice == ExportFormatChoice.CSV) {
                    val csv = JsonToCsvExporter.export(raw)
                    if (useBom) {
                        byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte()) + csv.toByteArray(Charsets.UTF_8)
                    } else {
                        csv.toByteArray(Charsets.UTF_8)
                    }
                } else {
                    raw.toByteArray(Charsets.UTF_8)
                }
                stream.write(contentBytes)
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

    LaunchedEffect(ledgerScope, timeSpan, startDate, endDate, exportType, state.ledgers) {
        viewModel.observeExportPreview(ledgerScope, exportDateRange, exportType)
    }

    OmniSubPageScaffold(
        title = "导出账单",
        onBack = onBack,
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
        ) {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                // 1. 顶部实时预览看板
                item {
                    ExportPreviewHubCard(
                        count = state.exportPreviewCount,
                        isLoading = state.isLoadingExportPreview,
                        format = formatChoice,
                    )
                }

                // 2. 导出格式与 BOM
                item {
                    ElevatedCard(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.elevatedCardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                        ),
                        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 0.dp),
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Text("导出格式", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold))

                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                ExportFormatChoice.entries.forEach { choice ->
                                    FilterChip(
                                        selected = formatChoice == choice,
                                        onClick = { formatChoice = choice },
                                        label = { Text(choice.label) },
                                        shape = RoundedCornerShape(12.dp),
                                    )
                                }
                            }

                            if (formatChoice == ExportFormatChoice.CSV) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                ) {
                                    Column(Modifier.weight(1f)) {
                                        Text("添加 UTF-8 BOM", style = MaterialTheme.typography.labelLarge)
                                        Text("解决 Excel 打开中文乱码问题（推荐开启）", style = OmniText.caption, color = mutedContent())
                                    }
                                    OmniExpressiveSwitch(checked = useBom, onCheckedChange = { useBom = it })
                                }
                            }
                        }
                    }
                }

                // 3. 时间跨度筛选
                item {
                    ElevatedCard(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.elevatedCardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                        ),
                        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 0.dp),
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Text("时间跨度", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold))

                            Row(
                                modifier = Modifier.horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                ExportTimeSpan.entries.forEach { span ->
                                    FilterChip(
                                        selected = timeSpan == span,
                                        onClick = { timeSpan = span },
                                        label = { Text(span.label) },
                                        shape = RoundedCornerShape(12.dp),
                                    )
                                }
                            }

                            AnimatedVisibility(visible = timeSpan == ExportTimeSpan.CUSTOM) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    ExportDateButton("开始日期", startDate, Modifier.weight(1f)) { startDate = it }
                                    ExportDateButton("结束日期", endDate, Modifier.weight(1f)) { endDate = it }
                                }
                            }
                        }
                    }
                }

                // 4. 账本范围筛选
                item {
                    ElevatedCard(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.elevatedCardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                        ),
                        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 0.dp),
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Text("账本范围", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold))

                            Row(
                                modifier = Modifier.horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                FilterChip(
                                    selected = ledgerScope is LedgerScope.All,
                                    onClick = { ledgerScope = LedgerScope.All },
                                    label = { Text("全部账本") },
                                    shape = RoundedCornerShape(12.dp),
                                )

                                state.ledgers.forEach { ledger ->
                                    FilterChip(
                                        selected = ledgerScope is LedgerScope.Single && (ledgerScope as LedgerScope.Single).ledgerId == ledger.id,
                                        onClick = { ledgerScope = LedgerScope.Single(ledger.id) },
                                        label = { Text(ledger.name) },
                                        shape = RoundedCornerShape(12.dp),
                                    )
                                }
                            }
                        }
                    }
                }

                // 5. 交易类型筛选
                item {
                    ElevatedCard(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.elevatedCardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                        ),
                        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 0.dp),
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Text("交易类型", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold))

                            Row(
                                modifier = Modifier.horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                listOf(
                                    null to "全部类型",
                                    TransactionType.EXPENSE to "支出",
                                    TransactionType.INCOME to "收入",
                                    TransactionType.TRANSFER to "转账",
                                ).forEach { (type, label) ->
                                    FilterChip(
                                        selected = exportType == type,
                                        onClick = { exportType = type },
                                        label = { Text(label) },
                                        shape = RoundedCornerShape(12.dp),
                                    )
                                }
                            }
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

            // 底部常驻胶囊导出按钮（0 笔或导出中时置灰）
            val count = state.exportPreviewCount ?: 0
            val canExport = !state.isExporting && count > 0

            Button(
                onClick = { viewModel.prepareExport(ledgerScope, exportDateRange, exportType) },
                enabled = canExport,
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp)
                    .height(52.dp),
            ) {
                Icon(Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    when {
                        state.isExporting -> "正在导出…"
                        count == 0 -> "当前筛选无明细可导出"
                        else -> "确认导出 ($count 笔明细)"
                    },
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        }
    }
}
