package com.omniflow.android.ui.feature.more

import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.omniflow.android.ui.MoreUiState
import com.omniflow.android.ui.OmniFlowViewModel
import com.omniflow.android.ui.OmniText
import com.omniflow.android.ui.designsystem.components.OmniBadgeIcon
import com.omniflow.android.ui.designsystem.components.OmniSubPageScaffold
import com.omniflow.android.ui.designsystem.tokens.SemanticBadgeType
import com.omniflow.android.ui.designsystem.tokens.semanticBadgeColors
import com.omniflow.android.ui.designsystem.tokens.themePrimaryColor
import com.omniflow.android.ui.mutedContent
import com.omniflow.core.domain.model.AppearanceMode
import com.omniflow.core.domain.model.LedgerScope
import com.omniflow.core.domain.model.ThemeColor
import com.omniflow.core.domain.model.TransactionDetailDisplayMode

/**
 * 通用设置页：遵循 Final Design M 方案。
 * 包含三张偏好面板（外观 / 记账习惯 / 提醒与备份），内嵌迷你实时预览。
 * 坚决剔除应用锁/生物识别与缓存清理行，全页徽章锁第 1 组 SYSTEM 色。
 */
@Composable
fun SettingsPage(
    state: MoreUiState,
    viewModel: OmniFlowViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val systemInDark = isSystemInDarkTheme()
    val systemColors = semanticBadgeColors(SemanticBadgeType.SYSTEM)
    val isPreviewDark = when (state.preferences.appearanceMode) {
        AppearanceMode.SYSTEM -> systemInDark
        AppearanceMode.LIGHT -> false
        AppearanceMode.DARK -> true
    }

    OmniSubPageScaffold(
        title = "通用设置",
        onBack = onBack,
        modifier = modifier,
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // 面板 1：外观与个性化（内嵌迷你实时预览）
            item {
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.elevatedCardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                    ),
                    elevation = CardDefaults.elevatedCardElevation(defaultElevation = 0.dp),
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            OmniBadgeIcon(
                                icon = Icons.Default.Palette,
                                badgeType = SemanticBadgeType.SYSTEM,
                            )
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text("外观与个性化", style = OmniText.titleRow)
                                Text("主题色与明暗显示模式", style = OmniText.caption, color = mutedContent())
                            }
                        }

                        // 迷你实时预览（假卡片 + 假按钮）
                        MiniAppearanceLivePreview(
                            themeColor = state.preferences.themeColor,
                            isDark = isPreviewDark,
                        )

                        // 明暗模式分段选择
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("深浅外观", style = OmniText.caption, color = mutedContent())
                            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                                AppearanceMode.entries.forEachIndexed { index, mode ->
                                    SegmentedButton(
                                        selected = state.preferences.appearanceMode == mode,
                                        onClick = { viewModel.setAppearanceMode(mode) },
                                        shape = SegmentedButtonDefaults.itemShape(index = index, count = AppearanceMode.entries.size),
                                        label = {
                                            Text(
                                                when (mode) {
                                                    AppearanceMode.SYSTEM -> "跟随系统"
                                                    AppearanceMode.LIGHT -> "浅色"
                                                    AppearanceMode.DARK -> "深色"
                                                },
                                            )
                                        },
                                    )
                                }
                            }
                        }

                        // 主题色 6 色点横排
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("主题强调色", style = OmniText.caption, color = mutedContent())
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                            ) {
                                ThemeColor.entries.forEach { color ->
                                    val selected = state.preferences.themeColor == color
                                    Column(
                                        modifier = Modifier
                                            .width(52.dp)
                                            .selectable(
                                                selected = selected,
                                                onClick = { viewModel.setThemeColor(color) },
                                                role = Role.RadioButton,
                                            ),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(4.dp),
                                    ) {
                                        Surface(
                                            modifier = Modifier
                                                .size(36.dp)
                                                .border(
                                                    width = if (selected) 2.5.dp else 0.dp,
                                                    color = MaterialTheme.colorScheme.primary,
                                                    shape = CircleShape,
                                                ),
                                            shape = CircleShape,
                                            color = themePrimaryColor(color, isPreviewDark),
                                        ) {}
                                        Text(
                                            themeColorLabel(color),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (selected) MaterialTheme.colorScheme.primary else mutedContent(),
                                            maxLines = 1,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 面板 2：记账习惯
            item {
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.elevatedCardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                    ),
                    elevation = CardDefaults.elevatedCardElevation(defaultElevation = 0.dp),
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            OmniBadgeIcon(
                                icon = Icons.Default.Receipt,
                                badgeType = SemanticBadgeType.SYSTEM,
                            )
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text("记账习惯", style = OmniText.titleRow)
                                Text("列表密度与记账呈现偏好", style = OmniText.caption, color = mutedContent())
                            }
                        }

                        // 首页明细显示模式
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("首页交易列表呈现", style = OmniText.caption, color = mutedContent())
                            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                                listOf(
                                    TransactionDetailDisplayMode.CARD to "卡片视图",
                                    TransactionDetailDisplayMode.LIST to "紧凑列表",
                                ).forEachIndexed { index, (mode, label) ->
                                    SegmentedButton(
                                        selected = state.preferences.transactionDetailDisplayMode == mode,
                                        onClick = {
                                            if (state.preferences.transactionDetailDisplayMode != mode) {
                                                viewModel.toggleDisplayMode()
                                            }
                                        },
                                        shape = SegmentedButtonDefaults.itemShape(index = index, count = 2),
                                        label = { Text(label) },
                                    )
                                }
                            }
                        }

                        // 默认首页账本范围
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("首页默认账本范围", style = OmniText.caption, color = mutedContent())
                            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                                listOf("全部账本", "当前单账本").forEachIndexed { index, label ->
                                    val isAll = state.preferences.homeLedgerScope is LedgerScope.All
                                    val isSelected = if (index == 0) isAll else !isAll
                                    SegmentedButton(
                                        selected = isSelected,
                                        onClick = {
                                            val newScope = if (index == 0) {
                                                LedgerScope.All
                                            } else {
                                                state.selectedLedgerId?.let { LedgerScope.Single(it) } ?: LedgerScope.All
                                            }
                                            viewModel.selectLedger(newScope)
                                        },
                                        shape = SegmentedButtonDefaults.itemShape(index = index, count = 2),
                                        label = { Text(label) },
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 面板 3：系统维护与偏好信息
            item {
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.elevatedCardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                    ),
                    elevation = CardDefaults.elevatedCardElevation(defaultElevation = 0.dp),
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            OmniBadgeIcon(
                                icon = Icons.Default.Tune,
                                badgeType = SemanticBadgeType.SYSTEM,
                            )
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text("系统与存储偏好", style = OmniText.titleRow)
                                Text("本地存储策略与快照轮转", style = OmniText.caption, color = mutedContent())
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                Icons.Default.CloudDone,
                                contentDescription = null,
                                tint = systemColors.content,
                                modifier = Modifier.size(20.dp),
                            )
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) {
                                Text("快照轮转保留", style = MaterialTheme.typography.labelLarge)
                                Text(
                                    "WebDAV 同步时自动保留最近 ${state.preferences.backupRetention} 份全量快照",
                                    style = OmniText.caption,
                                    color = mutedContent(),
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                Icons.Default.Alarm,
                                contentDescription = null,
                                tint = systemColors.content,
                                modifier = Modifier.size(20.dp),
                            )
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) {
                                Text("通知与提醒", style = MaterialTheme.typography.labelLarge)
                                Text(
                                    "记账与账单到期提醒已统一在「提醒」时间线中枢管理",
                                    style = OmniText.caption,
                                    color = mutedContent(),
                                )
                            }
                        }
                    }
                }
            }

            item { Spacer(Modifier.height(16.dp)) }
        }
    }
}
