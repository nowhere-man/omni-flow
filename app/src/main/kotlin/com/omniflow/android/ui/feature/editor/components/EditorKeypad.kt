package com.omniflow.android.ui.feature.editor.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.omniflow.android.ui.OmniRadius
import com.omniflow.android.ui.surfaceInset

/**
 * 记账专属 4x4 纯净数字与算符键盘。
 * 操作按钮（完成/再记）已按 M3 Expressive 规范解耦拆离至独立操作区。
 */
@Composable
fun EditorKeypad(
    enabled: Boolean,
    onKey: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptics = LocalHapticFeedback.current
    val rows = listOf(
        listOf("1", "2", "3", "+"),
        listOf("4", "5", "6", "-"),
        listOf("7", "8", "9", "退格"),
        listOf(".", "0", "C", "="),
    )

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        rows.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                row.forEach { key ->
                    val isOperator = key in listOf("+", "-", "*", "/", "C", "退格", "=")
                    val color = if (isOperator) {
                        MaterialTheme.colorScheme.secondaryContainer
                    } else {
                        surfaceInset()
                    }
                    val contentColor = if (isOperator) {
                        MaterialTheme.colorScheme.onSecondaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    }

                    Surface(
                        onClick = {
                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onKey(key)
                        },
                        enabled = enabled,
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp),
                        shape = MaterialTheme.shapes.small,
                        color = color,
                        contentColor = contentColor,
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = key,
                                style = if (key == "退格" || key == "C") {
                                    MaterialTheme.typography.titleSmall
                                } else {
                                    MaterialTheme.typography.titleLarge
                                },
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                }
            }
        }
    }
}
