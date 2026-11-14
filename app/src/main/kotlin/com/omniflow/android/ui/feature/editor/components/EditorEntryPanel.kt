package com.omniflow.android.ui.feature.editor.components

import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.omniflow.android.ui.OmniRadius
import com.omniflow.android.ui.OmniText
import com.omniflow.android.ui.TransactionEditorUiState
import com.omniflow.android.ui.amountColor
import com.omniflow.android.ui.designsystem.components.OmniDatePickerDialog
import com.omniflow.android.ui.designsystem.tokens.categoryBadgeColors
import com.omniflow.android.ui.designsystem.tokens.getCategoryIcon
import com.omniflow.android.ui.mutedContent
import com.omniflow.android.ui.surfaceCard
import com.omniflow.android.ui.surfaceInset
import com.omniflow.core.domain.model.AppTimeZone
import com.omniflow.core.domain.model.Category
import com.omniflow.core.domain.model.TransactionType
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime

@Composable
fun TransactionEntryPanel(
    state: TransactionEditorUiState,
    primary: Category?,
    selectedSecondary: Category?,
    secondaryCategories: List<Category>,
    onSecondary: (String?) -> Unit,
    onAddSecondary: () -> Unit,
    onNote: (String) -> Unit,
    onDate: (Instant) -> Unit,
    onNoteFocusChanged: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val dateTime = state.occurredAt.toLocalDateTime(AppTimeZone)
    val amountColor = amountColor(state.type)
    val isTransfer = state.type == TransactionType.TRANSFER

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(OmniRadius.medium),
        color = surfaceCard(),
    ) {
        Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                if (isTransfer) {
                    val fromName = state.accounts.firstOrNull { it.id == state.accountId }?.name ?: "转出"
                    val toName = state.accounts.firstOrNull { it.id == state.targetAccountId }?.name ?: "转入"
                    Text(
                        text = "转账：$fromName → $toName",
                        style = OmniText.bodyRow.copy(fontWeight = FontWeight.Medium),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                } else {
                    if (primary != null) {
                        val catColors = categoryBadgeColors(primary.iconKey)
                        Surface(
                            modifier = Modifier.size(28.dp),
                            shape = CircleShape,
                            color = catColors.container,
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(getCategoryIcon(primary.iconKey), contentDescription = null, Modifier.size(18.dp), tint = catColors.content)
                            }
                        }
                        Spacer(Modifier.width(6.dp))
                    }
                    Text(
                        text = primary?.let { p -> selectedSecondary?.let { "${p.name} - ${it.name}" } ?: p.name } ?: "先选择分类",
                        style = OmniText.bodyRow.copy(fontWeight = FontWeight.Medium),
                        color = if (primary == null) mutedContent() else MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                }
                Spacer(Modifier.width(8.dp))
                Text(
                    text = state.amountInput.ifBlank { "0" },
                    style = OmniText.amountHero,
                    color = amountColor,
                    maxLines = 1,
                )
            }

            state.error?.let {
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }

            if (!isTransfer && primary != null) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    secondaryCategories.forEach { category ->
                        FilterChip(
                            selected = category.id == selectedSecondary?.id,
                            onClick = { onSecondary(if (category.id == selectedSecondary?.id) primary.id else category.id) },
                            label = { Text(category.name, maxLines = 1) },
                        )
                    }
                    Surface(onClick = onAddSecondary, shape = CircleShape, color = MaterialTheme.colorScheme.secondaryContainer) {
                        Icon(Icons.Default.Add, contentDescription = "新建二级分类", modifier = Modifier.padding(8.dp).size(16.dp))
                    }
                }
            }

            Row(Modifier.fillMaxWidth().height(38.dp), verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .height(34.dp),
                    color = surfaceInset(),
                    shape = MaterialTheme.shapes.small,
                ) {
                    Row(Modifier.padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.EditNote,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.width(6.dp))
                        Box(Modifier.weight(1f)) {
                            if (state.note.isBlank()) {
                                Text("备注", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                            }
                            BasicTextField(
                                value = state.note,
                                onValueChange = onNote,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .onFocusChanged { onNoteFocusChanged(it.isFocused) },
                                singleLine = true,
                                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                                textStyle = TextStyle(
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontSize = MaterialTheme.typography.bodySmall.fontSize,
                                ),
                            )
                        }
                    }
                }
                Spacer(Modifier.width(6.dp))
                var showDatePicker by remember { mutableStateOf(false) }
                Surface(
                    onClick = { showDatePicker = true },
                    modifier = Modifier.height(34.dp),
                    color = surfaceInset(),
                    shape = MaterialTheme.shapes.small,
                ) {
                    Row(
                        Modifier.padding(horizontal = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Icon(Icons.Default.CalendarMonth, contentDescription = "选择日期", modifier = Modifier.size(16.dp))
                        Text(
                            "${dateTime.monthNumber}月${dateTime.dayOfMonth}日",
                            style = MaterialTheme.typography.labelLarge,
                            maxLines = 1,
                        )
                    }
                }
                if (showDatePicker) {
                    OmniDatePickerDialog(
                        initialDate = dateTime.date,
                        onDateSelected = { selectedDate ->
                            onDate(LocalDateTime(selectedDate, dateTime.time).toInstant(AppTimeZone))
                        },
                        onDismissRequest = { showDatePicker = false },
                    )
                }
            }
        }
    }
}

@Composable
fun NoteInputBar(onDone: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceContainer,
        tonalElevation = 2.dp,
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "输入备注后点完成回到数字键盘",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = onDone) { Text("完成") }
        }
    }
}
