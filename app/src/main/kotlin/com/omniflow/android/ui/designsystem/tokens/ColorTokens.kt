package com.omniflow.android.ui.designsystem.tokens

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance

object OmniColorTokens {
    // 语义色彩：支出使用柔和粉彩珊瑚红，收入使用柔和粉彩鼠尾草绿，警告使用柔和沙金
    // 浅色与深色均满足关键文字对比度 >= 4.5:1
    val ExpenseLight = Color(0xFFB54942)
    val ExpenseDark = Color(0xFFE58F88)
    val OnExpenseLight = Color(0xFFFFFFFF)
    val OnExpenseDark = Color(0xFF491411)

    val IncomeLight = Color(0xFF367749)
    val IncomeDark = Color(0xFF7EC895)
    val OnIncomeLight = Color(0xFFFFFFFF)
    val OnIncomeDark = Color(0xFF093717)

    val WarningLight = Color(0xFF8B6012)
    val WarningDark = Color(0xFFE8B661)

    // Tonal Surface 暖调容器阶梯规范 (对标 Gmail M3 Expressive)
    val SurfaceBackgroundLight = Color(0xFFF7F2EB)
    val SurfaceBackgroundDark = Color(0xFF171512)

    val SurfaceContainerLowLight = Color(0xFFFFFFFF)
    val SurfaceContainerLowDark = Color(0xFF22201D)

    val SurfaceContainerLight = Color(0xFFF2ECE2)
    val SurfaceContainerDark = Color(0xFF292622)

    val SurfaceContainerHighLight = Color(0xFFEBE5DB)
    val SurfaceContainerHighDark = Color(0xFF302D29)

    // Expressive 语义徽章色彩（容器色 + 图标色）对
    // Light: Pastel 容器 + 同色相柔和深色图标（不再近黑）；Dark: 暗色调容器 + 同色相粉彩浅色图标
    val BadgeSystemContainerLight = Color(0xFFEDE4FF)
    val BadgeSystemContentLight = Color(0xFF5A3E85)
    val BadgeSystemContainerDark = Color(0xFF382A52)
    val BadgeSystemContentDark = Color(0xFFD6C2FF)

    val BadgeDataSyncContainerLight = Color(0xFFD8E6FA)
    val BadgeDataSyncContentLight = Color(0xFF26528D)
    val BadgeDataSyncContainerDark = Color(0xFF1E3554)
    val BadgeDataSyncContentDark = Color(0xFFBBD7FD)

    val BadgeFinancialContainerLight = Color(0xFFD3EED9)
    val BadgeFinancialContentLight = Color(0xFF206337)
    val BadgeFinancialContainerDark = Color(0xFF1A3D25)
    val BadgeFinancialContentDark = Color(0xFFB4E8C2)

    val BadgePlanningContainerLight = Color(0xFFFFE5C4)
    val BadgePlanningContentLight = Color(0xFF7D4E15)
    val BadgePlanningContainerDark = Color(0xFF4A3215)
    val BadgePlanningContentDark = Color(0xFFFED8A7)

    val BadgeAiContainerLight = Color(0xFFF3DEFA)
    val BadgeAiContentLight = Color(0xFF6B2F86)
    val BadgeAiContainerDark = Color(0xFF3E204E)
    val BadgeAiContentDark = Color(0xFFE9BFF7)

    val BadgeCategoryContainerLight = Color(0xFFFFDFE8)
    val BadgeCategoryContentLight = Color(0xFF852E4B)
    val BadgeCategoryContainerDark = Color(0xFF48202F)
    val BadgeCategoryContentDark = Color(0xFFFDC2D3)

    val BadgeImportExportContainerLight = Color(0xFFD7F0FA)
    val BadgeImportExportContentLight = Color(0xFF165F7B)
    val BadgeImportExportContainerDark = Color(0xFF153B4A)
    val BadgeImportExportContentDark = Color(0xFFB6E6F7)

    // 统计排行榜金银铜奖牌色（柔和粉彩版）
    val RankGoldContainerLight = Color(0xFFFEF3D6)
    val RankGoldContentLight = Color(0xFF7E5B10)
    val RankGoldContainerDark = Color(0xFF473612)
    val RankGoldContentDark = Color(0xFFFCE19B)

    val RankSilverContainerLight = Color(0xFFEAEBED)
    val RankSilverContentLight = Color(0xFF4C5563)
    val RankSilverContainerDark = Color(0xFF2C323B)
    val RankSilverContentDark = Color(0xFFCFD5DE)

    val RankBronzeContainerLight = Color(0xFFF7E8DD)
    val RankBronzeContentLight = Color(0xFF7A4A28)
    val RankBronzeContainerDark = Color(0xFF422817)
    val RankBronzeContentDark = Color(0xFFE8BCA0)

    // 第三方账单品牌标识柔和色
    val ImportMeituanContainer = Color(0xFFFEE895)
    val ImportMeituanContent = Color(0xFF433502)
    val ImportDefaultContainer = Color(0xFFC75D56)
    val ImportDefaultContent = Color(0xFFFFFFFF)

    // 账本封面柔和渐变色盘
    val LedgerCoverDailyStart = Color(0xFF8BA5EE)
    val LedgerCoverDailyEnd = Color(0xFFAAC2F7)
    val LedgerCoverFamilyStart = Color(0xFFEAA190)
    val LedgerCoverFamilyEnd = Color(0xFFF8BFAD)
    val LedgerCoverTravelStart = Color(0xFF6DC3BB)
    val LedgerCoverTravelEnd = Color(0xFF9CE0D7)
    val LedgerCoverWorkStart = Color(0xFF978AE8)
    val LedgerCoverWorkEnd = Color(0xFFBDB2F7)
    val LedgerCoverStudyStart = Color(0xFFD895C5)
    val LedgerCoverStudyEnd = Color(0xFFF0BBE1)
    val LedgerCoverCarStart = Color(0xFF6F99BE)
    val LedgerCoverCarEnd = Color(0xFF9EBFDC)
    val LedgerCoverFitnessStart = Color(0xFF86BF7C)
    val LedgerCoverFitnessEnd = Color(0xFFAEDD9F)
    val LedgerCoverInvestStart = Color(0xFFD4AD62)
    val LedgerCoverInvestEnd = Color(0xFFEED08E)
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

@Composable
@ReadOnlyComposable
fun warningColor(): Color =
    if (MaterialTheme.colorScheme.background.luminance() < 0.5f) {
        OmniColorTokens.WarningDark
    } else {
        OmniColorTokens.WarningLight
    }

@Composable
@ReadOnlyComposable
fun errorColor(): Color = expenseColor()
