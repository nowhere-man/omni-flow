package com.omniflow.android.ui.designsystem.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.max

data class SideBySideBarPoint(
    val id: String,
    val label: String,
    val primaryValue: Long, // e.g. income minor
    val secondaryValue: Long, // e.g. expense minor
    val primaryFormatted: String,
    val secondaryFormatted: String,
    val balanceFormatted: String,
    val contentDescription: String,
)

@Composable
fun SideBySideBarChart(
    points: List<SideBySideBarPoint>,
    primaryColor: Color,
    secondaryColor: Color,
    modifier: Modifier = Modifier,
    chartHeight: Dp = 160.dp,
    selectedPointId: String? = null,
    onPointSelect: ((SideBySideBarPoint?) -> Unit)? = null,
    onTooltipClick: ((SideBySideBarPoint) -> Unit)? = null,
) {
    val selectedPoint = points.find { it.id == selectedPointId }
    val maxVal = max(
        1L,
        points.maxOfOrNull { max(it.primaryValue, it.secondaryValue) } ?: 1L,
    )

    Column(modifier = modifier.fillMaxWidth()) {
        // Tooltip Floating Capsule
        AnimatedVisibility(
            visible = selectedPoint != null,
            enter = fadeIn() + slideInVertically { -it / 2 },
            exit = fadeOut() + slideOutVertically { -it / 2 },
        ) {
            if (selectedPoint != null) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    tonalElevation = 1.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                        .clickable(enabled = onTooltipClick != null) {
                            onTooltipClick?.invoke(selectedPoint)
                        },
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Column {
                            Text(
                                text = selectedPoint.label,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f),
                            )
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = "收 ${selectedPoint.primaryFormatted}",
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                                    color = primaryColor,
                                )
                                Text(
                                    text = "支 ${selectedPoint.secondaryFormatted}",
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                                    color = secondaryColor,
                                )
                                Text(
                                    text = "余 ${selectedPoint.balanceFormatted}",
                                    style = MaterialTheme.typography.bodySmall,
                                )
                            }
                        }
                        if (onTooltipClick != null) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                                contentDescription = "查看流水",
                                modifier = Modifier.padding(start = 8.dp),
                                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                            )
                        }
                    }
                }
            }
        }

        // Chart Area
        if (points.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(chartHeight),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "当前周期暂无收支趋势数据",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            val isDense = points.size > 14
            val scrollState = rememberScrollState()
            val rowModifier = if (isDense) {
                Modifier
                    .fillMaxWidth()
                    .horizontalScroll(scrollState)
            } else {
                Modifier.fillMaxWidth()
            }

            Row(
                modifier = rowModifier
                    .height(chartHeight)
                    .padding(top = 8.dp, bottom = 4.dp),
                horizontalArrangement = if (isDense) Arrangement.spacedBy(10.dp) else Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom,
            ) {
                points.forEach { point ->
                    val isSelected = point.id == selectedPointId
                    val barWidth = if (isDense) 8.dp else 10.dp

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Bottom,
                        modifier = Modifier
                            .fillMaxHeight()
                            .widthIn(min = if (isDense) 24.dp else 28.dp)
                            .semantics { contentDescription = point.contentDescription }
                            .clickable {
                                onPointSelect?.invoke(if (isSelected) null else point)
                            },
                    ) {
                        // Bars container
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(2.dp),
                            verticalAlignment = Alignment.Bottom,
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                .background(
                                    if (isSelected) {
                                        MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.35f)
                                    } else {
                                        Color.Transparent
                                    },
                                )
                                .padding(horizontal = 2.dp),
                        ) {
                            // Primary Bar (Income)
                            val primaryRatio = (point.primaryValue.toFloat() / maxVal.toFloat()).coerceIn(0f, 1f)
                            Box(
                                modifier = Modifier
                                    .width(barWidth)
                                    .fillMaxHeight(fraction = primaryRatio.coerceAtLeast(0.02f))
                                    .clip(RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp))
                                    .background(primaryColor),
                            )

                            // Secondary Bar (Expense)
                            val secondaryRatio = (point.secondaryValue.toFloat() / maxVal.toFloat()).coerceIn(0f, 1f)
                            Box(
                                modifier = Modifier
                                    .width(barWidth)
                                    .fillMaxHeight(fraction = secondaryRatio.coerceAtLeast(0.02f))
                                    .clip(RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp))
                                    .background(secondaryColor),
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        // Label
                        Text(
                            text = point.label,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isSelected) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        )
                    }
                }
            }
        }
    }
}
