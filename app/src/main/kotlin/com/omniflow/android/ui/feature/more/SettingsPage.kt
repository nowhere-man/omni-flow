package com.omniflow.android.ui.feature.more

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.omniflow.android.ui.MoreUiState
import com.omniflow.android.ui.OmniFlowViewModel
import com.omniflow.android.ui.designsystem.components.OmniExpressiveSwitch
import com.omniflow.android.ui.designsystem.components.OmniSettingsDivider
import com.omniflow.android.ui.designsystem.components.OmniSettingsGroup
import com.omniflow.android.ui.designsystem.components.OmniSettingsRow
import com.omniflow.android.ui.designsystem.tokens.SemanticBadgeType
import com.omniflow.android.ui.themePrimaryColor
import com.omniflow.core.domain.model.AppearanceMode
import com.omniflow.core.domain.model.ThemeColor

/**
 * 系统设置页：遵循 Gmail M3 Expressive 规范。
 * 使用分组大圆角卡片、M3 ListItem、Expressive 开关与 M3 标准分段按钮。
 */
@Composable
fun SettingsPage(
    state: MoreUiState,
    viewModel: OmniFlowViewModel,
    dynamicColorEnabled: Boolean,
    onDynamicColorChanged: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var cacheSubtitle by remember { mutableStateOf("清理导入临时文件与网络缓存") }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // 卡片 1：隐私与安全
        item {
            OmniSettingsGroup(title = "隐私与安全") {
                OmniSettingsRow(
                    title = "应用锁",
                    icon = Icons.Default.Lock,
                    badgeType = SemanticBadgeType.SYSTEM,
                    subtitle = "启动或回到前台时验证设备凭据",
                    trailing = {
                        OmniExpressiveSwitch(
                            checked = state.preferences.appLockEnabled,
                            onCheckedChange = viewModel::setAppLockEnabled,
                        )
                    },
                )
            }
        }

        // 卡片 2：外观与个性化
        item {
            OmniSettingsGroup(title = "外观与个性化") {
                OmniSettingsRow(
                    title = "界面外观",
                    icon = Icons.Default.DarkMode,
                    badgeType = SemanticBadgeType.SYSTEM,
                    subtitle = "跟随系统、浅色或深色",
                    content = {
                        SingleChoiceSegmentedButtonRow(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp),
                        ) {
                            for ((index, mode) in AppearanceMode.entries.withIndex()) {
                                SegmentedButton(
                                    selected = state.preferences.appearanceMode == mode,
                                    onClick = { viewModel.setAppearanceMode(mode) },
                                    shape = SegmentedButtonDefaults.itemShape(
                                        index = index,
                                        count = AppearanceMode.entries.size,
                                    ),
                                    label = {
                                        Text(
                                            when (mode) {
                                                AppearanceMode.SYSTEM -> "系统"
                                                AppearanceMode.LIGHT -> "浅色"
                                                AppearanceMode.DARK -> "深色"
                                            },
                                            style = MaterialTheme.typography.labelMedium,
                                        )
                                    },
                                )
                            }
                        }
                    },
                )
                OmniSettingsDivider()
                OmniSettingsRow(
                    title = "主题色",
                    icon = Icons.Default.Palette,
                    badgeType = SemanticBadgeType.SYSTEM,
                    subtitle = "选择按钮和导航的全局强调色",
                    content = {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.padding(top = 8.dp),
                        ) {
                            items(ThemeColor.entries, key = { it.name }) { color ->
                                val selected = state.preferences.themeColor == color
                                Column(
                                    modifier = Modifier
                                        .width(60.dp)
                                        .selectable(
                                            selected = selected,
                                            onClick = { viewModel.setThemeColor(color) },
                                            role = Role.RadioButton,
                                        ),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(6.dp),
                                ) {
                                    Surface(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .border(
                                                width = if (selected) 3.dp else 0.dp,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                shape = CircleShape,
                                            ),
                                        shape = CircleShape,
                                        color = themePrimaryColor(color, false),
                                    ) {}
                                    Text(
                                        themeColorLabel(color),
                                        style = MaterialTheme.typography.bodySmall,
                                        maxLines = 1,
                                    )
                                }
                            }
                        }
                    },
                )
                OmniSettingsDivider()
                OmniSettingsRow(
                    title = "动态取色",
                    icon = Icons.Default.ColorLens,
                    badgeType = SemanticBadgeType.SYSTEM,
                    subtitle = "使用 Android 系统壁纸生成 Material 3 配色（Android 12+）",
                    trailing = {
                        OmniExpressiveSwitch(
                            checked = dynamicColorEnabled,
                            onCheckedChange = onDynamicColorChanged,
                        )
                    },
                )
            }
        }

        // 卡片 3：系统维护
        item {
            OmniSettingsGroup(title = "系统维护") {
                OmniSettingsRow(
                    title = "清理临时缓存",
                    icon = Icons.Default.Storage,
                    badgeType = SemanticBadgeType.SYSTEM,
                    subtitle = cacheSubtitle,
                    trailing = {
                        OutlinedButton(
                            onClick = {
                                val deleted = context.cacheDir.deleteRecursively()
                                cacheSubtitle = if (deleted) "临时缓存已成功清理" else "清理完成"
                            },
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                            modifier = Modifier.height(32.dp),
                        ) {
                            Text("清理", style = MaterialTheme.typography.labelMedium)
                        }
                    },
                )
            }
        }

        item { Spacer(Modifier.height(24.dp)) }
    }
}

private fun themeColorLabel(color: ThemeColor): String = when (color) {
    ThemeColor.MIST_BLUE -> "雾蓝"
    ThemeColor.SAGE -> "鼠尾草"
    ThemeColor.LAVENDER -> "薰衣草"
    ThemeColor.SOFT_CORAL -> "柔珊瑚"
    ThemeColor.WARM_AMBER -> "暖琥珀"
    ThemeColor.GRAPHITE -> "石墨灰"
}
