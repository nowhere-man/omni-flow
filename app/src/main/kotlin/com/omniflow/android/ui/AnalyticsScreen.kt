package com.omniflow.android.ui

import android.app.DatePickerDialog
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.graphics.Shape
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
    onSummary: (TransactionType?) -> Unit,
    onStatementTable: (Int) -> Unit,
    onDismissStatementTable: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val now = Clock.System.now()
    val showCurrentRangeButton = state.rangeMode != AnalyticsRangeMode.CUSTOM &&
        (now < state.range.startInclusive || now >= state.range.endExclusive)
    LazyColumn(
        modifier = modifier.readableContentWidth().fillMaxHeight().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item { Spacer(Modifier.height(8.dp)); LedgerScopePill(state.scope, state.ledgers, onScope) }
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
                RangeStepper(
                    label = state.range.displayLabel(state.rangeMode),
                    onPrevious = { onShiftRange(-1) },
                    onNext = { onShiftRange(1) },
                    onLabel = onCurrentRange.takeIf { showCurrentRangeButton },
                )
            }
        }
        when {
            state.isLoading && state.dashboard == null -> item {
                Text("加载中…", Modifier.padding(24.dp), style = OmniText.caption, color = mutedContent())
            }
            state.error != null && state.dashboard == null -> item {
                Text(state.error, color = MaterialTheme.colorScheme.error, style = OmniText.caption)
            }
            state.dashboard != null -> {
                val dashboard = state.dashboard
                val hasTransactions = dashboard.summary.expenseTotal != Money.Zero ||
                    dashboard.summary.incomeTotal != Money.Zero
                // 首页那三张卡就是这份信息最紧凑的排版，不再另画一张「收支汇总」
                item {
                    SummaryCardRow(
                        expense = dashboard.summary.expenseTotal,
                        income = dashboard.summary.incomeTotal,
                        onExpense = { onSummary(TransactionType.EXPENSE) },
                        onIncome = { onSummary(TransactionType.INCOME) },
                        onNet = { onSummary(null) },
                    )
                }
                // 收支趋势只在「年」下有意义：按月一根柱，一屏刚好放满十二个月
                if (state.rangeMode == AnalyticsRangeMode.YEAR) {
                    item { TrendCard(dashboard, now) }
                }
                if (state.rangeMode == AnalyticsRangeMode.MONTH) {
                    item { YearBars(dashboard, state, onMonthSelected, onStatementTable) }
                }
                if (hasTransactions) {
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
                    item { CategoryCard(dashboard, onCategorySelected) }
                    item { RankingCard(dashboard, state.analyticsType, onTransactionSelected) }
                    item { TagAnalysisCard(dashboard, state.analyticsType) }
                }
            }
        }
        item { Spacer(Modifier.height(88.dp)) }
    }
    state.statementTable?.let { StatementTableSheet(it, onStatementTable, onDismissStatementTable) }
}

/**
 * 年度收支趋势。标签写「1月」而不是「01」；本年度只画到当前月，
 * 后面那几根空柱子会让人以为收支突然归零。
 */
@Composable
private fun TrendCard(dashboard: AnalyticsDashboardState, now: Instant) {
    var selected by remember(dashboard.query.range) { mutableStateOf<Instant?>(null) }
    val points = dashboard.trend.points.filter { it.start <= now }
    val maximum = points.maxOfOrNull { max(it.income.minor, it.expense.minor) }?.coerceAtLeast(1) ?: 1
    AnalyticsCard {
        if (points.isEmpty()) {
            Text("当前范围暂无趋势数据", style = OmniText.caption, color = mutedContent())
            return@AnalyticsCard
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            points.forEach { point ->
                val isSelected = selected == point.start
                Column(
                    Modifier.weight(1f)
                        .clip(RoundedCornerShape(OmniRadius.small))
                        .background(if (isSelected) surfaceInset() else Color.Transparent)
                        .clickable { selected = if (isSelected) null else point.start }
                        .semantics {
                            contentDescription = "${point.label}，收入${point.income.asRmb()}，支出${point.expense.asRmb()}"
                        },
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Row(Modifier.height(112.dp)) {
                        Column(Modifier.weight(1f).fillMaxHeight(), horizontalAlignment = Alignment.End) {
                            VerticalBar(point.income.minor, maximum, incomeColor(), IncomeBarShape)
                        }
                        Column(Modifier.weight(1f).fillMaxHeight(), horizontalAlignment = Alignment.Start) {
                            VerticalBar(point.expense.minor, maximum, expenseColor(), ExpenseBarShape)
                        }
                    }
                    Text(
                        point.label.monthLabel(),
                        style = OmniText.caption,
                        color = if (isSelected) MaterialTheme.colorScheme.onSurface else mutedContent(),
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        maxLines = 1,
                    )
                }
            }
        }
        selected?.let { selectedStart ->
            points.firstOrNull { it.start == selectedStart }?.let { point ->
                Surface(shape = RoundedCornerShape(OmniRadius.small), color = surfaceInset()) {
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(point.label.monthLabel(), Modifier.weight(1f), style = OmniText.caption, color = mutedContent())
                        Text("收 ${point.income.asRmb()}", style = OmniText.caption, color = incomeColor())
                        Spacer(Modifier.width(10.dp))
                        Text("支 ${point.expense.asRmb()}", style = OmniText.caption, color = mutedContent())
                    }
                }
            }
        }
    }
}

