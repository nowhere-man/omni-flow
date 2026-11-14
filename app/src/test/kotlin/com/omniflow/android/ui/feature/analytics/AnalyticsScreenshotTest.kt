package com.omniflow.android.ui.feature.analytics

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import com.omniflow.core.domain.model.CategoryBreakdownItem
import com.omniflow.core.domain.model.CategoryDiffItem
import com.omniflow.core.domain.model.CategoryShareItem
import com.omniflow.core.domain.model.ChartData
import com.omniflow.core.domain.model.ChartPoint
import com.omniflow.core.domain.model.DateRange
import com.omniflow.core.domain.model.FinancialHealthSummary
import com.omniflow.core.domain.model.FinancialInsight
import com.omniflow.core.domain.model.InsightType
import com.omniflow.core.domain.model.Money
import com.omniflow.core.domain.model.TimeGranularity
import com.omniflow.core.domain.model.TransactionRankingItem
import com.omniflow.core.domain.model.TransactionSummary
import com.omniflow.core.domain.model.TransactionType
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import org.junit.Rule
import org.junit.Test

class AnalyticsScreenshotTest {
    @get:Rule
    val paparazzi = Paparazzi(
        deviceConfig = DeviceConfig.PIXEL_5,
        theme = "android:Theme.Material.NoActionBar",
    )

    private val sampleHealthSummary = FinancialHealthSummary(
        income = Money(2200000L),
        expense = Money(820000L),
        daysCount = 30,
    )

    private val sampleInsight = FinancialInsight(
        type = InsightType.SAVINGS_ACHIEVEMENT,
        title = "本月结余率达 62.7%，储蓄表现优异",
        message = "净结余 +¥13,800.00，支出控制在预算目标内，请继续保持健康储蓄习惯。",
    )

    private val sampleTrendData = ChartData(
        range = DateRange.ofClosedDates(LocalDate(2026, 5, 1), LocalDate(2026, 10, 31)),
        granularity = TimeGranularity.MONTH,
        points = listOf(
            ChartPoint(Instant.fromEpochMilliseconds(1777593600000L), "5月", expense = Money(920000L), income = Money(1800000L)),
            ChartPoint(Instant.fromEpochMilliseconds(1780272000000L), "6月", expense = Money(1250000L), income = Money(2100000L)),
            ChartPoint(Instant.fromEpochMilliseconds(1782864000000L), "7月", expense = Money(890000L), income = Money(1950000L)),
            ChartPoint(Instant.fromEpochMilliseconds(1785542400000L), "8月", expense = Money(1400000L), income = Money(2200000L)),
            ChartPoint(Instant.fromEpochMilliseconds(1788220800000L), "9月", expense = Money(1100000L), income = Money(2050000L)),
            ChartPoint(Instant.fromEpochMilliseconds(1790812800000L), "10月", expense = Money(820000L), income = Money(2200000L)),
        ),
    )

    private val sampleCurrentSummary = TransactionSummary(
        incomeTotal = Money(2200000L),
        expenseTotal = Money(820000L),
    )

    private val samplePreviousSummary = TransactionSummary(
        incomeTotal = Money(2000000L),
        expenseTotal = Money(1100000L),
    )

    private val sampleDiffs = listOf(
        CategoryDiffItem("c1", "餐饮美食", null, currentAmount = Money(320000L), previousAmount = Money(250000L)),
        CategoryDiffItem("c2", "数码科技", null, currentAmount = Money(150000L), previousAmount = Money(450000L)),
        CategoryDiffItem("c3", "日用百货", null, currentAmount = Money(80000L), previousAmount = Money(60000L)),
        CategoryDiffItem("c4", "交通出行", null, currentAmount = Money(40000L), previousAmount = Money(70000L)),
    )

    private val sampleBreakdowns = listOf(
        CategoryBreakdownItem(
            primaryCategoryId = "c1",
            primaryCategoryName = "餐饮美食",
            iconKey = null,
            amount = Money(320000L),
            secondaryCategories = listOf(
                CategoryShareItem("c1_1", "外卖正餐", null, Money(200000L), shareRatio = 0.25),
                CategoryShareItem("c1_2", "咖啡下午茶", null, Money(120000L), shareRatio = 0.15),
            ),
            shareRatio = 0.40,
        ),
        CategoryBreakdownItem("c2", "数码科技", null, Money(240000L), emptyList(), shareRatio = 0.30),
        CategoryBreakdownItem("c3", "居住缴费", null, Money(160000L), emptyList(), shareRatio = 0.20),
        CategoryBreakdownItem("c4", "休闲娱乐", null, Money(80000L), emptyList(), shareRatio = 0.10),
    )

