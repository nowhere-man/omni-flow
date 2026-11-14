package com.omniflow.android.ui.designsystem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import com.omniflow.android.ui.designsystem.tokens.OmniIconTokens
import com.omniflow.android.ui.designsystem.tokens.SemanticBadgeType
import com.omniflow.android.ui.designsystem.tokens.categoryBadgeColors
import com.omniflow.android.ui.designsystem.tokens.getCategoryIcon
import com.omniflow.android.ui.designsystem.tokens.semanticBadgeColors

/**
 * Material 3 Expressive 圆形粉彩语义图标徽章。
 * 遵循 Gmail M3E 设置页规范：40dp 圆形容器底色 + 20dp 居中图标。
 */
@Composable
fun OmniBadgeIcon(
    icon: ImageVector,
    badgeType: SemanticBadgeType,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
) {
    val colors = semanticBadgeColors(badgeType)
    OmniBadgeIcon(
        icon = icon,
        containerColor = colors.container,
        contentColor = colors.content,
        modifier = modifier,
        contentDescription = contentDescription,
    )
}

@Composable
fun OmniBadgeIcon(
    icon: ImageVector,
    containerColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier,
    containerSize: Dp = OmniIconTokens.badgeContainerSize,
    iconSize: Dp = OmniIconTokens.badgeIconSize,
    contentDescription: String? = null,
) {
    Box(
        modifier = modifier
            .size(containerSize)
            .clip(CircleShape)
            .background(containerColor),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = contentColor,
            modifier = Modifier.size(iconSize),
        )
    }
}

/**
 * 分类专属 Expressive 粉彩徽章：从 CategoryColorTokens 自动获取对应的低饱和配色。
 */
@Composable
fun OmniCategoryBadgeIcon(
    iconKey: String?,
    modifier: Modifier = Modifier,
    containerSize: Dp = OmniIconTokens.badgeContainerSize,
    iconSize: Dp = OmniIconTokens.badgeIconSize,
    contentDescription: String? = null,
) {
    val colors = categoryBadgeColors(iconKey)
    OmniBadgeIcon(
        icon = getCategoryIcon(iconKey),
        containerColor = colors.container,
        contentColor = colors.content,
        modifier = modifier,
        containerSize = containerSize,
        iconSize = iconSize,
        contentDescription = contentDescription,
    )
}