/** 趋势点的标签是 `2026-08` 这种，图表上只需要「8月」。 */
private fun String.monthLabel(): String =
    substringAfterLast('-').toIntOrNull()?.let { "${it}月" } ?: this

@Composable
private fun YearBars(
    dashboard: AnalyticsDashboardState,
    state: AnalyticsUiState,
    onMonthSelected: (Int) -> Unit,
    onStatementTable: (Int) -> Unit,
) {
    var layout by remember { mutableStateOf(BarLayout.DIVERGING) }
    AnalyticsCard {
        // 卡片标题和「2026 年」都去掉：年份已经写在上面的周期切换里，重复一遍只是噪音
        OmniSegmented(
            options = BarLayout.entries,
            selected = layout,
            label = { if (it == BarLayout.DIVERGING) "上下" else "并排" },
            onSelected = { layout = it },
            modifier = Modifier.width(150.dp),
        )
        val maximum = max(
            dashboard.yearStatement.months.maxOfOrNull { it.income.minor } ?: 0,
            dashboard.yearStatement.months.maxOfOrNull { it.expense.minor } ?: 0,
        ).coerceAtLeast(1)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            dashboard.yearStatement.months.forEach { month ->
                MonthBars(
                    month = month,
                    maximum = maximum,
                    layout = layout,
                    selected = state.rangeMode == AnalyticsRangeMode.MONTH &&
                        state.range.startInclusive.toLocalDateTime(ChinaTimeZone).monthNumber == month.month,
                    onClick = { onMonthSelected(month.month) },
                    modifier = Modifier.weight(1f),
                )
            }
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
                Box(Modifier.height(1.dp).fillMaxWidth().background(MaterialTheme.colorScheme.outlineVariant))
                Column(Modifier.weight(1f).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    InvertedBar(month.expense.minor, maximum, expenseColor())
                }
            }
        } else {
            Row(Modifier.height(126.dp)) {
                Column(Modifier.weight(1f).fillMaxHeight(), horizontalAlignment = Alignment.End) {
                    VerticalBar(month.income.minor, maximum, incomeColor(), IncomeBarShape)
                }
                Column(Modifier.weight(1f).fillMaxHeight(), horizontalAlignment = Alignment.Start) {
                    VerticalBar(month.expense.minor, maximum, expenseColor(), ExpenseBarShape)
                }
            }
        }
        Text(
            "${month.month}月",
            style = OmniText.caption,
            color = if (selected) MaterialTheme.colorScheme.primary else mutedContent(),
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
            .width(10.dp)
            .background(color, RoundedCornerShape(bottomStart = 4.dp, bottomEnd = 4.dp)),
    )
    Spacer(Modifier.weight((1f - fraction).coerceAtLeast(0.0001f)))
}

/** 用 weight 分配而不是写死 dp，柱子才能真正填满所在容器。 */
@Composable
private fun ColumnScope.VerticalBar(
    value: Long,
    maximum: Long,
    color: Color,
    shape: Shape = RoundedCornerShape(4.dp),
) {
    val fraction = (value.toDouble() / maximum).toFloat().coerceIn(0f, 1f)
    Spacer(Modifier.weight((1f - fraction).coerceAtLeast(0.0001f)))
    Box(
        Modifier
            .weight(fraction.coerceAtLeast(0.0001f))
            .width(10.dp)
            .background(color, shape),
    )
}

/** 并排的一对柱子：收入在左、支出在右，相邻的一侧收成直角贴在一起。 */
private val IncomeBarShape = RoundedCornerShape(topStart = 4.dp, bottomStart = 4.dp)
private val ExpenseBarShape = RoundedCornerShape(topEnd = 4.dp, bottomEnd = 4.dp)

