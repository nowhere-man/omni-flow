package com.omniflow.android.ui.designsystem.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.omniflow.android.ui.designsystem.tokens.CategoryBadgeColors
import com.omniflow.android.ui.designsystem.tokens.OmniShapeTokens
import com.omniflow.android.ui.designsystem.tokens.OmniSpacingTokens
import com.omniflow.android.ui.designsystem.tokens.OmniTypographyTokens
import com.omniflow.android.ui.designsystem.tokens.SemanticBadgeType
import com.omniflow.android.ui.designsystem.tokens.listItemContainerColor

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
 * Material 3 Expressive 分段列表分组容器（对齐 Google 账号页实测）。
 *
 * - 组内每一项是独立卡片 (listItemContainerColor)，项与项之间留 2dp 间隙透出页面背景，不使用分割线。
 * - 首项顶部、末项底部为 20dp 大圆角，组内相邻边为 4dp 小圆角；单项分组四角均为 20dp。
 * - 组与组之间的 16dp 间距由调用方列表 (OmniSpacingTokens.groupSpacing) 提供。
 */
@Composable
fun OmniSettingsGroup(
    modifier: Modifier = Modifier,
    content: OmniSettingsGroupScope.() -> Unit,
) {
    val scope = OmniSettingsGroupScopeImpl().apply(content)
    val lastIndex = scope.items.lastIndex

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(OmniSpacingTokens.intraGroupSpacing),
    ) {
        scope.items.forEachIndexed { index, itemContent ->
            Surface(
                shape = segmentedItemShape(index = index, lastIndex = lastIndex),
                color = listItemContainerColor(),
                modifier = Modifier.fillMaxWidth(),
            ) {
                itemContent()
            }
        }
    }
}

private fun segmentedItemShape(index: Int, lastIndex: Int): Shape {
    val outer = OmniShapeTokens.listItemOuterRadius
    val inner = OmniShapeTokens.listItemInnerRadius
    val top = if (index == 0) outer else inner
    val bottom = if (index == lastIndex) outer else inner
    return RoundedCornerShape(topStart = top, topEnd = top, bottomStart = bottom, bottomEnd = bottom)
}

/**
 * 列表型设置行：官方 M3 ListItem 标准实现。
 * 自身无外层独立卡片，左侧粉彩色圆形图标徽章、标题、副标题、尾随控件。
 * 底色与圆角由所属 OmniSettingsGroup 的单项卡片提供，涟漪随之裁剪。
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

/**
 * 带控件的设置行：上方为标准 OmniSettingsRow（图标 + 标题 + 副标题），
 * 下方为占满整行宽度的控件区（分段按钮、Chip、色块等），避免被图标缩进挤压。
 */
@Composable
fun OmniSettingsControlRow(
    title: String,
    icon: ImageVector,
    badgeType: SemanticBadgeType,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    control: @Composable () -> Unit,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        OmniSettingsRow(
            title = title,
            icon = icon,
            badgeType = badgeType,
            subtitle = subtitle,
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = OmniSpacingTokens.medium,
                    end = OmniSpacingTokens.medium,
                    bottom = OmniSpacingTokens.medium,
                ),
        ) {
            control()
        }
    }
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
