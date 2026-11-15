package com.omniflow.android.ui.designsystem.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.omniflow.android.ui.designsystem.tokens.OmniShapeTokens
import com.omniflow.android.ui.designsystem.tokens.OmniSpacingTokens

/**
 * 28dp 大圆角 ModalBottomSheet 统一表单脚手架。
 *
 * 规格契约：
 * - 28dp 顶部大圆角 (OmniShapeTokens.bottomSheet)
 * - 拖拽把手 (DragHandle)
 * - 统一胶囊操作按钮组 (取消 OutlinedButton / 保存 FilledButton)
 * - 键盘自适应 (imePadding + navigationBarsPadding) 与滚动区域隔离
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OmniSheetScaffold(
    title: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    onSave: (() -> Unit)? = null,
    saveText: String = "保存",
    saveEnabled: Boolean = true,
    cancelText: String = "取消",
    onCancel: () -> Unit = onDismiss,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    trailingAction: @Composable (() -> Unit)? = null,
    scrollable: Boolean = true,
    content: @Composable ColumnScope.() -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = OmniShapeTokens.bottomSheet,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        dragHandle = { BottomSheetDefaults.DragHandle() },
        windowInsets = WindowInsets(0, 0, 0, 0),
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding()
                .padding(horizontal = OmniSpacingTokens.pageHorizontal),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = OmniSpacingTokens.small),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface,
                )
                trailingAction?.invoke()
            }

            Spacer(Modifier.height(OmniSpacingTokens.mediumSmall))

            val contentModifier = if (scrollable) {
                Modifier
                    .weight(1f, fill = false)
                    .verticalScroll(rememberScrollState())
            } else {
                Modifier
            }

            Column(
                modifier = contentModifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(OmniSpacingTokens.mediumSmall),
                content = content,
            )

            Spacer(Modifier.height(OmniSpacingTokens.medium))

            if (onSave != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = OmniSpacingTokens.pageVertical),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    OutlinedButton(
                        onClick = onCancel,
                        shape = OmniShapeTokens.button,
                    ) {
                        Text(cancelText)
                    }
                    Spacer(Modifier.width(OmniSpacingTokens.small))
                    Button(
                        onClick = onSave,
                        enabled = saveEnabled,
                        shape = OmniShapeTokens.button,
                    ) {
                        Text(saveText)
                    }
                }
            } else {
                Spacer(Modifier.height(OmniSpacingTokens.pageVertical))
            }
        }
    }
}
