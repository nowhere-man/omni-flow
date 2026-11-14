package com.omniflow.android.ui.designsystem.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

data class DonutSliceData(
    val id: String,
    val value: Double,
    val color: Color,
    val label: String,
)

@Composable
fun DonutChart(
    slices: List<DonutSliceData>,
    modifier: Modifier = Modifier,
    chartSize: Dp = 180.dp,
    strokeWidth: Dp = 26.dp,
    gapDegrees: Float = 2f,
    selectedId: String? = null,
    onSliceClick: ((DonutSliceData) -> Unit)? = null,
    centerContent: @Composable () -> Unit = {},
) {
    val density = LocalDensity.current
    val strokeWidthPx = with(density) { strokeWidth.toPx() }
    val emptyRingColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)

    Box(
        modifier = modifier.size(chartSize),
        contentAlignment = Alignment.Center,
    ) {
        val total = slices.sumOf { it.value }

        Canvas(
            modifier = Modifier
                .size(chartSize)
                .pointerInput(slices, total) {
                    if (onSliceClick != null && total > 0.0) {
                        detectTapGestures { tapOffset ->
                            val center = Offset(size.width / 2f, size.height / 2f)
                            val dx = tapOffset.x - center.x
                            val dy = tapOffset.y - center.y
                            val dist = kotlin.math.hypot(dx, dy)
                            val outerRadius = size.width / 2f
                            val innerRadius = outerRadius - strokeWidthPx

                            if (dist in innerRadius..outerRadius) {
                                var angle = (atan2(dy, dx) * 180f / PI.toFloat() + 360f + 90f) % 360f
                                val totalGaps = slices.size * gapDegrees
                                val availableDegrees = (360f - totalGaps).coerceAtLeast(0f)
                                var currentAngle = 0f

                                for (slice in slices) {
                                    val sliceSweep = (slice.value / total * availableDegrees).toFloat()
                                    if (angle in currentAngle..(currentAngle + sliceSweep)) {
                                        onSliceClick(slice)
                                        break
                                    }
                                    currentAngle += sliceSweep + gapDegrees
                                }
                            }
                        }
                    }
                },
        ) {
            val canvasSize = size.minDimension
            val radius = (canvasSize - strokeWidthPx) / 2f
            val arcSize = Size(radius * 2f, radius * 2f)
            val topLeft = Offset((size.width - arcSize.width) / 2f, (size.height - arcSize.height) / 2f)

            if (slices.isEmpty() || total <= 0.0) {
                drawArc(
                    color = emptyRingColor,
                    startAngle = 0f,
                    sweepAngle = 360f,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokeWidthPx, cap = StrokeCap.Round),
                )
            } else {
                val totalGaps = slices.size * gapDegrees
                val availableDegrees = (360f - totalGaps).coerceAtLeast(0f)
                var startAngle = -90f

                slices.forEach { slice ->
                    val sweep = (slice.value / total * availableDegrees).toFloat()
                    val isSelected = slice.id == selectedId
                    val currentStroke = if (isSelected) strokeWidthPx * 1.15f else strokeWidthPx

                    drawArc(
                        color = slice.color,
                        startAngle = startAngle,
                        sweepAngle = sweep.coerceAtLeast(0.5f),
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = currentStroke, cap = StrokeCap.Round),
                    )
                    startAngle += sweep + gapDegrees
                }
            }
        }

        Box(contentAlignment = Alignment.Center) {
            centerContent()
        }
    }
}
