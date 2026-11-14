package com.omniflow.android.ui.feature.analytics

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.omniflow.android.ui.CategoryIcon
import com.omniflow.android.ui.asRmb
import com.omniflow.android.ui.designsystem.components.DonutChart
import com.omniflow.android.ui.designsystem.components.DonutSliceData
import com.omniflow.core.domain.model.CategoryBreakdownItem
import com.omniflow.core.domain.model.Money
import com.omniflow.core.domain.model.TransactionType
import java.util.Locale

private val CategoryPalette = listOf(
    Color(0xFF4E79A7),
    Color(0xFFF28E2B),
    Color(0xFFE15759),
    Color(0xFF76B7B2),
    Color(0xFF59A14F),
    Color(0xFFEDC948),
    Color(0xFFB07AA1),
    Color(0xFFFF9DA7),
    Color(0xFF9C755F),
    Color(0xFFBAB0AC),
)

private data class DisplayCategoryRow(
    val categoryId: String,
    val displayName: String,
    val iconKey: String?,
    val amount: Money,
    val ratio: Float,
    val percentageText: String,
    val color: Color,
)

@Composable
fun CategoryShareCard(
    categoryBreakdowns: List<CategoryBreakdownItem>,
    selectedType: TransactionType,
    modifier: Modifier = Modifier,
    onTypeChange: ((TransactionType) -> Unit)? = null,
    onCategoryClick: ((String) -> Unit)? = null,
) {
    var includeSecondary by remember { mutableStateOf(false) }
    var selectedSliceId by remember { mutableStateOf<String?>(null) }

    val totalAmount = remember(categoryBreakdowns) {
        categoryBreakdowns.fold(Money.Zero) { acc, item -> acc + item.amount }
    }

    val displayRows = remember(categoryBreakdowns, includeSecondary) {
        if (!includeSecondary) {
            categoryBreakdowns.mapIndexed { index, item ->
                DisplayCategoryRow(
                    categoryId = item.primaryCategoryId,
                    displayName = item.primaryCategoryName,
                    iconKey = item.iconKey,
                    amount = item.amount,
                    ratio = item.shareRatio.toFloat(),
                    percentageText = String.format(Locale.US, "%.1f%%", item.shareRatio * 100),
                    color = CategoryPalette[index % CategoryPalette.size],
                )
            }
        } else {
            val list = mutableListOf<DisplayCategoryRow>()
            var colorIdx = 0
            categoryBreakdowns.forEach { primary ->
                if (primary.secondaryCategories.isEmpty()) {
                    list.add(
                        DisplayCategoryRow(
                            categoryId = primary.primaryCategoryId,
                            displayName = primary.primaryCategoryName,
                            iconKey = primary.iconKey,
                            amount = primary.amount,
                            ratio = primary.shareRatio.toFloat(),
                            percentageText = String.format(Locale.US, "%.1f%%", primary.shareRatio * 100),
                            color = CategoryPalette[colorIdx % CategoryPalette.size],
                        ),
                    )
                    colorIdx++
                } else {
                    primary.secondaryCategories.forEach { sec ->
                        list.add(
                            DisplayCategoryRow(
                                categoryId = sec.categoryId,
                                displayName = "${primary.primaryCategoryName} - ${sec.categoryName}",
                                iconKey = sec.iconKey ?: primary.iconKey,
                                amount = sec.amount,
                                ratio = sec.shareRatio.toFloat(),
                                percentageText = String.format(Locale.US, "%.1f%%", sec.shareRatio * 100),
                                color = CategoryPalette[colorIdx % CategoryPalette.size],
                            ),
                        )
                        colorIdx++
                    }
                }
            }
            list.sortedByDescending { it.amount }
        }
    }

    val donutSlices = remember(displayRows) {
        displayRows.take(8).map { row ->
            DonutSliceData(
                id = row.categoryId,
                value = row.amount.minor.toDouble(),
                color = row.color,
                label = row.displayName,
            )
        }
    }

    ElevatedCard(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Header: Title + Type Switcher (Expense / Income)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = "分类占比",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                )

                if (onTypeChange != null) {
                    val types = listOf(TransactionType.EXPENSE, TransactionType.INCOME)
                    SingleChoiceSegmentedButtonRow(
                        modifier = Modifier.height(32.dp),
                    ) {
                        types.forEachIndexed { index, type ->
                            SegmentedButton(
                                shape = SegmentedButtonDefaults.itemShape(index = index, count = types.size),
                                onClick = { onTypeChange(type) },
                                selected = selectedType == type,
                                label = {
                                    Text(
                                        text = if (type == TransactionType.EXPENSE) "支出" else "收入",
                                        style = MaterialTheme.typography.labelSmall,
                                    )
                                },
                            )
                        }
                    }
                }
            }

            if (categoryBreakdowns.isEmpty() || totalAmount.minor == 0L) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "当前周期无记账分类",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                // Donut Chart Centered
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center,
                ) {
                    DonutChart(
                        slices = donutSlices,
                        chartSize = 180.dp,
                        strokeWidth = 26.dp,
                        gapDegrees = 2f,
                        selectedId = selectedSliceId,
                        onSliceClick = { slice ->
                            selectedSliceId = if (selectedSliceId == slice.id) null else slice.id
                        },
                        centerContent = {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = totalAmount.asRmb(),
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontFeatureSettings = "tnum",
                                    ),
                                )
                                Text(
                                    text = "共 ${displayRows.size} 个分类",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        },
                    )
                }

                // Secondary toggle row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    FilterChip(
                        selected = includeSecondary,
                        onClick = { includeSecondary = !includeSecondary },
                        label = {
                            Text(
                                text = if (includeSecondary) "含二级分类" else "仅一级分类",
                                style = MaterialTheme.typography.labelSmall,
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(),
                    )
                }

                // List Items
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    displayRows.forEach { item ->
                        val isSelected = item.categoryId == selectedSliceId
                        ListItem(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    onCategoryClick?.invoke(item.categoryId)
                                },
                            colors = ListItemDefaults.colors(
                                containerColor = if (isSelected) {
                                    MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
                                } else {
                                    Color.Transparent
                                },
                            ),
                            leadingContent = {
                                CategoryIcon(iconKey = item.iconKey, size = 32)
                            },
                            headlineContent = {
                                Text(
                                    text = item.displayName,
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                    maxLines = 1,
                                )
                            },
                            supportingContent = {
                                LinearProgressIndicator(
                                    progress = { item.ratio.coerceIn(0f, 1f) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(6.dp)
                                        .clip(RoundedCornerShape(3.dp)),
                                    color = item.color,
                                    trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                                    strokeCap = StrokeCap.Round,
                                )
                            },
                            trailingContent = {
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = item.amount.asRmb(),
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = FontWeight.SemiBold,
                                            fontFeatureSettings = "tnum",
                                        ),
                                    )
                                    Text(
                                        text = item.percentageText,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontFeatureSettings = "tnum",
                                        ),
                                    )
                                }
                            },
                        )
                    }
                }
            }
        }
    }
}
