package com.omniflow.android.ui.designsystem.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

data class OmniBarSegment(
    val key: String,
    val weight: Float,
    val color: Color,
    val label: String = "",
)

/**
 * Material 3 Expressive 分段构成条 (资产构成、收支比例等)。
 *
 * 规格契约：
 * - 纯胶囊容器 (CircleShape)
 * - 支持各段点击联动高亮
 * - 零安全除法保护 (zero-safe)
 */
@Composable
fun OmniSegmentBar(
    segments: List<OmniBarSegment>,
    modifier: Modifier = Modifier,
    selectedKey: String? = null,
    onSegmentClick: ((OmniBarSegment) -> Unit)? = null,
    height: Dp = 14.dp,
    gap: Dp = 2.dp,
) {
    val validSegments = segments.filter { it.weight > 0f }
    val totalWeight = validSegments.sumOf { it.weight.toDouble() }.toFloat()

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceContainerHighest),
    ) {
        if (totalWeight <= 0f || validSegments.isEmpty()) {
            // 空态单条灰底
            Box(
                Modifier
                    .fillMaxWidth()
                    .fillMaxHeight()
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest),
            )
        } else {
            Row(modifier = Modifier.fillMaxWidth().fillMaxHeight()) {
                validSegments.forEachIndexed { index, segment ->
                    val isSelected = selectedKey != null && segment.key == selectedKey
                    val alpha by animateFloatAsState(
                        targetValue = if (selectedKey == null || isSelected) 1f else 0.45f,
                        label = "segment_alpha",
                    )

                    Box(
                        modifier = Modifier
                            .weight(segment.weight.coerceAtLeast(0.01f))
                            .fillMaxHeight()
                            .background(segment.color.copy(alpha = alpha))
                            .then(
                                if (onSegmentClick != null) {
                                    Modifier.clickable { onSegmentClick(segment) }
                                } else Modifier,
                            ),
                    )

                    if (index < validSegments.lastIndex && gap > 0.dp) {
                        Spacer(Modifier.width(gap))
                    }
                }
            }
        }
    }
}
