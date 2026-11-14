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

    // Expressive 语义徽章色彩（容器色 + 图标色）对
    // Light: Pastel 容器 + 深色图标；Dark: 暗色调容器 + 浅色图标
    val BadgeSystemContainerLight = Color(0xFFEADDFF)
    val BadgeSystemContentLight = Color(0xFF21005D)
    val BadgeSystemContainerDark = Color(0xFF4F378B)
    val BadgeSystemContentDark = Color(0xFFEADDFF)

    val BadgeDataSyncContainerLight = Color(0xFFD3E3FD)
    val BadgeDataSyncContentLight = Color(0xFF041E49)
    val BadgeDataSyncContainerDark = Color(0xFF0842A0)
    val BadgeDataSyncContentDark = Color(0xFFD3E3FD)

    val BadgeFinancialContainerLight = Color(0xFFC4EED0)
    val BadgeFinancialContentLight = Color(0xFF072711)
    val BadgeFinancialContainerDark = Color(0xFF0F5223)
    val BadgeFinancialContentDark = Color(0xFFC4EED0)

    val BadgePlanningContainerLight = Color(0xFFFFDDB3)
    val BadgePlanningContentLight = Color(0xFF2B1700)
    val BadgePlanningContainerDark = Color(0xFF6B3B00)
    val BadgePlanningContentDark = Color(0xFFFFDDB3)

    val BadgeAiContainerLight = Color(0xFFF3DAFF)
    val BadgeAiContentLight = Color(0xFF32004A)
    val BadgeAiContainerDark = Color(0xFF641B8D)
    val BadgeAiContentDark = Color(0xFFF3DAFF)

    val BadgeCategoryContainerLight = Color(0xFFFFD8E4)
    val BadgeCategoryContentLight = Color(0xFF31111D)
    val BadgeCategoryContainerDark = Color(0xFF633B48)
    val BadgeCategoryContentDark = Color(0xFFFFD8E4)

    val BadgeImportExportContainerLight = Color(0xFFDDF4FF)
    val BadgeImportExportContentLight = Color(0xFF001F2A)
    val BadgeImportExportContainerDark = Color(0xFF004D63)
    val BadgeImportExportContentDark = Color(0xFFDDF4FF)
}

enum class SemanticBadgeType {
    SYSTEM,        // 系统设置、应用锁、外观
    DATA_SYNC,     // 数据备份、WebDAV、云同步
    FINANCIAL,     // 账本、账户、资产
    PLANNING,      // 预算、规划、提醒、规则
    AI,            // AI 助手、智能分类
    CATEGORY,      // 分类、标签
    IMPORT_EXPORT, // 导入、导出
}

data class SemanticBadgeColors(
    val container: Color,
    val content: Color,
)

@Composable
@ReadOnlyComposable
fun semanticBadgeColors(type: SemanticBadgeType): SemanticBadgeColors {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    return when (type) {
        SemanticBadgeType.SYSTEM -> SemanticBadgeColors(
            container = if (isDark) OmniColorTokens.BadgeSystemContainerDark else OmniColorTokens.BadgeSystemContainerLight,
            content = if (isDark) OmniColorTokens.BadgeSystemContentDark else OmniColorTokens.BadgeSystemContentLight,
        )
        SemanticBadgeType.DATA_SYNC -> SemanticBadgeColors(
            container = if (isDark) OmniColorTokens.BadgeDataSyncContainerDark else OmniColorTokens.BadgeDataSyncContainerLight,
            content = if (isDark) OmniColorTokens.BadgeDataSyncContentDark else OmniColorTokens.BadgeDataSyncContentLight,
        )
        SemanticBadgeType.FINANCIAL -> SemanticBadgeColors(
            container = if (isDark) OmniColorTokens.BadgeFinancialContainerDark else OmniColorTokens.BadgeFinancialContainerLight,
            content = if (isDark) OmniColorTokens.BadgeFinancialContentDark else OmniColorTokens.BadgeFinancialContentLight,
        )
        SemanticBadgeType.PLANNING -> SemanticBadgeColors(
            container = if (isDark) OmniColorTokens.BadgePlanningContainerDark else OmniColorTokens.BadgePlanningContainerLight,
            content = if (isDark) OmniColorTokens.BadgePlanningContentDark else OmniColorTokens.BadgePlanningContentLight,
        )
        SemanticBadgeType.AI -> SemanticBadgeColors(
            container = if (isDark) OmniColorTokens.BadgeAiContainerDark else OmniColorTokens.BadgeAiContainerLight,
            content = if (isDark) OmniColorTokens.BadgeAiContentDark else OmniColorTokens.BadgeAiContentLight,
        )
        SemanticBadgeType.CATEGORY -> SemanticBadgeColors(
            container = if (isDark) OmniColorTokens.BadgeCategoryContainerDark else OmniColorTokens.BadgeCategoryContainerLight,
            content = if (isDark) OmniColorTokens.BadgeCategoryContentDark else OmniColorTokens.BadgeCategoryContentLight,
        )
        SemanticBadgeType.IMPORT_EXPORT -> SemanticBadgeColors(
            container = if (isDark) OmniColorTokens.BadgeImportExportContainerDark else OmniColorTokens.BadgeImportExportContainerLight,
            content = if (isDark) OmniColorTokens.BadgeImportExportContentDark else OmniColorTokens.BadgeImportExportContentLight,
        )
    }
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