@Composable
private fun RankingCard(
    dashboard: AnalyticsDashboardState,
    selected: TransactionType,
    onTransactionSelected: (String) -> Unit,
) {
    var expanded by remember(dashboard.query.range, selected) { mutableStateOf(false) }
    AnalyticsCard {
        Text("收支排行", style = OmniText.caption, color = mutedContent())
        val items = dashboard.ranking.take(if (expanded) 10 else 3)
        if (items.isEmpty()) {
            Text(
                if (selected == TransactionType.EXPENSE) "暂无支出排行" else "暂无收入排行",
                style = OmniText.caption,
                color = mutedContent(),
            )
        } else {
            items.forEachIndexed { index, item ->
                Row(
                    Modifier.fillMaxWidth().clickable { onTransactionSelected(item.transactionId) },
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("${index + 1}", Modifier.width(20.dp), style = OmniText.caption, color = mutedContent())
                    CategoryIcon(item.iconKey, size = 34)
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            item.categoryDisplayName,
                            style = OmniText.bodyRow.copy(fontWeight = FontWeight.Medium),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            listOfNotNull(
                                item.note?.takeIf(String::isNotBlank),
                                item.occurredAt.toLocalDateTime(ChinaTimeZone).let { "${it.monthNumber}月${it.dayOfMonth}日" },
                            ).joinToString(" · "),
                            style = OmniText.caption,
                            color = mutedContent(),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    AmountText(item.amount, selected, style = OmniText.amountTile(item.amount.asPlainAmount().length + 1))
                }
            }
        }
        if (dashboard.ranking.size > 3) {
            TextButton(onClick = { expanded = !expanded }, modifier = Modifier.fillMaxWidth()) {
                Text(if (expanded) "收起" else "展示更多", style = OmniText.caption)
            }
        }
    }
}

@Composable
private fun TagAnalysisCard(dashboard: AnalyticsDashboardState, selected: TransactionType) {
    val total = if (selected == TransactionType.EXPENSE) dashboard.summary.expenseTotal else dashboard.summary.incomeTotal
    if (dashboard.tagAnalysis.isEmpty()) return
    AnalyticsCard {
        Text("标签统计", style = OmniText.caption, color = mutedContent())
        dashboard.tagAnalysis.forEach { item ->
            val fraction = if (total == Money.Zero) 0f else item.amount.minor.toFloat() / total.minor
            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(item.tagName, Modifier.weight(1f), style = OmniText.bodyRow)
                    Text("${item.transactionCount} 笔", style = OmniText.caption, color = mutedContent())
                    Spacer(Modifier.width(12.dp))
                    Text(item.amount.asPlainAmount(), style = OmniText.bodyRow, fontWeight = FontWeight.SemiBold)
                }
                LinearProgressIndicator(
                    progress = { fraction.coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth().height(6.dp),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = surfaceInset(),
                    strokeCap = StrokeCap.Round,
                )
            }
        }
    }
}

@Composable
private fun CategoryCard(dashboard: AnalyticsDashboardState, onCategorySelected: (String) -> Unit) {
    var showSecondary by remember { mutableStateOf(false) }
    val total = dashboard.categoryBreakdowns.fold(Money.Zero) { value, item -> value + item.amount }
    if (dashboard.categoryBreakdowns.isEmpty()) return
    AnalyticsCard {
        CategoryDonut(dashboard.categoryBreakdowns, total)
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
        TextButton(onClick = { showSecondary = !showSecondary }, modifier = Modifier.fillMaxWidth()) {
            Text(if (showSecondary) "只看一级分类" else "展开二级分类", style = OmniText.caption)
        }
    }
}

