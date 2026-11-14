package com.omniflow.android.ui.designsystem.tokens

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance

object OmniColorTokens {
    // 语义色彩：支出/破坏性操作使用红色，收入使用绿色
    val ExpenseLight = Color(0xFFBA1A1A)
    val ExpenseDark = Color(0xFFFFB4AB)
    val OnExpenseLight = Color(0xFFFFFFFF)
    val OnExpenseDark = Color(0xFF690005)

    val IncomeLight = Color(0xFF1B6E3E)
    val IncomeDark = Color(0xFF78DC94)
    val OnIncomeLight = Color(0xFFFFFFFF)
    val OnIncomeDark = Color(0xFF00391A)

    val WarningLight = Color(0xFF7D5700)
    val WarningDark = Color(0xFFFFBA39)

    // Tonal Surface 容器阶梯规范
    val SurfaceBackgroundLight = Color(0xFFF4F4F6)
    val SurfaceBackgroundDark = Color(0xFF0C0C0E)

    val SurfaceContainerLowLight = Color(0xFFF0F0F2)
    val SurfaceContainerLowDark = Color(0xFF141416)

    val SurfaceContainerLight = Color(0xFFEBEBEF)
    val SurfaceContainerDark = Color(0xFF1C1C1F)

    val SurfaceContainerHighLight = Color(0xFFE5E5EB)
    val SurfaceContainerHighDark = Color(0xFF25252A)
}

@Composable
@ReadOnlyComposable
fun expenseColor(): Color =
    if (MaterialTheme.colorScheme.background.luminance() < 0.5f) {
        OmniColorTokens.ExpenseDark
    } else {
        OmniColorTokens.ExpenseLight
    }

@Composable
@ReadOnlyComposable
fun incomeColor(): Color =
    if (MaterialTheme.colorScheme.background.luminance() < 0.5f) {
        OmniColorTokens.IncomeDark
    } else {
        OmniColorTokens.IncomeLight
    }
