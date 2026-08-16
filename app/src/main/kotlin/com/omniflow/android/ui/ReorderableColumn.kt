package com.omniflow.android.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntOffsetAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import kotlin.math.roundToInt

/**
 * 长按拖拽排序的单列列表，行高固定。
 *
 * 从记账页的分类网格里提炼出来的单列版本：长按进入拖拽（带触感反馈），
 * 拖过半行就换位，松手回调最终顺序。[resetSignal] 变化时回滚到 [items]，
 * 用来在保存失败后撤销乐观更新。
 */
@Composable
internal fun <T> ReorderableColumn(
    items: List<T>,
    key: (T) -> Any,
    rowHeight: Dp,
    onReordered: (List<T>) -> Unit,
    modifier: Modifier = Modifier,
    resetSignal: Any? = null,
    spacing: Dp = 8.dp,
    row: @Composable (item: T, dragging: Boolean) -> Unit,
) {
    val density = LocalDensity.current
    val haptics = LocalHapticFeedback.current
    val slotHeight = rowHeight + spacing
    val slotHeightPx = with(density) { slotHeight.toPx() }
    var ordered by remember(items, resetSignal) { mutableStateOf(items) }
    var draggedKey by remember { mutableStateOf<Any?>(null) }
    var dragOffset by remember { mutableStateOf(0f) }
    var changed by remember { mutableStateOf(false) }

    Box(modifier.fillMaxWidth().height(slotHeight * ordered.size.coerceAtLeast(1).toFloat())) {
        ordered.forEachIndexed { index, item ->
            key(key(item)) {
                val target = IntOffset(0, (index * slotHeightPx).roundToInt())
                val animatedTarget by animateIntOffsetAsState(target, spring(), label = "reorder-position")
                val dragging = draggedKey == key(item)
                val scale by animateFloatAsState(if (dragging) 1.03f else 1f, spring(), label = "reorder-scale")
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(rowHeight)
                        .offset { if (dragging) IntOffset(0, target.y + dragOffset.roundToInt()) else animatedTarget }
                        .zIndex(if (dragging) 2f else 0f)
                        .graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                            shadowElevation = if (dragging) 6.dp.toPx() else 0f
                        }
                        .pointerInput(key(item), ordered.size) {
                            detectDragGesturesAfterLongPress(
                                onDragStart = {
                                    draggedKey = key(item)
                                    dragOffset = 0f
                                    changed = false
                                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                },
                                onDragCancel = {
                                    ordered = items
                                    draggedKey = null
                                    dragOffset = 0f
                                    changed = false
                                },
                                onDragEnd = {
                                    if (changed) onReordered(ordered)
                                    draggedKey = null
                                    dragOffset = 0f
                                    changed = false
                                },
                                onDrag = { change, amount ->
                                    change.consume()
                                    val current = ordered.indexOfFirst { key(it) == key(item) }
                                    if (current < 0) return@detectDragGesturesAfterLongPress
                                    dragOffset += amount.y
                                    val center = current * slotHeightPx + dragOffset + slotHeightPx / 2f
                                    val targetIndex = (center / slotHeightPx).toInt().coerceIn(0, ordered.lastIndex)
                                    if (targetIndex != current) {
                                        ordered = ordered.toMutableList()
                                            .apply { add(targetIndex, removeAt(current)) }
                                        dragOffset += (current - targetIndex) * slotHeightPx
                                        changed = true
                                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    }
                                },
                            )
                        },
                ) {
                    row(item, dragging)
                }
            }
        }
    }
}
