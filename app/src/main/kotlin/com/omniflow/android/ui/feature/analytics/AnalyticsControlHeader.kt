package com.omniflow.android.ui.feature.analytics

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.omniflow.android.ui.LedgerScopePill
import com.omniflow.android.ui.designsystem.components.OmniDatePickerDialog
import com.omniflow.android.ui.displayLabel
import com.omniflow.core.domain.model.AppTimeZone
import com.omniflow.core.domain.model.DateRange
import com.omniflow.core.domain.model.Ledger
import com.omniflow.core.domain.model.LedgerScope
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.toLocalDateTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsControlHeader(
    scope: LedgerScope,
    ledgers: List<Ledger>,
    rangeMode: AnalyticsRangeMode,
    range: DateRange,
    onScopeChange: (LedgerScope) -> Unit,
    onRangeModeChange: (AnalyticsRangeMode) -> Unit,
    onShiftRange: (Long) -> Unit,
    onResetRange: () -> Unit,
    onCustomRangeSelected: (DateRange) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showStartPicker by remember { mutableStateOf(false) }
    var showEndPicker by remember { mutableStateOf(false) }
    var customStartDate by remember { mutableStateOf<LocalDate?>(null) }

    val now = Clock.System.now()
    val showReset = rangeMode != AnalyticsRangeMode.CUSTOM &&
        (now < range.startInclusive || now >= range.endExclusive)

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        // Top: Ledger scope selector
        LedgerScopePill(
            scope = scope,
            ledgers = ledgers,
            onScope = onScopeChange,
        )

        // Main Header Container: Segmented buttons + Stepper
        ElevatedCard(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.medium,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                // M3 SingleChoiceSegmentedButtonRow
                val modes = AnalyticsRangeMode.entries
                SingleChoiceSegmentedButtonRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(36.dp),
                ) {
                    modes.forEachIndexed { index, mode ->
                        SegmentedButton(
                            shape = SegmentedButtonDefaults.itemShape(index = index, count = modes.size),
                            onClick = { onRangeModeChange(mode) },
                            selected = rangeMode == mode,
                            label = {
                                Text(
                                    text = mode.label,
                                    style = MaterialTheme.typography.labelMedium,
                                )
                            },
                        )
                    }
                }

                // Range Stepper / Date controller
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    if (rangeMode == AnalyticsRangeMode.CUSTOM) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                        ) {
                            FilledTonalButton(
                                onClick = { showStartPicker = true },
                                modifier = Modifier.height(34.dp),
                            ) {
                                Text(
                                    text = range.displayLabel(rangeMode),
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                )
                            }
                        }
                    } else {
                        IconButton(
                            onClick = { onShiftRange(-1) },
                            modifier = Modifier.size(34.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Default.ArrowBackIosNew,
                                contentDescription = "上一周期",
                                modifier = Modifier.size(16.dp),
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Text(
                                text = range.displayLabel(rangeMode),
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                            )
                            if (showReset) {
                                TextButton(
                                    onClick = onResetRange,
                                    modifier = Modifier.height(28.dp),
                                ) {
                                    Text(
                                        text = "回到当前",
                                        style = MaterialTheme.typography.labelSmall,
                                    )
                                }
                            }
                        }

                        IconButton(
                            onClick = { onShiftRange(1) },
                            modifier = Modifier.size(34.dp),
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                                contentDescription = "下一周期",
                                modifier = Modifier.size(16.dp),
                            )
                        }
                    }
                }
            }
        }
    }

    if (showStartPicker) {
        val initialStart = range.startLocalDate(AppTimeZone)
        OmniDatePickerDialog(
            initialDate = initialStart,
            title = { Text("选择开始日期", modifier = Modifier.padding(16.dp)) },
            onDateSelected = { start ->
                customStartDate = start
                showStartPicker = false
                showEndPicker = true
            },
            onDismissRequest = { showStartPicker = false },
        )
    }

    if (showEndPicker) {
        val startDate = customStartDate ?: range.startLocalDate(AppTimeZone)
        val initialEnd = range.endLocalDateInclusive(AppTimeZone)
        OmniDatePickerDialog(
            initialDate = initialEnd,
            title = { Text("选择结束日期", modifier = Modifier.padding(16.dp)) },
            onDateSelected = { end ->
                showEndPicker = false
                val newRange = DateRange.ofClosedDates(startDate, end, AppTimeZone)
                onCustomRangeSelected(newRange)
            },
            onDismissRequest = { showEndPicker = false },
        )
    }
}

private val AnalyticsRangeMode.label: String get() = when (this) {
    AnalyticsRangeMode.WEEK -> "周"
    AnalyticsRangeMode.MONTH -> "月"
    AnalyticsRangeMode.YEAR -> "年"
    AnalyticsRangeMode.CUSTOM -> "范围"
}
