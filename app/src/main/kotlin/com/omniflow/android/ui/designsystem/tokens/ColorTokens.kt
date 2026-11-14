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

    // Tonal Surface 暖调容器阶梯规范 (对标 Gmail M3 Expressive & Google Account M3E Dark)
    val SurfaceBackgroundLight = Color(0xFFF5EDE0)
    val SurfaceBackgroundDark = Color(0xFF1E1F21)

    val SurfaceContainerLowLight = Color(0xFFFFFDF8)
    val SurfaceContainerLowDark = Color(0xFF38393B)

    val SurfaceContainerLight = Color(0xFFEFE6D8)
    val SurfaceContainerDark = Color(0xFF2B2C2E)

    val SurfaceContainerHighLight = Color(0xFFE8DECF)
    val SurfaceContainerHighDark = Color(0xFF444547)
    val SurfaceContainerHighestDark = Color(0xFF4E5052)

    // Expressive 语义徽章色彩（容器色 + 图标色）对
    // 浅/深两模式统一为「亮粉彩圆盘 + 同色相深色 glyph」，深色模式不再使用深底浅图标
    val BadgeSystemContainerLight = Color(0xFFEDE4FF)
    val BadgeSystemContentLight = Color(0xFF5529A4)
    val BadgeSystemContainerDark = Color(0xFFD8BAFA)
    val BadgeSystemContentDark = Color(0xFF5529A4)

    val BadgeDataSyncContainerLight = Color(0xFFD8E6FA)
    val BadgeDataSyncContentLight = Color(0xFF014A75)
    val BadgeDataSyncContainerDark = Color(0xFF7ECEFD)
    val BadgeDataSyncContentDark = Color(0xFF014A75)

    val BadgeFinancialContainerLight = Color(0xFFD3EED9)
    val BadgeFinancialContentLight = Color(0xFF09421A)
    val BadgeFinancialContainerDark = Color(0xFF64C881)
    val BadgeFinancialContentDark = Color(0xFF09421A)

    val BadgePlanningContainerLight = Color(0xFFFEDEC5)
    val BadgePlanningContentLight = Color(0xFF763404)
    val BadgePlanningContainerDark = Color(0xFFFDB481)
    val BadgePlanningContentDark = Color(0xFF763404)

    val BadgeAiContainerLight = Color(0xFFFDE4EC)
    val BadgeAiContentLight = Color(0xFF8E0054)
    val BadgeAiContainerDark = Color(0xFFFDADE2)
    val BadgeAiContentDark = Color(0xFF8E0054)

    val BadgeCategoryContainerLight = Color(0xFFFFDFE8)
    val BadgeCategoryContentLight = Color(0xFF8E0054)
    val BadgeCategoryContainerDark = Color(0xFFFDADE2)
    val BadgeCategoryContentDark = Color(0xFF8E0054)

    val BadgeImportExportContainerLight = Color(0xFFAEF0FE)
    val BadgeImportExportContentLight = Color(0xFF004D5A)
    val BadgeImportExportContainerDark = Color(0xFF6EDEF4)
    val BadgeImportExportContentDark = Color(0xFF004D5A)

    // 统计排行榜金银铜奖牌色（柔和粉彩版，深色模式统一亮粉彩+深色前景）
    val RankGoldContainerLight = Color(0xFFFEF3D6)
    val RankGoldContentLight = Color(0xFF7E5B10)
    val RankGoldContainerDark = Color(0xFFFDD663)
    val RankGoldContentDark = Color(0xFF6B4605)

    val RankSilverContainerLight = Color(0xFFEAEBED)
    val RankSilverContentLight = Color(0xFF4C5563)
    val RankSilverContainerDark = Color(0xFFD3D7DD)
    val RankSilverContentDark = Color(0xFF2C323B)

    val RankBronzeContainerLight = Color(0xFFF7E8DD)
    val RankBronzeContentLight = Color(0xFF7A4A28)
    val RankBronzeContainerDark = Color(0xFFFDB481)
    val RankBronzeContentDark = Color(0xFF763404)

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
