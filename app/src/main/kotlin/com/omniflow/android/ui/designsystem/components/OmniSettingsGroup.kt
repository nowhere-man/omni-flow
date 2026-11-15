package com.omniflow.android.ui.designsystem.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
 * Material 3 Expressive 设置分组作用域。
 */
interface OmniSettingsGroupScope {
    fun item(content: @Composable () -> Unit)
}

private class OmniSettingsGroupScopeImpl : OmniSettingsGroupScope {
    val items = mutableListOf<@Composable () -> Unit>()
    override fun item(content: @Composable () -> Unit) {
        items.add(content)
    }
}

/**
 * Material 3 Expressive 设置分组容器。
 *
 * 规范遵循 Google 账号卡片分组规范（对齐 M3 标准）：
 * - 同一分组包裹在统一的 24dp 圆角卡片中 (surfaceContainerLow)。
 * - 组内只有第一项顶部和最后一项底部是圆角，中间项为矩形直角。
 * - 组内相邻项之间由细分割线 (HorizontalDivider) 连接，组与组之间保持 16dp 间距。
 * - 单项目分组自动呈现全圆角 (24dp)。
 */
@Composable
fun OmniSettingsGroup(
    modifier: Modifier = Modifier,
    content: OmniSettingsGroupScope.() -> Unit,
) {
    val scope = OmniSettingsGroupScopeImpl().apply(content)

    ElevatedCard(
        modifier = modifier.fillMaxWidth(),
        shape = OmniShapeTokens.card,
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
        elevation = CardDefaults.elevatedCardElevation(
            defaultElevation = 0.dp,
        ),
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            scope.items.forEachIndexed { index, itemContent ->
                if (index > 0) {
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
                        thickness = 0.5.dp,
                    )
                }
                itemContent()
            }
        }
    }
}

/**
 * 列表型设置行：官方 M3 ListItem 标准实现。
 * 自身无外层独立卡片，左侧粉彩色圆形图标徽章、标题、副标题、尾随控件。
 * 遵循所属 OmniSettingsGroup 的圆角边界与涟漪范围。
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
    Surface(
        onClick = onClick ?: {},
        enabled = onClick != null,
        color = Color.Transparent,
        modifier = modifier.fillMaxWidth(),
    ) {
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
}
