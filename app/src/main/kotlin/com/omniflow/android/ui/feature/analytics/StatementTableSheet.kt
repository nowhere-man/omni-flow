package com.omniflow.android.ui.feature.analytics

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.omniflow.android.ui.asRmb
import com.omniflow.android.ui.designsystem.tokens.expenseColor
import com.omniflow.android.ui.designsystem.tokens.incomeColor
import com.omniflow.core.domain.model.Money
import com.omniflow.core.domain.model.StatementMonth
import com.omniflow.core.domain.model.StatementTable
import kotlin.math.max

private enum class StatementFilter { ALL, INCOME, EXPENSE }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatementTableSheet(
    table: StatementTable,
    onYearChange: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    var filter by remember { mutableStateOf(StatementFilter.ALL) }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = { onYearChange(table.year - 1) }) {
                        Icon(Icons.Default.ArrowBackIosNew, "上一年")
                    }
                    Text(
                        text = "${table.year} 年账单",
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    )
                    IconButton(onClick = { onYearChange(table.year + 1) }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowForwardIos, "下一年")
                    }
                }
            }

            item {
                val filters = StatementFilter.entries
                SingleChoiceSegmentedButtonRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(36.dp),
                ) {
                    filters.forEachIndexed { index, item ->
                        SegmentedButton(
                            shape = SegmentedButtonDefaults.itemShape(index = index, count = filters.size),
                            onClick = { filter = item },
                            selected = filter == item,
                            label = {
                                Text(
                                    when (item) {
                                        StatementFilter.ALL -> "全部"
                                        StatementFilter.INCOME -> "收入"
                                        StatementFilter.EXPENSE -> "支出"
                                    },
                                    style = MaterialTheme.typography.labelMedium,
                                )
                            },
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
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        months.forEach { month ->
            Column(
                Modifier
                    .weight(1f)
                    .semantics {
                        contentDescription = "${month.month}月，收入${month.income.asRmb()}，支出${month.expense.asRmb()}，结余${month.netIncome.asRmb()}"
                    },
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Row(Modifier.height(96.dp), verticalAlignment = Alignment.Bottom) {
                    if (filter != StatementFilter.EXPENSE) {
                        val incomeFrac = (month.income.minor.toFloat() / maximum.toFloat()).coerceIn(0f, 1f)
                        Box(
                            Modifier
                                .weight(1f)
                                .fillMaxHeight(incomeFrac.coerceAtLeast(0.02f))
                                .clip(RoundedCornerShape(topStart = 2.dp, topEnd = 2.dp))
                                .background(incomeColor()),
                        )
                    }
                    if (filter != StatementFilter.INCOME) {
                        val expenseFrac = (month.expense.minor.toFloat() / maximum.toFloat()).coerceIn(0f, 1f)
                        Box(
                            Modifier
                                .weight(1f)
                                .fillMaxHeight(expenseFrac.coerceAtLeast(0.02f))
                                .clip(RoundedCornerShape(topStart = 2.dp, topEnd = 2.dp))
                                .background(expenseColor()),
                        )
                    }
                }
                Text(
                    text = "${month.month}月",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
private fun StatementHeader(filter: StatementFilter) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Text("月份", Modifier.weight(1f), style = MaterialTheme.typography.labelMedium)
        if (filter != StatementFilter.EXPENSE) {
            Text("收入", Modifier.weight(1.2f), textAlign = TextAlign.End, style = MaterialTheme.typography.labelMedium)
        }
        if (filter != StatementFilter.INCOME) {
            Text("支出", Modifier.weight(1.2f), textAlign = TextAlign.End, style = MaterialTheme.typography.labelMedium)
        }
        if (filter == StatementFilter.ALL) {
            Text("结余", Modifier.weight(1.2f), textAlign = TextAlign.End, style = MaterialTheme.typography.labelMedium)
        }
    }
}

@Composable
private fun StatementRow(
    label: String,
    income: Money,
    expense: Money,
    filter: StatementFilter,
    isTotal: Boolean = false,
) {
    val net = income - expense
    val fontWeight = if (isTotal) FontWeight.Bold else FontWeight.Normal
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium.copy(fontWeight = fontWeight))
        if (filter != StatementFilter.EXPENSE) {
            Text(
                income.asRmb(),
                Modifier.weight(1.2f),
                textAlign = TextAlign.End,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = fontWeight,
                    fontFeatureSettings = "tnum",
                ),
                color = incomeColor(),
            )
        }
        if (filter != StatementFilter.INCOME) {
            Text(
                expense.asRmb(),
                Modifier.weight(1.2f),
                textAlign = TextAlign.End,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = fontWeight,
                    fontFeatureSettings = "tnum",
                ),
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
        if (filter == StatementFilter.ALL) {
            Text(
                net.asRmb(),
                Modifier.weight(1.2f),
                textAlign = TextAlign.End,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = fontWeight,
                    fontFeatureSettings = "tnum",
                ),
                color = if (net.minor >= 0L) incomeColor() else expenseColor(),
            )
        }
    }
}
