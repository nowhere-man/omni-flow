package com.omniflow.android.ui.designsystem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import kotlin.math.max

data class DivergingBarRowData(
    val id: String,
    val name: String,
    val iconKey: String?,
    val diffAmount: Long, // minor units, positive = more, negative = less
    val isIncrease: Boolean,
    val formattedDiff: String, // e.g. "+¥120.00" or "-¥85.00"
)

@Composable
fun HorizontalDivergingBarChart(
    items: List<DivergingBarRowData>,
    maxAbsDiff: Long,
    increaseColor: Color,
    decreaseColor: Color,
    modifier: Modifier = Modifier,
    iconSlot: @Composable ((String?) -> Unit)? = null,
    onItemClick: ((DivergingBarRowData) -> Unit)? = null,
) {
    val effectiveMax = max(1L, maxAbsDiff)

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        // Axis Header Guide
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Spacer(modifier = Modifier.width(92.dp))
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = "◀ 少花",
                    style = MaterialTheme.typography.labelSmall,
                    color = decreaseColor,
                )
                Text(
                    text = "0",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline,
                )
                Text(
                    text = "多花 ▶",
                    style = MaterialTheme.typography.labelSmall,
                    color = increaseColor,
                )
            }
            Spacer(modifier = Modifier.width(92.dp))
        }

        items.forEach { item ->
            val isClickable = onItemClick != null
            val rowMod = if (isClickable) {
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onItemClick?.invoke(item) }
                    .padding(vertical = 4.dp, horizontal = 4.dp)
            } else {
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp, horizontal = 4.dp)
            }

            Row(
                modifier = rowMod,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Category identity (92dp)
                Row(
                    modifier = Modifier.width(92.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    if (iconSlot != null) {
                        iconSlot(item.iconKey)
                    }
                    Text(
                        text = item.name,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                // Diverging Bar Zone
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(20.dp)
                        .padding(horizontal = 4.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    // Center 0 Axis Line
                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .fillMaxHeight()
                            .background(MaterialTheme.colorScheme.outlineVariant),
                    )

                    val ratio = (abs(item.diffAmount).toFloat() / effectiveMax.toFloat()).coerceIn(0f, 1f)
                    val barColor = if (item.isIncrease) increaseColor else decreaseColor

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        // Left Half (Decrease / Save)
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight(),
                            contentAlignment = Alignment.CenterEnd,
                        ) {
                            if (!item.isIncrease && item.diffAmount != 0L) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(fraction = ratio.coerceAtLeast(0.04f))
                                        .height(12.dp)
                                        .clip(RoundedCornerShape(topStart = 6.dp, bottomStart = 6.dp))
                                        .background(barColor),
                                )
                            }
                        }

                        // Right Half (Increase / Spend more)
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight(),
                            contentAlignment = Alignment.CenterStart,
                        ) {
                            if (item.isIncrease && item.diffAmount != 0L) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(fraction = ratio.coerceAtLeast(0.04f))
                                        .height(12.dp)
                                        .clip(RoundedCornerShape(topEnd = 6.dp, bottomEnd = 6.dp))
                                        .background(barColor),
                                )
                            }
                        }
                    }
                }

                // Diff Amount (92dp)
                Text(
                    text = item.formattedDiff,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = FontFamily.Monospace,
                    ),
                    color = if (item.isIncrease) increaseColor else decreaseColor,
                    modifier = Modifier.width(92.dp),
                    textAlign = TextAlign.End,
                    maxLines = 1,
                    softWrap = false,
                )
            }
        }
    }
}
