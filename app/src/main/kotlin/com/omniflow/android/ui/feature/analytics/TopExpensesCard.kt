package com.omniflow.android.ui.feature.analytics

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.omniflow.android.ui.CategoryIcon
import com.omniflow.android.ui.asRmb
import com.omniflow.android.ui.designsystem.tokens.expenseColor
import com.omniflow.android.ui.designsystem.tokens.incomeColor
import com.omniflow.core.domain.model.AppTimeZone
import com.omniflow.core.domain.model.TransactionRankingItem
import com.omniflow.core.domain.model.TransactionType
import kotlinx.datetime.toLocalDateTime

@Composable
fun TopExpensesCard(
    rankingItems: List<TransactionRankingItem>,
    selectedType: TransactionType,
    modifier: Modifier = Modifier,
    onTypeChange: ((TransactionType) -> Unit)? = null,
    onTransactionClick: ((String) -> Unit)? = null,
) {
    var expanded by remember { mutableStateOf(false) }

    val displayedItems = remember(rankingItems, expanded) {
        if (expanded) rankingItems else rankingItems.take(3)
    }

    ElevatedCard(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            // Header: Title + Type Switcher
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = if (selectedType == TransactionType.EXPENSE) "大额支出排行" else "大额收入排行",
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

            if (rankingItems.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = if (selectedType == TransactionType.EXPENSE) "当前周期无大额支出" else "当前周期无大额收入",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    displayedItems.forEachIndexed { index, item ->
                        val rank = index + 1
                        val badgeBg = when (rank) {
                            1 -> Color(0xFFFFD700).copy(alpha = 0.25f)
                            2 -> Color(0xFFC0C0C0).copy(alpha = 0.25f)
                            3 -> Color(0xFFCD7F32).copy(alpha = 0.25f)
                            else -> MaterialTheme.colorScheme.surfaceContainerHigh
                        }
                        val badgeColor = when (rank) {
                            1 -> Color(0xFFD4AF37)
                            2 -> Color(0xFF5A6B7C)
                            3 -> Color(0xFFCD7F32)
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        }

                        val dt = item.occurredAt.toLocalDateTime(AppTimeZone)
                        val dateStr = "${dt.monthNumber}月${dt.dayOfMonth}日"
                        val noteStr = if (!item.note.isNullOrBlank()) " · ${item.note}" else ""
                        val subText = "$dateStr · ${item.accountName}$noteStr"

                        ListItem(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    onTransactionClick?.invoke(item.transactionId)
                                },
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                            leadingContent = {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    // Rank Badge
                                    Box(
                                        modifier = Modifier
                                            .size(24.dp)
                                            .clip(CircleShape)
                                            .background(badgeBg),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Text(
                                            text = "$rank",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontWeight = FontWeight.Bold,
                                            ),
                                            color = badgeColor,
                                        )
                                    }
                                    CategoryIcon(iconKey = item.iconKey, size = 32)
                                }
                            },
                            headlineContent = {
                                Text(
                                    text = item.categoryDisplayName,
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            },
                            supportingContent = {
                                Text(
                                    text = subText,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            },
                            trailingContent = {
                                Text(
                                    text = item.amount.asRmb(),
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontFeatureSettings = "tnum",
                                    ),
                                    color = if (item.type == TransactionType.EXPENSE) {
                                        expenseColor()
                                    } else {
                                        incomeColor()
                                    },
                                )
                            },
                        )
                    }
                }

                // Expand / Collapse action
                if (rankingItems.size > 3) {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center,
                    ) {
                        TextButton(onClick = { expanded = !expanded }) {
                            Text(
                                text = if (expanded) "收起" else "查看全部 (${rankingItems.size}笔)",
                                style = MaterialTheme.typography.labelMedium,
                            )
                        }
                    }
                }
            }
        }
    }
}
