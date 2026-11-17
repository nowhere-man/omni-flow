package com.omniflow.android.ui.feature.more

import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Receipt
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
import com.omniflow.android.ui.designsystem.components.OmniSettingsControlRow
import com.omniflow.android.ui.designsystem.components.OmniSettingsGroup
import com.omniflow.android.ui.designsystem.components.OmniSettingsRow
import com.omniflow.android.ui.designsystem.components.OmniSubPageScaffold
import com.omniflow.android.ui.designsystem.tokens.OmniSpacingTokens
import com.omniflow.android.ui.designsystem.tokens.SemanticBadgeType
import com.omniflow.android.ui.designsystem.tokens.themePrimaryColor
import com.omniflow.android.ui.mutedContent
import com.omniflow.core.domain.model.AppearanceMode
import com.omniflow.core.domain.model.LedgerScope
import com.omniflow.core.domain.model.ThemeColor
import com.omniflow.core.domain.model.TransactionDetailDisplayMode

/**
 * 通用设置页：遵循 Final Design M 方案。
 * 采用分段列表（OmniSettingsGroup）三组：外观（含迷你实时预览）/ 记账习惯 / 系统与存储。
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
                .padding(horizontal = OmniSpacingTokens.pageHorizontal),
            verticalArrangement = Arrangement.spacedBy(OmniSpacingTokens.groupSpacing),
        ) {
            // 分组 1：外观与个性化（迷你实时预览 + 深浅外观 + 主题强调色）
            item {
                OmniSettingsGroup {
                    item {
                        Box(Modifier.padding(OmniSpacingTokens.medium)) {
                            MiniAppearanceLivePreview(
                                themeColor = state.preferences.themeColor,
                                isDark = isPreviewDark,
                            )
                        }
                    }
                    item {
                        OmniSettingsControlRow(
                            title = "深浅外观",
                            subtitle = "跟随系统或固定浅色、深色",
                            icon = Icons.Default.DarkMode,
                            badgeType = SemanticBadgeType.SYSTEM,
                        ) {
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
                    }
                    item {
                        OmniSettingsControlRow(
                            title = "主题强调色",
                            subtitle = "按钮、选中态与高亮使用的主色",
                            icon = Icons.Default.Palette,
                            badgeType = SemanticBadgeType.SYSTEM,
                        ) {
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

            // 分组 2：记账习惯
            item {
                OmniSettingsGroup {
                    item {
                        OmniSettingsControlRow(
                            title = "首页交易列表呈现",
                            subtitle = "卡片视图或紧凑列表",
                            icon = Icons.Default.Receipt,
                            badgeType = SemanticBadgeType.SYSTEM,
                        ) {
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
                    }
                    item {
                        OmniSettingsControlRow(
                            title = "首页默认账本范围",
                            subtitle = "打开首页时默认展示的账本",
                            icon = Icons.AutoMirrored.Filled.MenuBook,
                            badgeType = SemanticBadgeType.SYSTEM,
                        ) {
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

            // 分组 3：系统与存储偏好（只读说明）
            item {
                OmniSettingsGroup {
                    item {
                        OmniSettingsRow(
                            title = "快照轮转保留",
                            subtitle = "WebDAV 同步时自动保留最近 ${state.preferences.backupRetention} 份全量快照",
                            icon = Icons.Default.CloudDone,
                            badgeType = SemanticBadgeType.SYSTEM,
                        )
                    }
                    item {
                        OmniSettingsRow(
                            title = "通知与提醒",
                            subtitle = "记账与账单到期提醒已统一在「提醒」时间线中枢管理",
                            icon = Icons.Default.Alarm,
                            badgeType = SemanticBadgeType.SYSTEM,
                        )
                    }
                }
            }

            item { Spacer(Modifier.height(16.dp)) }
        }
    }
}
