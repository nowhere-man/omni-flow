package com.omniflow.android.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

/**
 * 单个药丸容器 + 内嵌选中块的分段控件，取代一排 FilterChip。
 * FilterChip 每个都自带边框和勾选图标，几个并排就显得碎；分段控件只有一个外框，
 * 选中态是容器内的一块，视觉上更整。
 */
@Composable
internal fun <T> OmniSegmented(
    options: List<T>,
    selected: T,
    label: (T) -> String,
    onSelected: (T) -> Unit,
    modifier: Modifier = Modifier,
    selectedContainer: (@Composable (T) -> Color)? = null,
) {
    Surface(
        modifier = modifier.height(40.dp),
        shape = CircleShape,
        color = surfaceInset(),
    ) {
        Row(Modifier.padding(3.dp)) {
            options.forEach { option ->
                val isSelected = option == selected
                val container = when {
                    !isSelected -> Color.Transparent
                    selectedContainer != null -> selectedContainer(option)
                    else -> MaterialTheme.colorScheme.surfaceBright
                }
                Surface(
                    onClick = { onSelected(option) },
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    shape = CircleShape,
                    color = container,
                    contentColor = if (isSelected) {
                        MaterialTheme.colorScheme.onSurface
                    } else {
                        mutedContent()
                    },
                ) {
                    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text(
                            label(option),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
    }
}
