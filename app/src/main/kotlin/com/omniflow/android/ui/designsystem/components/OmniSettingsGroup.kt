package com.omniflow.android.ui.designsystem.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.omniflow.android.ui.designsystem.tokens.CategoryBadgeColors
import com.omniflow.android.ui.designsystem.tokens.SemanticBadgeType

/**
 * Material 3 Expressive 分组大圆角卡片容器。
 * 对应 Gmail 新版设置页模板：小节标题 + 24dp 大圆角 ElevatedCard (surfaceContainerLow)。
 */
@Composable
fun OmniSettingsGroup(
    title: String? = null,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        if (!title.isNullOrBlank()) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 8.dp),
            )
        }
        ElevatedCard(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.elevatedCardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            ),
            elevation = CardDefaults.elevatedCardElevation(
                defaultElevation = 0.5.dp,
            ),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                content()
            }
        }
    }
}

/**
 * 列表型设置行：官方 M3 ListItem 标准实现。
 * 左侧粉彩色圆形图标徽章、标题、副标题、尾随控件。
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
    val clickModifier = if (onClick != null) {
        Modifier.clickable(role = Role.Button, onClick = onClick)
    } else {
        Modifier
    }

    ListItem(
        headlineContent = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
            )
        },
        supportingContent = if (subtitle != null || content != null) {
            {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (subtitle != null) {
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodySmall,
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
        modifier = modifier.then(clickModifier),
    )
}

/**
 * 设置列表项行间细分割线，图标后缩进 56dp。
 */
@Composable
fun OmniSettingsDivider(modifier: Modifier = Modifier) {
    HorizontalDivider(
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
        modifier = modifier.padding(start = 56.dp, end = 16.dp),
    )
}
