package com.omniflow.android.ui.feature.editor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.omniflow.android.ui.LedgerScopePill
import com.omniflow.android.ui.OmniText
import com.omniflow.android.ui.TransactionEditorUiState
import com.omniflow.android.ui.feature.editor.components.EditorActionButtons
import com.omniflow.android.ui.feature.editor.components.EditorAttributeRow
import com.omniflow.android.ui.feature.editor.components.EditorKeypad
import com.omniflow.android.ui.feature.editor.components.NewSecondaryCategoryDialog
import com.omniflow.android.ui.feature.editor.components.NoteInputBar
import com.omniflow.android.ui.feature.editor.components.PagedCategoryGrid
import com.omniflow.android.ui.feature.editor.components.TransactionEntryPanel
import com.omniflow.android.ui.mutedContent
import com.omniflow.core.domain.model.Account
import com.omniflow.core.domain.model.TransactionType
import kotlinx.datetime.Instant

@Composable
fun TransactionEditorContent(
    state: TransactionEditorUiState,
    onType: (TransactionType) -> Unit,
    onLedger: (String?) -> Unit,
    onAccount: (Account) -> Unit,
    onTargetAccount: (Account) -> Unit,
    onCategory: (String?) -> Unit,
    onReorderPrimary: (List<String>) -> Unit,
    onCreateSecondary: (String) -> Unit,
    onTag: (String) -> Unit,
    onNote: (String) -> Unit,
    onDate: (Instant) -> Unit,
    onExcluded: (Boolean) -> Unit,
    onAmountKey: (String) -> Unit,
    onSaveAgain: () -> Unit,
    onDone: () -> Unit,
    sheetHeight: Dp,
    modifier: Modifier = Modifier,
) {
    val selectedCategory = state.categories.firstOrNull { it.id == state.categoryId }
    val primaryId = selectedCategory?.parentId ?: selectedCategory?.id
    val primaryCategories = state.categories.filter { it.parentId == null && it.type == state.type }
    val secondaryCategories = state.categories.filter { it.parentId == primaryId && it.type == state.type }
    val selectedPrimary = state.categories.firstOrNull { it.id == primaryId }
    val selectedSecondary = selectedCategory?.takeIf { it.parentId != null }
    var showSecondaryDialog by remember { mutableStateOf(false) }
    var showMoreMenu by remember { mutableStateOf(false) }
    var noteFocused by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current
    val categoryRows = if (sheetHeight < 700.dp) 2 else 3
    val isTransfer = state.type == TransactionType.TRANSFER

    Column(
        modifier = modifier
            .fillMaxWidth()
            .height(sheetHeight)
            .imePadding(),
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            LedgerScopePill(
                label = state.ledgers.firstOrNull { it.id == state.ledgerId }?.name ?: "选择账本",
                ledgers = state.ledgers,
                onAll = null,
                onSelected = { onLedger(it.id) },
            )

            // M3 SingleChoiceSegmentedButtonRow：支出 / 收入 / 转账
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                val options = listOf(TransactionType.EXPENSE, TransactionType.INCOME, TransactionType.TRANSFER)
                options.forEachIndexed { index, type ->
                    SegmentedButton(
                        selected = state.type == type,
                        onClick = { onType(type) },
                        shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size),
                        label = {
                            Text(
                                when (type) {
                                    TransactionType.EXPENSE -> "支出"
                                    TransactionType.INCOME -> "收入"
                                    TransactionType.TRANSFER -> "转账"
                                }
                            )
                        },
                    )
                }
            }

            if (isTransfer) {
                // 转账模式隐藏分类网格，展示简要说明并留出呼吸间距
                Spacer(Modifier.weight(1f))
            } else {
                if (primaryCategories.isEmpty()) {
                    Text("选择账本后加载分类", style = OmniText.caption, color = mutedContent())
                    Spacer(Modifier.weight(1f))
                } else {
                    PagedCategoryGrid(
                        categories = primaryCategories,
                        selectedId = primaryId,
                        onSelected = onCategory,
                        onReordered = onReorderPrimary,
                        resetSignal = state.error,
                        rowsPerPage = categoryRows,
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            EditorAttributeRow(
                state = state,
                onAccount = onAccount,
                onTargetAccount = onTargetAccount,
                onTag = onTag,
                moreExpanded = showMoreMenu,
                onMoreExpanded = { showMoreMenu = it },
                onDate = onDate,
                onExcluded = onExcluded,
            )

            TransactionEntryPanel(
                state = state,
                primary = selectedPrimary,
                selectedSecondary = selectedSecondary,
                secondaryCategories = secondaryCategories,
                onSecondary = onCategory,
                onAddSecondary = { showSecondaryDialog = true },
                onNote = onNote,
                onDate = onDate,
                onNoteFocusChanged = { noteFocused = it },
            )
            Spacer(Modifier.height(4.dp))
        }

        if (noteFocused) {
            NoteInputBar(onDone = { focusManager.clearFocus() })
        } else {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surfaceContainer,
                tonalElevation = 2.dp,
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    EditorKeypad(
                        enabled = !state.isSaving,
                        onKey = onAmountKey,
                    )
                    EditorActionButtons(
                        isSaving = state.isSaving,
                        onSaveAgain = onSaveAgain,
                        onDone = onDone,
                    )
                }
            }
        }
    }

    if (showSecondaryDialog) {
        NewSecondaryCategoryDialog(
            primaryName = state.categories.firstOrNull { it.id == primaryId }?.name.orEmpty(),
            onDismiss = { showSecondaryDialog = false },
            onCreate = { name ->
                onCreateSecondary(name)
                showSecondaryDialog = false
            },
        )
    }
}
