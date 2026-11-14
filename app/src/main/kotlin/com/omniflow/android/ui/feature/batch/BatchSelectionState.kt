package com.omniflow.android.ui.feature.batch

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

@Stable
class BatchSelectionState(
    initialSelected: Set<String> = emptySet(),
) {
    var selectedIds by mutableStateOf(initialSelected)
        private set

    var isActive by mutableStateOf(initialSelected.isNotEmpty())
        private set

    val count: Int get() = selectedIds.size

    fun isSelected(id: String): Boolean = id in selectedIds

    fun startSelection(id: String) {
        isActive = true
        selectedIds = setOf(id)
    }

    fun toggle(id: String) {
        val updated = if (id in selectedIds) selectedIds - id else selectedIds + id
        selectedIds = updated
    }

    fun selectAll(allIds: Collection<String>) {
        selectedIds = allIds.toSet()
    }

    fun deselectAll() {
        selectedIds = emptySet()
    }

    fun clear() {
        selectedIds = emptySet()
        isActive = false
    }
}

@Composable
fun rememberBatchSelectionState(): BatchSelectionState {
    return remember { BatchSelectionState() }
}
