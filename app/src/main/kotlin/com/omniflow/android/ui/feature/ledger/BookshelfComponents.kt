package com.omniflow.android.ui.feature.ledger

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.omniflow.android.ui.designsystem.tokens.getCategoryIcon
import com.omniflow.android.ui.designsystem.tokens.ledgerBadgeColors
import com.omniflow.core.domain.model.LedgerCoverSpec

/**
 * 标准 40dp 圆形账本徽章。
 * 全 App 小场景（顶栏切换条、选择器行、列表项）统一为圆形。
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
