package com.omniflow.android.ui.feature.home

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.omniflow.android.ui.OmniRadius
import com.omniflow.android.ui.OmniText
import com.omniflow.android.ui.TransactionRow
import com.omniflow.android.ui.TransactionTile
import com.omniflow.android.ui.asCompactRmb
import com.omniflow.android.ui.displayName
import com.omniflow.android.ui.expenseColor
import com.omniflow.android.ui.feature.batch.BatchSelectionState
import com.omniflow.android.ui.incomeColor
import com.omniflow.android.ui.mutedContent
import com.omniflow.android.ui.surfaceCard
import com.omniflow.core.domain.model.DayTransactionGroup
import com.omniflow.core.domain.model.Money
import com.omniflow.core.domain.model.TransactionDetailDisplayMode
import com.omniflow.core.domain.model.TransactionListItem
import com.omniflow.core.domain.model.hourMinuteText

@Composable
fun TransactionDayGroupItem(
    group: DayTransactionGroup,
    displayMode: TransactionDetailDisplayMode,
    selectionState: BatchSelectionState?,
    onEdit: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(group.date.displayName(), style = OmniText.caption, color = mutedContent())
            Spacer(Modifier.weight(1f))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                if (group.incomeTotal != Money.Zero) {
                    Text("收 ${group.incomeTotal.asCompactRmb()}", color = incomeColor(), style = OmniText.caption)
                }
                if (group.expenseTotal != Money.Zero) {
                    Text("支 ${group.expenseTotal.asCompactRmb()}", color = mutedContent(), style = OmniText.caption)
                }
            }
        }
        TransactionItems(group.items, displayMode, onEdit, selectionState)
    }
}

@Composable
fun TransactionItems(
    items: List<TransactionListItem>,
    displayMode: TransactionDetailDisplayMode,
    onEdit: (String) -> Unit,
    selectionState: BatchSelectionState? = null,
    modifier: Modifier = Modifier,
) {
    when (displayMode) {
        TransactionDetailDisplayMode.LIST -> Surface(
            modifier = modifier.fillMaxWidth(),
            shape = RoundedCornerShape(OmniRadius.medium),
            color = surfaceCard(),
        ) {
            Column(Modifier.padding(horizontal = 12.dp)) {
                items.forEachIndexed { index, item ->
                    TransactionListRow(item, selectionState, onEdit)
                    if (index != items.lastIndex) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                    }
                }
            }
        }

        TransactionDetailDisplayMode.CARD -> Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items.chunked(2).forEach { row ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    row.forEach { item ->
                        TransactionCard(item, selectionState, Modifier.weight(1f), onEdit)
                    }
                    repeat(2 - row.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TransactionListRow(
    item: TransactionListItem,
    selectionState: BatchSelectionState?,
    onEdit: (String) -> Unit,
) {
    val isSelecting = selectionState?.isActive == true
    val isSelected = selectionState?.isSelected(item.id) == true

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = {
                    if (isSelecting) {
                        selectionState?.toggle(item.id)
                    } else {
                        onEdit(item.id)
                    }
                },
                onLongClick = {
                    selectionState?.startSelection(item.id)
                },
            ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (isSelecting) {
            Checkbox(
                checked = isSelected,
                onCheckedChange = { selectionState?.toggle(item.id) },
                modifier = Modifier.padding(end = 4.dp),
            )
        }
        TransactionRow(
            iconKey = item.categoryIconKey,
            title = item.categoryDisplayName,
            subtitle = item.note,
            amount = item.amount,
            type = item.type,
            timeText = item.occurredAt.hourMinuteText(),
            dimmed = item.isExcluded,
            onClick = {
                if (isSelecting) {
                    selectionState?.toggle(item.id)
                } else {
                    onEdit(item.id)
                }
            },
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TransactionCard(
    item: TransactionListItem,
    selectionState: BatchSelectionState?,
    modifier: Modifier = Modifier,
    onEdit: (String) -> Unit,
) {
    val isSelecting = selectionState?.isActive == true
    val isSelected = selectionState?.isSelected(item.id) == true

    Box(modifier = modifier) {
        TransactionTile(
            iconKey = item.categoryIconKey,
            title = item.categoryDisplayName,
            subtitle = item.note,
            amount = item.amount,
            type = item.type,
            timeText = item.occurredAt.hourMinuteText(),
            modifier = Modifier
                .fillMaxWidth()
                .combinedClickable(
                    onClick = {
                        if (isSelecting) {
                            selectionState?.toggle(item.id)
                        } else {
                            onEdit(item.id)
                        }
                    },
                    onLongClick = {
                        selectionState?.startSelection(item.id)
                    },
                ),
            onClick = {
                if (isSelecting) {
                    selectionState?.toggle(item.id)
                } else {
                    onEdit(item.id)
                }
            },
        )
        if (isSelecting) {
            Checkbox(
                checked = isSelected,
                onCheckedChange = { selectionState?.toggle(item.id) },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(4.dp),
            )
        }
    }
}