    private val sampleRankings = listOf(
        TransactionRankingItem(
            transactionId = "t1",
            ledgerId = "l1",
            ledgerName = "日常账本",
            accountId = "a1",
            accountName = "微信支付",
            categoryId = "c2",
            categoryName = "数码科技",
            occurredAt = Instant.fromEpochMilliseconds(1791800000000L),
            primaryCategoryName = "数码",
            secondaryCategoryName = "笔记本",
            iconKey = null,
            note = "MacBook Pro M3 Max",
            type = TransactionType.EXPENSE,
            amount = Money(1299900L),
            source = null,
        ),
        TransactionRankingItem(
            transactionId = "t2",
            ledgerId = "l1",
            ledgerName = "日常账本",
            accountId = "a1",
            accountName = "招商银行",
            categoryId = "c3",
            categoryName = "居住缴费",
            occurredAt = Instant.fromEpochMilliseconds(1790900000000L),
            primaryCategoryName = "居住",
            secondaryCategoryName = "房租",
            iconKey = null,
            note = "10月份房屋租金",
            type = TransactionType.EXPENSE,
            amount = Money(350000L),
            source = null,
        ),
        TransactionRankingItem(
            transactionId = "t3",
            ledgerId = "l1",
            ledgerName = "日常账本",
            accountId = "a1",
            accountName = "支付宝",
            categoryId = "c1",
            categoryName = "餐饮美食",
            occurredAt = Instant.fromEpochMilliseconds(1792300000000L),
            primaryCategoryName = "餐饮",
            secondaryCategoryName = "聚餐",
            iconKey = null,
            note = "团队周五团建聚餐",
            type = TransactionType.EXPENSE,
            amount = Money(128000L),
            source = null,
        ),
    )

    @Composable
    private fun ScreenshotContainer(
        darkTheme: Boolean = false,
        content: @Composable () -> Unit,
    ) {
        val colorScheme = if (darkTheme) darkColorScheme() else lightColorScheme()
        MaterialTheme(colorScheme = colorScheme) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(16.dp)
            ) {
                content()
            }
        }
    }

    @Test
    fun healthSummaryCard_light() {
        paparazzi.snapshot {
            ScreenshotContainer(darkTheme = false) {
                HealthSummaryCard(healthSummary = sampleHealthSummary)
            }
        }
    }

    @Test
    fun healthSummaryCard_dark() {
        paparazzi.snapshot {
            ScreenshotContainer(darkTheme = true) {
                HealthSummaryCard(healthSummary = sampleHealthSummary)
            }
        }
    }

    @Test
    fun financialInsightCard_light() {
        paparazzi.snapshot {
            ScreenshotContainer(darkTheme = false) {
                FinancialInsightCard(insight = sampleInsight)
            }
        }
    }

    @Test
    fun financialInsightCard_dark() {
        paparazzi.snapshot {
            ScreenshotContainer(darkTheme = true) {
                FinancialInsightCard(insight = sampleInsight)
            }
        }
    }

    @Test
    fun trendBarChartCard_light() {
        paparazzi.snapshot {
            ScreenshotContainer(darkTheme = false) {
                TrendBarChartCard(
                    trendData = sampleTrendData,
                    rangeMode = AnalyticsRangeMode.MONTH,
                )
            }
        }
    }

    @Test
    fun trendBarChartCard_dark() {
        paparazzi.snapshot {
            ScreenshotContainer(darkTheme = true) {
                TrendBarChartCard(
                    trendData = sampleTrendData,
                    rangeMode = AnalyticsRangeMode.MONTH,
                )
            }
        }
    }

    @Test
    fun categoryDiffCard_light() {
        paparazzi.snapshot {
            ScreenshotContainer(darkTheme = false) {
                CategoryDiffCard(
                    categoryDiffs = sampleDiffs,
                    currentSummary = sampleCurrentSummary,
                    previousSummary = samplePreviousSummary,
                )
            }
        }
    }

    @Test
    fun categoryDiffCard_dark() {
        paparazzi.snapshot {
            ScreenshotContainer(darkTheme = true) {
                CategoryDiffCard(
                    categoryDiffs = sampleDiffs,
                    currentSummary = sampleCurrentSummary,
                    previousSummary = samplePreviousSummary,
                )
            }
        }
    }

    @Test
    fun categoryShareCard_light() {
        paparazzi.snapshot {
            ScreenshotContainer(darkTheme = false) {
                CategoryShareCard(
                    categoryBreakdowns = sampleBreakdowns,
                    selectedType = TransactionType.EXPENSE,
                )
            }
        }
    }

    @Test
    fun categoryShareCard_dark() {
        paparazzi.snapshot {
            ScreenshotContainer(darkTheme = true) {
                CategoryShareCard(
                    categoryBreakdowns = sampleBreakdowns,
                    selectedType = TransactionType.EXPENSE,
                )
            }
        }
    }

    @Test
    fun topExpensesCard_light() {
        paparazzi.snapshot {
            ScreenshotContainer(darkTheme = false) {
                TopExpensesCard(
                    rankingItems = sampleRankings,
                    selectedType = TransactionType.EXPENSE,
                )
            }
        }
    }

    @Test
    fun topExpensesCard_dark() {
        paparazzi.snapshot {
            ScreenshotContainer(darkTheme = true) {
                TopExpensesCard(
                    rankingItems = sampleRankings,
                    selectedType = TransactionType.EXPENSE,
                )
            }
        }
    }
}
