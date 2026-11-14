package com.omniflow.android.ui.designsystem.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.omniflow.android.ui.designsystem.tokens.CategoryBadgeColors
import com.omniflow.android.ui.designsystem.tokens.OmniShapeTokens
import com.omniflow.android.ui.designsystem.tokens.OmniSpacingTokens
import com.omniflow.android.ui.designsystem.tokens.OmniTypographyTokens
import com.omniflow.android.ui.designsystem.tokens.SemanticBadgeType

/**
 * Material 3 Expressive 设置分组容器。
 *
 * 依据 Jack 2026-11-15 决策（推翻第 5 轮贴合大卡片方案）：
 * 组内各行不再是同一卡片内的切片，而是各自渲染为独立圆角卡片，
 * 行与行之间留出 8.dp 页面底色间隙 (intraGroupSpacing)；组与组之间保持 16.dp 间距 (groupSpacing)。
 */
@Composable
fun OmniSettingsGroup(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(OmniSpacingTokens.intraGroupSpacing),
        content = content,
    )
}

/**
 * 列表型设置行：官方 M3 ListItem 标准实现。
 * 自身为独立 24dp 大圆角卡片 (surfaceContainerLow)，左侧粉彩色圆形图标徽章、标题、副标题、尾随控件。
 */
@Composable
fun OmniSettingsRow(
    title: String,
    icon: ImageVector,
    badgeType: SemanticBadgeType,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    onClick: (() -> Unit)? = null,
    trailing: @Composable (() -> Unit)? = null,
    content: @Composable (() -> Unit)? = null,
) {
    OmniSettingsRowInternal(
        title = title,
        leading = { OmniBadgeIcon(icon = icon, badgeType = badgeType) },
        modifier = modifier,
        subtitle = subtitle,
        onClick = onClick,
        trailing = trailing,
        content = content,
    )
}

@Composable
fun OmniSettingsRow(
    title: String,
    icon: ImageVector,
    badgeColors: CategoryBadgeColors,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    onClick: (() -> Unit)? = null,
    trailing: @Composable (() -> Unit)? = null,
    content: @Composable (() -> Unit)? = null,
) {
    OmniSettingsRowInternal(
        title = title,
        leading = {
            OmniBadgeIcon(
                icon = icon,
                containerColor = badgeColors.container,
                contentColor = badgeColors.content,
            )
        },
        modifier = modifier,
        subtitle = subtitle,
        onClick = onClick,
        trailing = trailing,
        content = content,
    )
}

@Composable
private fun OmniSettingsRowInternal(
    title: String,
    leading: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    onClick: (() -> Unit)? = null,
    trailing: @Composable (() -> Unit)? = null,
    content: @Composable (() -> Unit)? = null,
) {
    val cardContent = @Composable {
        ListItem(
            headlineContent = {
                Text(
                    text = title,
                    style = OmniTypographyTokens.settingsRowTitle,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            },
            supportingContent = if (subtitle != null || content != null) {
                {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        if (subtitle != null) {
                            Text(
                                text = subtitle,
                                style = OmniTypographyTokens.settingsRowSubtitle,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        content?.invoke()
                    }
                }
            } else null,
            leadingContent = leading,
            trailingContent = trailing,
            colors = ListItemDefaults.colors(
                containerColor = Color.Transparent,
            ),
        )
    }

    if (onClick != null) {
        ElevatedCard(
            onClick = onClick,
            shape = OmniShapeTokens.card,
            colors = CardDefaults.elevatedCardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            ),
            elevation = CardDefaults.elevatedCardElevation(
                defaultElevation = 0.dp,
            ),
            modifier = modifier.fillMaxWidth(),
        ) {
            cardContent()
        }
    } else {
        ElevatedCard(
            shape = OmniShapeTokens.card,
            colors = CardDefaults.elevatedCardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            ),
            elevation = CardDefaults.elevatedCardElevation(
                defaultElevation = 0.dp,
            ),
            modifier = modifier.fillMaxWidth(),
        ) {
            cardContent()
        }
    }
}
