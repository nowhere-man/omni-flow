package com.omniflow.android.ui.feature.editor.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntOffsetAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.omniflow.android.ui.designsystem.tokens.categoryBadgeColors
import com.omniflow.android.ui.designsystem.tokens.getCategoryIcon
import com.omniflow.android.ui.surfaceInset
import com.omniflow.core.domain.model.Category
import kotlin.math.ceil
import kotlin.math.roundToInt

private val MaxCategoryCell = 72.dp
private val MinCategoryCell = 56.dp

@Composable
fun PagedCategoryGrid(
    categories: List<Category>,
    selectedId: String?,
    onSelected: (String?) -> Unit,
    onReordered: (List<String>) -> Unit,
    resetSignal: Any?,
    rowsPerPage: Int,
    modifier: Modifier = Modifier,
) {
    val columns = 5
    val perPage = columns * rowsPerPage
    val density = LocalDensity.current
    val haptics = LocalHapticFeedback.current
    var width by remember { mutableIntStateOf(0) }
    var ordered by remember(categories, resetSignal) { mutableStateOf(categories) }
    var draggedId by remember { mutableStateOf<String?>(null) }
    var dragOffset by remember { mutableStateOf(Offset.Zero) }
    var changed by remember { mutableStateOf(false) }
    val pageCount = ceil(ordered.size / perPage.toFloat()).toInt().coerceAtLeast(1)
    val pagerState = rememberPagerState(pageCount = { pageCount })

    BoxWithConstraints(modifier.fillMaxWidth()) {
        val dotsHeight = if (pageCount > 1) 16.dp else 0.dp
        val cellHeight = ((maxHeight - dotsHeight) / rowsPerPage).coerceIn(MinCategoryCell, MaxCategoryCell)
        val cellHeightPx = with(density) { cellHeight.toPx() }
        Column(Modifier.fillMaxWidth()) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(cellHeight * rowsPerPage.toFloat()),
                userScrollEnabled = draggedId == null,
                verticalAlignment = Alignment.Top,
            ) { page ->
                val pageItems = ordered.drop(page * perPage).take(perPage)
                val pageRows = ceil(pageItems.size / columns.toFloat()).toInt().coerceAtLeast(1)
                Box(Modifier.fillMaxSize().onSizeChanged { width = it.width }) {
                    if (width > 0) {
                        val cellWidthPx = width.toFloat() / columns
                        val cellWidth = with(density) { cellWidthPx.toDp() }
                        fun position(index: Int) = IntOffset(
                            x = ((index % columns) * cellWidthPx).roundToInt(),
                            y = ((index / columns) * cellHeightPx).roundToInt(),
                        )
                        pageItems.forEachIndexed { indexInPage, category ->
                            key(category.id) {
                                val target = position(indexInPage)
                                val animatedTarget by animateIntOffsetAsState(target, spring(), label = "category-position")
                                val dragging = draggedId == category.id
                                val scale by animateFloatAsState(if (dragging) 1.08f else 1f, spring(), label = "category-scale")
                                CategoryTile(
                                    category = category,
                                    selected = selectedId == category.id,
                                    dragging = dragging,
                                    onClick = { onSelected(category.id) },
                                    modifier = Modifier
                                        .offset {
                                            if (dragging) {
                                                IntOffset(target.x + dragOffset.x.roundToInt(), target.y + dragOffset.y.roundToInt())
                                            } else animatedTarget
                                        }
                                        .width(cellWidth)
                                        .height(cellHeight)
                                        .padding(2.dp)
                                        .zIndex(if (dragging) 2f else 0f)
                                        .graphicsLayer {
                                            scaleX = scale
                                            scaleY = scale
                                            shadowElevation = if (dragging) 4.dp.toPx() else 0f
                                        }
                                        .pointerInput(category.id, width, page) {
                                            detectDragGesturesAfterLongPress(
                                                onDragStart = {
                                                    draggedId = category.id
                                                    dragOffset = Offset.Zero
                                                    changed = false
                                                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                                },
                                                onDragCancel = {
                                                    ordered = categories
                                                    draggedId = null
                                                    dragOffset = Offset.Zero
                                                    changed = false
                                                },
                                                onDragEnd = {
                                                    if (changed) onReordered(ordered.map(Category::id))
                                                    draggedId = null
                                                    dragOffset = Offset.Zero
                                                    changed = false
                                                },
                                                onDrag = { change, amount ->
                                                    change.consume()
                                                    val pageOffset = page * perPage
                                                    val currentIndex = ordered.indexOfFirst { it.id == category.id }
                                                    if (currentIndex < 0) return@detectDragGesturesAfterLongPress
                                                    val currentInPage = currentIndex - pageOffset
                                                    if (currentInPage !in pageItems.indices) return@detectDragGesturesAfterLongPress
                                                    dragOffset += amount
                                                    val oldBase = position(currentInPage)
                                                    val centerX = oldBase.x + dragOffset.x + cellWidthPx / 2f
                                                    val centerY = oldBase.y + dragOffset.y + cellHeightPx / 2f
                                                    val column = (centerX / cellWidthPx).toInt().coerceIn(0, columns - 1)
                                                    val row = (centerY / cellHeightPx).toInt().coerceIn(0, pageRows - 1)
                                                    val targetInPage = (row * columns + column).coerceIn(0, pageItems.lastIndex)
                                                    if (targetInPage != currentInPage) {
                                                        ordered = ordered.toMutableList()
                                                            .apply { add(targetInPage + pageOffset, removeAt(currentIndex)) }
                                                        val newBase = position(targetInPage)
                                                        dragOffset += Offset(
                                                            (oldBase.x - newBase.x).toFloat(),
                                                            (oldBase.y - newBase.y).toFloat(),
                                                        )
                                                        changed = true
                                                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                    }
                                                },
                                            )
                                        },
                                )
                            }
                        }
                    }
                }
            }
            if (pageCount > 1) {
                Row(
                    Modifier.fillMaxWidth().height(16.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    repeat(pageCount) { page ->
                        val active = pagerState.currentPage == page
                        Box(
                            Modifier
                                .padding(horizontal = 3.dp)
                                .size(if (active) 7.dp else 5.dp)
                                .clip(CircleShape)
                                .background(
                                    if (active) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.outlineVariant
                                    },
                                ),
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CategoryTile(
    category: Category,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    dragging: Boolean = false,
) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        color = if (selected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.75f) else Color.Transparent,
        border = if (selected) BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else null,
        tonalElevation = if (dragging) 8.dp else 0.dp,
        onClick = onClick,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterVertically),
            ) {
                val badgeColors = categoryBadgeColors(category.iconKey)
                Surface(
                    modifier = Modifier.size(40.dp),
                    shape = CircleShape,
                    color = if (selected) Color.Transparent else badgeColors.container,
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = getCategoryIcon(category.iconKey),
                            contentDescription = null,
                            modifier = Modifier.size(24.dp),
                            tint = if (selected) MaterialTheme.colorScheme.primary else badgeColors.content,
                        )
                    }
                }
                Text(
                    category.name,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                )
            }
        }
    }
}

@Composable
fun NewSecondaryCategoryDialog(
    primaryName: String,
    onDismiss: () -> Unit,
    onCreate: (String) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("$primaryName · 新建二级分类") },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("分类名称") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
        },
        confirmButton = {
            Button(onClick = { onCreate(name.trim()) }, enabled = name.isNotBlank()) { Text("创建") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } },
    )
}
