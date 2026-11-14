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
import androidx.compose.ui.graphics.vector.ImageVector
import com.omniflow.android.ui.designsystem.tokens.OmniIconTokens
import com.omniflow.android.ui.designsystem.tokens.SemanticBadgeType
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
    Box(
        modifier = modifier
            .size(OmniIconTokens.badgeContainerSize)
            .clip(CircleShape)
            .background(colors.container),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = colors.content,
            modifier = Modifier.size(OmniIconTokens.badgeIconSize),
        )
    }
}
