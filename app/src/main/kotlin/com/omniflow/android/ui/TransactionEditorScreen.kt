package com.omniflow.android.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import com.omniflow.android.ui.feature.editor.TransactionEditorContent
import com.omniflow.core.domain.model.TransactionType
import kotlinx.datetime.Instant

@Composable
internal fun TransactionEditorScreen(
    state: TransactionEditorUiState,
    onType: (TransactionType) -> Unit,
    onLedger: (String?) -> Unit,
    onAccount: (String?) -> Unit,
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
    onTargetAccount: ((String?) -> Unit)? = null,
) {
    TransactionEditorContent(
        state = state,
        onType = onType,
        onLedger = onLedger,
        onAccount = { onAccount(it.id) },
        onTargetAccount = { onTargetAccount?.invoke(it.id) },
        onCategory = onCategory,
        onReorderPrimary = onReorderPrimary,
        onCreateSecondary = onCreateSecondary,
        onTag = onTag,
        onNote = onNote,
        onDate = onDate,
        onExcluded = onExcluded,
        onAmountKey = onAmountKey,
        onSaveAgain = onSaveAgain,
        onDone = onDone,
        sheetHeight = sheetHeight,
        modifier = modifier,
    )
}
