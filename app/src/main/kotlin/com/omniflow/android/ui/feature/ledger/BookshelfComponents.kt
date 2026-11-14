package com.omniflow.android.ui.feature.ledger

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.omniflow.android.ui.designsystem.tokens.OmniColorTokens
import com.omniflow.android.ui.designsystem.tokens.getCategoryIcon
import com.omniflow.android.ui.designsystem.tokens.ledgerBadgeColors
import com.omniflow.core.domain.model.LedgerCoverSpec

internal val BookShape = RoundedCornerShape(
    topStart = 4.dp,
    topEnd = 12.dp,
    bottomEnd = 12.dp,
    bottomStart = 4.dp,
)

/**
 * 虚线新增账本占位组件 (书架末尾)。
 */
@Composable
fun DashedNewLedgerBook(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val primary = MaterialTheme.colorScheme.primary
    val strokeColor = MaterialTheme.colorScheme.outlineVariant

    Box(
        modifier = modifier
            .aspectRatio(3f / 4f)
            .clip(BookShape)
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .border(
                width = 1.5.dp,
                color = strokeColor,
                shape = BookShape,
            )
            .clickable { onClick() },
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(48.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "新建账本",
                        tint = primary,
                        modifier = Modifier.size(24.dp),
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
            Text(
                text = "新建账本",
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Medium),
                color = primary,
            )
        }
    }
}

/**
 * 书架暖色实木托板 (浅浮雕层 + 下沉阴影)。
 */
@Composable
fun ShelfPlank(
    modifier: Modifier = Modifier,
) {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val plankColor = if (isDark) OmniColorTokens.ShelfPlankDark else OmniColorTokens.ShelfPlankLight
    val shadowColor = if (isDark) OmniColorTokens.ShelfPlankShadowDark else OmniColorTokens.ShelfPlankShadowLight

    Column(modifier = modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(10.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(plankColor),
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .background(Brush.verticalGradient(listOf(shadowColor, Color.Transparent))),
        )
    }
}

/**
 * 治愈缺陷 8.2 的标准 40dp 圆形账本徽章。
 * 全 App 小场景（顶栏切换条、选择器行、列表项）强制统一为圆形，取缔方角图标。
 */
@Composable
fun OmniLedgerBadge(
    coverKey: String?,
    modifier: Modifier = Modifier,
    ledgerId: String = "",
    size: Dp = 40.dp,
    iconSize: Dp = 20.dp,
    contentDescription: String? = null,
) {
    val spec = remember(coverKey, ledgerId) { LedgerCoverSpec.parse(coverKey, ledgerId) }
    val preset = when (spec) {
        is LedgerCoverSpec.Preset -> spec
        is LedgerCoverSpec.Custom -> LedgerCoverSpec.fallback(ledgerId)
    }
    val colors = ledgerBadgeColors(preset.colorKey)

    Surface(
        modifier = modifier.size(size),
        shape = CircleShape,
        color = colors.first,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = getCategoryIcon(preset.iconKey),
                contentDescription = contentDescription,
                modifier = Modifier.size(iconSize),
                tint = colors.second,
            )
        }
    }
}
