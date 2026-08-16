package com.omniflow.android.ui

import android.app.DatePickerDialog
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.omniflow.core.domain.model.AnalyticsDashboardState
import com.omniflow.core.domain.model.CategoryBreakdownItem
import com.omniflow.core.domain.model.DateRange
import com.omniflow.core.domain.model.LedgerScope
import com.omniflow.core.domain.model.Money
import com.omniflow.core.domain.model.StatementMonth
import com.omniflow.core.domain.model.StatementTable
import com.omniflow.core.domain.model.TransactionType
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.toLocalDateTime
import kotlin.math.max

private enum class BarLayout { DIVERGING, SIDE_BY_SIDE }
private enum class StatementFilter { ALL, INCOME, EXPENSE }

@Composable
internal fun AnalyticsScreen(
    state: AnalyticsUiState,
    onScope: (LedgerScope) -> Unit,
    onRangeMode: (AnalyticsRangeMode) -> Unit,
    onShiftRange: (Long) -> Unit,
    onCurrentRange: () -> Unit,
    onCustomRange: (DateRange) -> Unit,
    onAnalyticsType: (TransactionType) -> Unit,
    onCategorySelected: (String) -> Unit,
    onTransactionSelected: (String) -> Unit,
    onMonthSelected: (Int) -> Unit,
    onStatementTable: (Int) -> Unit,
    onDismissStatementTable: () -> Unit,
    onAddTransaction: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val now = Clock.System.now()
    val showCurrentRangeButton = state.rangeMode != AnalyticsRangeMode.CUSTOM &&
        (now < state.range.startInclusive || now >= state.range.endExclusive)
    LazyColumn(
        modifier = modifier.readableContentWidth().fillMaxHeight().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item { Spacer(Modifier.height(8.dp)); LedgerScopeMenu(state.scope, state.ledgers, onScope) }
        item {
            OmniSegmented(
                options = AnalyticsRangeMode.entries,
                selected = state.rangeMode,
                label = { it.label },
                onSelected = onRangeMode,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        if (state.rangeMode == AnalyticsRangeMode.CUSTOM) {
            item { CustomRangeControls(state.range, onCustomRange) }
        } else {
            item {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { onShiftRange(-1) }) { Icon(Icons.Default.ArrowBackIosNew, "上一周期") }
                    Text(
                        state.range.displayLabel(state.rangeMode),
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                    )
                    IconButton(onClick = { onShiftRange(1) }) { Icon(Icons.AutoMirrored.Filled.ArrowForwardIos, "下一周期") }
                }
                if (showCurrentRangeButton) {
                    FilledTonalButton(onClick = onCurrentRange, modifier = Modifier.fillMaxWidth()) {
                        Text(state.rangeMode.currentRangeLabel())
                    }
                }
            }
        }
        when {
            state.isLoading && state.dashboard == null -> item { Text("加载中…", Modifier.padding(24.dp)) }
            state.error != null && state.dashboard == null -> item { Text(state.error, color = MaterialTheme.colorScheme.error) }
            state.dashboard != null -> {
                val dashboard = state.dashboard
                val hasTransactions = dashboard.summary.expenseTotal != Money.Zero || dashboard.summary.incomeTotal != Money.Zero
                item { SummaryComparisonCard(dashboard) }
                item { TrendCard(dashboard) }
                if (state.rangeMode == AnalyticsRangeMode.MONTH) {
                    item { YearBars(dashboard, state, onMonthSelected, onStatementTable) }
                }
                if (!hasTransactions) {
                    item { EmptyAnalytics(onAddTransaction) }
                } else {
                    // 收支开关提到页面级，一次切换三张卡一起变
                    item {
                        OmniSegmented(
                            options = TransactionType.entries,
                            selected = state.analyticsType,
                            label = { if (it == TransactionType.EXPENSE) "支出" else "收入" },
                            onSelected = onAnalyticsType,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                    item { RankingCard(dashboard, state.analyticsType, onTransactionSelected) }
                    item { CategoryCard(dashboard, state.analyticsType, onCategorySelected) }
                    item { TagAnalysisCard(dashboard, state.analyticsType) }
                }
            }
        }
        item { Spacer(Modifier.height(88.dp)) }
    }
    state.statementTable?.let { StatementTableSheet(it, onStatementTable, onDismissStatementTable) }
}

/** 周/月/年/范围药丸分段控件，与记账页收支切换同一样式。 */
@Composable
private fun SummaryComparisonCard(dashboard: AnalyticsDashboardState) {
    AnalyticsCard {
        AnalyticsHeader("收支汇总") {}
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            if (maxWidth < 420.dp) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    SummaryLine("总支出", dashboard.summary.expenseTotal, dashboard.previousSummary.expenseTotal, MaterialTheme.colorScheme.onSurface)
                    SummaryLine("总收入", dashboard.summary.incomeTotal, dashboard.previousSummary.incomeTotal, incomeColor())
                    SummaryLine("总结余", dashboard.summary.netIncome, dashboard.previousSummary.netIncome, if (dashboard.summary.netIncome.minor >= 0) incomeColor() else expenseColor())
                }
            } else {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SummaryValue("总支出", dashboard.summary.expenseTotal, dashboard.previousSummary.expenseTotal, MaterialTheme.colorScheme.onSurface, Modifier.weight(1f))
                    SummaryValue("总收入", dashboard.summary.incomeTotal, dashboard.previousSummary.incomeTotal, incomeColor(), Modifier.weight(1f))
                    SummaryValue("总结余", dashboard.summary.netIncome, dashboard.previousSummary.netIncome, if (dashboard.summary.netIncome.minor >= 0) incomeColor() else expenseColor(), Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun SummaryLine(title: String, current: Money, previous: Money, color: Color) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(title, Modifier.weight(1f), style = OmniText.caption, color = mutedContent())
        Text(current.asRmb(), style = OmniText.amountPrimary, color = color, maxLines = 1)
        Spacer(Modifier.width(12.dp))
        ChangeBadge(current, previous)
    }
}

@Composable
private fun SummaryValue(title: String, current: Money, previous: Money, color: Color, modifier: Modifier = Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(title, style = OmniText.caption, color = mutedContent())
        Text(current.asRmb(), style = OmniText.amountPrimary, color = color, maxLines = 1, overflow = TextOverflow.Ellipsis)
        ChangeBadge(current, previous)
    }
}

/** 涨跌带箭头和颜色，纯灰文字看不出好坏。 */
@Composable
private fun ChangeBadge(current: Money, previous: Money) {
    if (previous == Money.Zero) {
        Text(
            if (current == Money.Zero) "较上期持平" else "上期无数据",
            style = OmniText.caption,
            color = mutedContent(),
            maxLines = 1,
        )
        return
    }
    val percent = ((current.minor - previous.minor).toDouble() / kotlin.math.abs(previous.minor) * 100).toInt()
    val (text, color) = when {
        percent > 0 -> "↑$percent%" to incomeColor()
        percent < 0 -> "↓${-percent}%" to expenseColor()
        else -> "持平" to mutedContent()
    }
    Text(text, style = OmniText.caption, color = color, maxLines = 1)
}

@Composable
private fun TrendCard(dashboard: AnalyticsDashboardState) {
    var selected by remember(dashboard.query.range, dashboard.query.trendGranularity) { mutableStateOf<Instant?>(null) }
    val maximum = dashboard.trend.points.maxOfOrNull { max(it.income.minor, it.expense.minor) }?.coerceAtLeast(1) ?: 1
    AnalyticsCard {
        AnalyticsHeader("收支趋势") {}
        if (dashboard.trend.points.isEmpty()) {
            Text("当前范围暂无趋势数据", color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                dashboard.trend.points.forEach { point ->
                    val isSelected = selected == point.start
                    Column(
                        Modifier.width(48.dp)
                            .clip(RoundedCornerShape(OmniRadius.small))
                            .background(if (isSelected) surfaceInset() else Color.Transparent)
                            .clickable { selected = if (isSelected) null else point.start }
                            .semantics {
                                contentDescription = "${point.label}，收入${point.income.asRmb()}，支出${point.expense.asRmb()}"
                            },
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Row(Modifier.height(112.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Column(Modifier.weight(1f).fillMaxHeight()) { VerticalBar(point.income.minor, maximum, incomeColor()) }
                            Column(Modifier.weight(1f).fillMaxHeight()) { VerticalBar(point.expense.minor, maximum, expenseColor()) }
                        }
                        Text(
                            point.label.takeLast(if (dashboard.query.trendGranularity.name == "MONTH") 2 else 5),
                            style = OmniText.caption,
                            color = if (isSelected) MaterialTheme.colorScheme.onSurface else mutedContent(),
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            maxLines = 1,
                        )
                    }
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                BarLegend(incomeColor(), "收入")
                Spacer(Modifier.width(16.dp))
                BarLegend(expenseColor(), "支出")
            }
            selected?.let { selectedStart ->
                dashboard.trend.points.firstOrNull { it.start == selectedStart }?.let { point ->
                    Surface(shape = RoundedCornerShape(OmniRadius.small), color = surfaceInset()) {
                        Row(
                            Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(point.label, Modifier.weight(1f), style = OmniText.caption, color = mutedContent())
                            Text("收入 ${point.income.asRmb()}", style = OmniText.caption, color = incomeColor())
                            Spacer(Modifier.width(10.dp))
                            Text("支出 ${point.expense.asRmb()}", style = OmniText.caption, color = expenseColor())
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun YearBars(
    dashboard: AnalyticsDashboardState,
    state: AnalyticsUiState,
    onMonthSelected: (Int) -> Unit,
    onStatementTable: (Int) -> Unit,
) {
    var layout by remember { mutableStateOf(BarLayout.DIVERGING) }
    AnalyticsCard {
        AnalyticsHeader("收支柱状图") {
            OmniSegmented(
                options = BarLayout.entries,
                selected = layout,
                label = { if (it == BarLayout.DIVERGING) "上下" else "并排" },
                onSelected = { layout = it },
                modifier = Modifier.width(140.dp),
            )
        }
        Text("${dashboard.yearStatement.year} 年", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        val maximum = max(
            dashboard.yearStatement.months.maxOfOrNull { it.income.minor } ?: 0,
            dashboard.yearStatement.months.maxOfOrNull { it.expense.minor } ?: 0,
        ).coerceAtLeast(1)
        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            dashboard.yearStatement.months.forEach { month ->
                MonthBars(
                    month = month,
                    maximum = maximum,
                    layout = layout,
                    selected = state.rangeMode == AnalyticsRangeMode.MONTH &&
                        state.range.startInclusive.toLocalDateTime(ChinaTimeZone).monthNumber == month.month,
                    onClick = { onMonthSelected(month.month) },
                    modifier = Modifier.width(48.dp),
                )
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
            BarLegend(incomeColor(), "收入")
            Spacer(Modifier.width(16.dp))
            BarLegend(expenseColor(), "支出")
        }
        Surface(
            onClick = { onStatementTable(dashboard.yearStatement.year) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(OmniRadius.small),
            color = surfaceInset(),
        ) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("查看账单表格", Modifier.weight(1f), style = OmniText.bodyRow)
                Icon(Icons.AutoMirrored.Filled.ArrowForwardIos, null, Modifier.size(14.dp), tint = mutedContent())
            }
        }
    }
}

@Composable
private fun MonthBars(
    month: StatementMonth,
    maximum: Long,
    layout: BarLayout,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier,
) {
    Column(
        modifier.semantics {
            contentDescription = "${month.month}月，收入${month.income.asRmb()}，支出${month.expense.asRmb()}"
        }.clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (layout == BarLayout.DIVERGING) {
            Column(Modifier.height(126.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Column(Modifier.weight(1f).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    VerticalBar(month.income.minor, maximum, incomeColor())
                }
                Box(Modifier.height(1.dp).fillMaxWidth().background(MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)))
                Column(Modifier.weight(1f).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    InvertedBar(month.expense.minor, maximum, expenseColor())
                }
            }
        } else {
            Row(Modifier.height(126.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Column(Modifier.weight(1f).fillMaxHeight()) { VerticalBar(month.income.minor, maximum, incomeColor()) }
                Column(Modifier.weight(1f).fillMaxHeight()) { VerticalBar(month.expense.minor, maximum, expenseColor()) }
            }
        }
        Text(
            "${month.month.toString().padStart(2, '0')}月",
            style = MaterialTheme.typography.labelSmall,
            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            maxLines = 1,
        )
    }
}

/** 从上往下长的柱，用在「上下」布局的支出半区。 */
@Composable
private fun ColumnScope.InvertedBar(value: Long, maximum: Long, color: Color) {
    val fraction = (value.toDouble() / maximum).toFloat().coerceIn(0f, 1f)
    Box(
        Modifier
            .weight(fraction.coerceAtLeast(0.0001f))
            .width(14.dp)
            .background(color, RoundedCornerShape(bottomStart = 4.dp, bottomEnd = 4.dp)),
    )
    Spacer(Modifier.weight((1f - fraction).coerceAtLeast(0.0001f)))
}

/** 用 weight 分配而不是写死 dp，柱子才能真正填满所在容器。 */
@Composable
private fun ColumnScope.VerticalBar(value: Long, maximum: Long, color: Color) {
    val fraction = (value.toDouble() / maximum).toFloat().coerceIn(0f, 1f)
    Spacer(Modifier.weight((1f - fraction).coerceAtLeast(0.0001f)))
    Box(
        Modifier
            .weight(fraction.coerceAtLeast(0.0001f))
            .width(14.dp)
            .background(color, RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp, bottomStart = 4.dp, bottomEnd = 4.dp)),
    )
}

@Composable
private fun BarLegend(color: Color, label: String) {
    Box(Modifier.size(10.dp).background(color, CircleShape))
    Spacer(Modifier.width(6.dp))
    Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
private fun RankingCard(
    dashboard: AnalyticsDashboardState,
    selected: TransactionType,
    onTransactionSelected: (String) -> Unit,
) {
    var expanded by remember(dashboard.query.range, selected) { mutableStateOf(false) }
    AnalyticsCard {
        AnalyticsHeader("收支排行榜") {}
        val items = dashboard.ranking.take(if (expanded) 10 else 3)
        if (items.isEmpty()) {
            Text(if (selected == TransactionType.EXPENSE) "暂无支出排行" else "暂无收入排行", color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            items.forEachIndexed { index, item ->
                Row(
                    Modifier.fillMaxWidth().clickable { onTransactionSelected(item.transactionId) }.padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("${index + 1}.", Modifier.width(28.dp), fontWeight = FontWeight.Bold)
                    Surface(Modifier.size(42.dp), RoundedCornerShape(13.dp), color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)) {
                        Box(contentAlignment = Alignment.Center) { SvgIcon(categoryIconKey(item.iconKey), Modifier.size(25.dp), tint = MaterialTheme.colorScheme.primary) }
                    }
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(item.categoryDisplayName, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(
                            listOfNotNull(
                                item.note?.takeIf(String::isNotBlank),
                                item.occurredAt.toLocalDateTime(ChinaTimeZone).let { "%02d-%02d %02d:%02d".format(it.monthNumber, it.dayOfMonth, it.hour, it.minute) },
                            ).joinToString(" · "),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    Text(item.amount.asRmb(), fontWeight = FontWeight.Bold, color = if (selected == TransactionType.EXPENSE) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.tertiary)
                }
            }
        }
        if (dashboard.ranking.size > 3) {
            TextButton(onClick = { expanded = !expanded }, modifier = Modifier.fillMaxWidth()) {
                Text(if (expanded) "收起" else "展示更多")
            }
        }
    }
}

@Composable
private fun TagAnalysisCard(dashboard: AnalyticsDashboardState, selected: TransactionType) {
    val total = if (selected == TransactionType.EXPENSE) dashboard.summary.expenseTotal else dashboard.summary.incomeTotal
    AnalyticsCard {
        AnalyticsHeader("记账标签分析") {}
        if (dashboard.tagAnalysis.isEmpty()) {
            Text("当前范围暂无标签数据", color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            dashboard.tagAnalysis.forEach { item ->
                val fraction = if (total == Money.Zero) 0f else item.amount.minor.toFloat() / total.minor
                Column(Modifier.fillMaxWidth().padding(vertical = 5.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(item.tagName, Modifier.weight(1f), fontWeight = FontWeight.Medium)
                        Text("${item.transactionCount} 笔", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.width(12.dp))
                        Text(item.amount.asRmb(), fontWeight = FontWeight.SemiBold)
                    }
                    LinearProgressIndicator(
                        progress = { fraction.coerceIn(0f, 1f) },
                        modifier = Modifier.fillMaxWidth().height(7.dp),
                        color = MaterialTheme.colorScheme.tertiary,
                        trackColor = MaterialTheme.colorScheme.tertiaryContainer,
                        strokeCap = StrokeCap.Round,
                    )
                }
            }
        }
    }
}

@Composable
private fun CategoryCard(dashboard: AnalyticsDashboardState, selected: TransactionType, onCategorySelected: (String) -> Unit) {
    var showSecondary by remember { mutableStateOf(false) }
    val total = dashboard.categoryBreakdowns.fold(Money.Zero) { value, item -> value + item.amount }
    AnalyticsCard {
        AnalyticsHeader("收支饼图") {}
        if (dashboard.categoryBreakdowns.isEmpty()) {
            Text(if (selected == TransactionType.EXPENSE) "暂无支出分类数据" else "暂无收入分类数据", color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            CategoryDonut(dashboard.categoryBreakdowns, total)
            FilterChip(showSecondary, { showSecondary = !showSecondary }, { Text("显示二级分类占比") })
            dashboard.categoryBreakdowns.forEachIndexed { index, item ->
                val palette = chartPalette()
                val color = palette[index % palette.size]
                CategoryRow(item.primaryCategoryName, item.amount, total, item.iconKey, color) {
                    onCategorySelected(item.primaryCategoryId)
                }
                if (showSecondary) {
                    item.secondaryCategories.forEach { secondary ->
                        CategoryRow("  ${secondary.categoryName}", secondary.amount, item.amount, secondary.iconKey, color.copy(alpha = 0.75f)) {
                            onCategorySelected(secondary.categoryId)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CategoryRow(label: String, amount: Money, total: Money, iconKey: String?, color: Color, onClick: () -> Unit) {
    val fraction = if (total.minor == 0L) 0f else amount.minor.toFloat() / total.minor
    Column(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            SvgIcon(categoryIconKey(iconKey), Modifier.size(24.dp), tint = color)
            Spacer(Modifier.width(8.dp))
            Text(label, Modifier.weight(1f), fontWeight = FontWeight.Medium)
            Text(amount.asRmb(), fontWeight = FontWeight.SemiBold)
            Text(" ${(fraction * 100).toInt()}%", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        LinearProgressIndicator(
            progress = { fraction.coerceIn(0f, 1f) },
            modifier = Modifier.fillMaxWidth().height(7.dp),
            color = color,
            trackColor = color.copy(alpha = 0.12f),
            strokeCap = StrokeCap.Round,
        )
    }
}

@Composable
private fun CategoryDonut(items: List<CategoryBreakdownItem>, total: Money) {
    val palette = chartPalette()
    val totalMinor = items.sumOf { it.amount.minor }.coerceAtLeast(1)
    val description = items.joinToString("，") { item ->
        "${item.primaryCategoryName} ${(item.amount.minor * 100 / totalMinor)}%"
    }
    Box(Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize().semantics { contentDescription = description }) {
            var start = -90f
            items.forEachIndexed { index, item ->
                val sweep = item.amount.minor.toFloat() / totalMinor * 360f
                // 留 1.5 度间隙，扇区之间才有呼吸感
                drawArc(
                    color = palette[index % palette.size],
                    startAngle = start + 0.75f,
                    sweepAngle = (sweep - 1.5f).coerceAtLeast(0.5f),
                    useCenter = false,
                    style = Stroke(width = 30.dp.toPx(), cap = StrokeCap.Butt),
                )
                start += sweep
            }
        }
        // 中心显示合计，否则圆环中间是一块空白
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("合计", style = OmniText.caption, color = mutedContent())
            Text(total.asRmb(), style = OmniText.amountPrimary, maxLines = 1)
            Text("${items.size} 个分类", style = OmniText.caption, color = mutedContent())
        }
    }
}

@Composable
private fun AnalyticsHeader(title: String, trailing: @Composable () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(title, Modifier.weight(1f), style = OmniText.titleRow)
        trailing()
    }
}

@Composable
private fun AnalyticsCard(content: @Composable () -> Unit) {
    Card(
        Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(OmniRadius.medium),
        colors = CardDefaults.cardColors(containerColor = surfaceCard()),
    ) {
        Column(Modifier.padding(OmniSpace.l), verticalArrangement = Arrangement.spacedBy(OmniSpace.m)) {
            content()
        }
    }
}

@Composable
private fun LedgerScopeMenu(scope: LedgerScope, ledgers: List<com.omniflow.core.domain.model.Ledger>, onScope: (LedgerScope) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val label = when (scope) {
        LedgerScope.All -> "所有账本"
        is LedgerScope.Single -> ledgers.firstOrNull { it.id == scope.ledgerId }?.name ?: "账本"
    }
    Box {
        Surface(
            onClick = { expanded = true },
            shape = CircleShape,
            color = surfaceInset(),
        ) {
            Row(
                Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Icon(Icons.AutoMirrored.Filled.MenuBook, null, Modifier.size(18.dp), tint = mutedContent())
                Text(label, style = MaterialTheme.typography.labelLarge, maxLines = 1)
            }
        }
        DropdownMenu(expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem({ Text("所有账本") }, onClick = { expanded = false; onScope(LedgerScope.All) })
            ledgers.forEach { ledger ->
                DropdownMenuItem({ Text(ledger.name) }, onClick = { expanded = false; onScope(LedgerScope.Single(ledger.id)) })
            }
        }
    }
}

@Composable
private fun CustomRangeControls(range: DateRange, onRange: (DateRange) -> Unit) {
    val context = LocalContext.current
    var start by remember(range) { mutableStateOf(range.startInclusive.toLocalDateTime(ChinaTimeZone).date) }
    var end by remember(range) { mutableStateOf(Instant.fromEpochMilliseconds(range.endExclusive.toEpochMilliseconds() - 1).toLocalDateTime(ChinaTimeZone).date) }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        FilledTonalButton({ showDatePicker(context, start) { start = it; onRange(customRange(start, end)) } }, Modifier.weight(1f)) {
            Text("开始 ${start.monthNumber}月${start.dayOfMonth}日", maxLines = 1)
        }
        FilledTonalButton({ showDatePicker(context, end) { end = it; onRange(customRange(start, end)) } }, Modifier.weight(1f)) {
            Text("结束 ${end.monthNumber}月${end.dayOfMonth}日", maxLines = 1)
        }
    }
}

private fun showDatePicker(context: android.content.Context, date: LocalDate, onDate: (LocalDate) -> Unit) {
    DatePickerDialog(context, { _, year, month, day -> onDate(LocalDate(year, month + 1, day)) }, date.year, date.monthNumber - 1, date.dayOfMonth).show()
}

private fun customRange(first: LocalDate, second: LocalDate): DateRange {
    val start = minOf(first, second)
    val end = maxOf(first, second)
    return DateRange(start.atStartOfDayIn(ChinaTimeZone), java.time.LocalDate.of(end.year, end.monthNumber, end.dayOfMonth).plusDays(1).let { LocalDate(it.year, it.monthValue, it.dayOfMonth) }.atStartOfDayIn(ChinaTimeZone))
}

@Composable
private fun EmptyAnalytics(onAdd: () -> Unit) {
    Column(Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("当前范围还没有收支", fontWeight = FontWeight.SemiBold)
        FilledTonalButton(onClick = onAdd) { Text("新增交易") }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun StatementTableSheet(table: StatementTable, onYear: (Int) -> Unit, onDismiss: () -> Unit) {
    var filter by remember { mutableStateOf(StatementFilter.ALL) }
    ModalBottomSheet(onDismissRequest = onDismiss) {
        LazyColumn(Modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { onYear(table.year - 1) }) { Icon(Icons.Default.ArrowBackIosNew, "上一年") }
                    Text("${table.year} 年账单", Modifier.weight(1f), textAlign = TextAlign.Center, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    IconButton(onClick = { onYear(table.year + 1) }) { Icon(Icons.AutoMirrored.Filled.ArrowForwardIos, "下一年") }
                }
            }
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatementFilter.entries.forEach { option ->
                        FilterChip(
                            selected = filter == option,
                            onClick = { filter = option },
                            label = {
                                Text(
                                    when (option) {
                                        StatementFilter.ALL -> "全部"
                                        StatementFilter.INCOME -> "收入"
                                        StatementFilter.EXPENSE -> "支出"
                                    },
                                    Modifier.fillMaxWidth(),
                                    textAlign = TextAlign.Center,
                                )
                            },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
            item { StatementChart(table.months, filter) }
            item { StatementHeader(filter) }
            item { StatementRow("全年", table.total.incomeTotal, table.total.expenseTotal, filter, true) }
            items(table.months) { month ->
                StatementRow("${month.month.toString().padStart(2, '0')}月", month.income, month.expense, filter)
            }
            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}

@Composable
private fun StatementChart(months: List<StatementMonth>, filter: StatementFilter) {
    val maximum = months.maxOfOrNull { month ->
        when (filter) {
            StatementFilter.ALL -> max(month.income.minor, month.expense.minor)
            StatementFilter.INCOME -> month.income.minor
            StatementFilter.EXPENSE -> month.expense.minor
        }
    }?.coerceAtLeast(1) ?: 1
    Row(
        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        months.forEach { month ->
            Column(
                Modifier.width(44.dp).semantics {
                    contentDescription = "${month.month}月，收入${month.income.asRmb()}，支出${month.expense.asRmb()}，结余${month.netIncome.asRmb()}"
                },
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Row(Modifier.height(96.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (filter != StatementFilter.EXPENSE) {
                        Column(Modifier.weight(1f).fillMaxHeight()) { VerticalBar(month.income.minor, maximum, incomeColor()) }
                    }
                    if (filter != StatementFilter.INCOME) {
                        Column(Modifier.weight(1f).fillMaxHeight()) { VerticalBar(month.expense.minor, maximum, expenseColor()) }
                    }
                }
                Text("${month.month}月", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun StatementHeader(filter: StatementFilter) {
    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("月份", Modifier.width(46.dp), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
        if (filter != StatementFilter.EXPENSE) Text("收入", Modifier.weight(1f), textAlign = TextAlign.End, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
        if (filter != StatementFilter.INCOME) Text("支出", Modifier.weight(1f), textAlign = TextAlign.End, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
        Text("结余", Modifier.weight(1f), textAlign = TextAlign.End, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun StatementRow(label: String, income: Money, expense: Money, filter: StatementFilter, emphasized: Boolean = false) {
    val net = income - expense
    Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(label, Modifier.width(46.dp), fontWeight = if (emphasized) FontWeight.Bold else FontWeight.SemiBold)
        if (filter != StatementFilter.EXPENSE) {
            Text(income.asRmb(), Modifier.weight(1f), textAlign = TextAlign.End, color = MaterialTheme.colorScheme.tertiary, fontWeight = if (emphasized) FontWeight.Bold else FontWeight.Normal)
        }
        if (filter != StatementFilter.INCOME) {
            Text(expense.asRmb(), Modifier.weight(1f), textAlign = TextAlign.End, color = MaterialTheme.colorScheme.error, fontWeight = if (emphasized) FontWeight.Bold else FontWeight.Normal)
        }
        Text(
            net.asRmb(),
            Modifier.weight(1f),
            textAlign = TextAlign.End,
            color = if (net.minor < 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.tertiary,
            fontWeight = if (emphasized) FontWeight.Bold else FontWeight.Normal,
        )
    }
}

private val AnalyticsRangeMode.label: String get() = when (this) {
    AnalyticsRangeMode.WEEK -> "周"
    AnalyticsRangeMode.MONTH -> "月"
    AnalyticsRangeMode.YEAR -> "年"
    AnalyticsRangeMode.CUSTOM -> "范围"
}

private fun AnalyticsRangeMode.currentRangeLabel(): String = when (this) {
    AnalyticsRangeMode.WEEK -> "回到本周"
    AnalyticsRangeMode.MONTH -> "回到本月"
    AnalyticsRangeMode.YEAR -> "回到今年"
    AnalyticsRangeMode.CUSTOM -> ""
}