@Composable
private fun CategoryRow(label: String, amount: Money, total: Money, iconKey: String?, color: Color, onClick: () -> Unit) {
    val fraction = if (total.minor == 0L) 0f else amount.minor.toFloat() / total.minor
    Column(
        Modifier.fillMaxWidth().clickable(onClick = onClick),
        verticalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            SvgIcon(categoryIconKey(iconKey), Modifier.size(22.dp), tint = color)
            Spacer(Modifier.width(8.dp))
            Text(label, Modifier.weight(1f), style = OmniText.bodyRow, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(amount.asPlainAmount(), style = OmniText.bodyRow, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.width(6.dp))
            Text("${(fraction * 100).toInt()}%", style = OmniText.caption, color = mutedContent())
        }
        LinearProgressIndicator(
            progress = { fraction.coerceIn(0f, 1f) },
            modifier = Modifier.fillMaxWidth().height(6.dp),
            color = color,
            trackColor = surfaceInset(),
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
    Box(Modifier.fillMaxWidth().height(DonutSize), contentAlignment = Alignment.Center) {
        // 必须是正方形画布：铺满宽度的话 drawArc 会按容器长宽画出椭圆
        Canvas(Modifier.size(DonutSize).semantics { contentDescription = description }) {
            var start = -90f
            items.forEachIndexed { index, item ->
                val sweep = item.amount.minor.toFloat() / totalMinor * 360f
                // 留 1.5 度间隙，扇区之间才有呼吸感
                drawArc(
                    color = palette[index % palette.size],
                    startAngle = start + 0.75f,
                    sweepAngle = (sweep - 1.5f).coerceAtLeast(0.5f),
                    useCenter = false,
                    style = Stroke(width = 28.dp.toPx(), cap = StrokeCap.Butt),
                )
                start += sweep
            }
        }
        // 中心显示合计，否则圆环中间是一块空白
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(total.asPlainAmount(), style = OmniText.amountPrimary, maxLines = 1)
            Text("${items.size} 个分类", style = OmniText.caption, color = mutedContent())
        }
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
@OptIn(ExperimentalMaterial3Api::class)
private fun StatementTableSheet(table: StatementTable, onYear: (Int) -> Unit, onDismiss: () -> Unit) {
    var filter by remember { mutableStateOf(StatementFilter.ALL) }
    ModalBottomSheet(onDismissRequest = onDismiss) {
        LazyColumn(Modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { onYear(table.year - 1) }) { Icon(Icons.Default.ArrowBackIosNew, "上一年", tint = mutedContent()) }
                    Text("${table.year} 年账单", Modifier.weight(1f), textAlign = TextAlign.Center, style = OmniText.titleRow)
                    IconButton(onClick = { onYear(table.year + 1) }) { Icon(Icons.AutoMirrored.Filled.ArrowForwardIos, "下一年", tint = mutedContent()) }
                }
            }
            item {
                OmniSegmented(
                    options = StatementFilter.entries,
                    selected = filter,
                    label = {
                        when (it) {
                            StatementFilter.ALL -> "全部"
                            StatementFilter.INCOME -> "收入"
                            StatementFilter.EXPENSE -> "支出"
                        }
                    },
                    onSelected = { filter = it },
                    modifier = Modifier.fillMaxWidth(),
                )
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
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        months.forEach { month ->
            Column(
                Modifier.weight(1f).semantics {
                    contentDescription = "${month.month}月，收入${month.income.asRmb()}，支出${month.expense.asRmb()}，结余${month.netIncome.asRmb()}"
                },
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Row(Modifier.height(96.dp)) {
                    if (filter != StatementFilter.EXPENSE) {
                        Column(Modifier.weight(1f).fillMaxHeight(), horizontalAlignment = Alignment.End) {
                            VerticalBar(month.income.minor, maximum, incomeColor(), IncomeBarShape)
                        }
                    }
                    if (filter != StatementFilter.INCOME) {
                        Column(Modifier.weight(1f).fillMaxHeight(), horizontalAlignment = Alignment.Start) {
                            VerticalBar(month.expense.minor, maximum, expenseColor(), ExpenseBarShape)
                        }
                    }
                }
                Text("${month.month}月", style = OmniText.caption, color = mutedContent())
            }
        }
    }
}

@Composable
private fun StatementHeader(filter: StatementFilter) {
    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("月份", Modifier.width(46.dp), style = OmniText.caption, color = mutedContent())
        if (filter != StatementFilter.EXPENSE) Text("收入", Modifier.weight(1f), textAlign = TextAlign.End, style = OmniText.caption, color = mutedContent())
        if (filter != StatementFilter.INCOME) Text("支出", Modifier.weight(1f), textAlign = TextAlign.End, style = OmniText.caption, color = mutedContent())
        Text("结余", Modifier.weight(1f), textAlign = TextAlign.End, style = OmniText.caption, color = mutedContent())
    }
}

@Composable
private fun StatementRow(label: String, income: Money, expense: Money, filter: StatementFilter, emphasized: Boolean = false) {
    val net = income - expense
    Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(label, Modifier.width(46.dp), fontWeight = if (emphasized) FontWeight.Bold else FontWeight.SemiBold)
        if (filter != StatementFilter.EXPENSE) {
            Text(income.asPlainAmount(), Modifier.weight(1f), textAlign = TextAlign.End, color = incomeColor(), fontWeight = if (emphasized) FontWeight.Bold else FontWeight.Normal)
        }
        if (filter != StatementFilter.INCOME) {
            Text(expense.asPlainAmount(), Modifier.weight(1f), textAlign = TextAlign.End, color = MaterialTheme.colorScheme.onSurface, fontWeight = if (emphasized) FontWeight.Bold else FontWeight.Normal)
        }
        Text(
            net.asPlainAmount(),
            Modifier.weight(1f),
            textAlign = TextAlign.End,
            color = if (net.minor < 0) expenseColor() else incomeColor(),
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

/** 饼图直径，画布必须是正方形。 */
private val DonutSize = 184.dp
