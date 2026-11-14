package com.omniflow.android.ui.feature.analytics

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.omniflow.android.ui.asRmb
import com.omniflow.android.ui.designsystem.tokens.expenseColor
import com.omniflow.android.ui.designsystem.tokens.incomeColor
import com.omniflow.core.domain.model.FinancialHealthSummary
import com.omniflow.core.domain.model.Money
import com.omniflow.core.domain.model.TransactionType
import java.util.Locale

@Composable
fun HealthSummaryCard(
    healthSummary: FinancialHealthSummary,
    modifier: Modifier = Modifier,
    onSummaryClick: ((TransactionType?) -> Unit)? = null,
) {
    val isNetPositive = healthSummary.netSavings.minor >= 0L
    val savingsColor = if (isNetPositive) incomeColor() else expenseColor()
    val rate = healthSummary.savingsRatePercentage

    val rateLabel = if (rate != null) {
        val formatted = String.format(Locale.US, "%.1f", rate)
        if (rate >= 0.0) "结余率 $formatted%" else "超支 $formatted%"
    } else {
        "结余率 --"
    }

    ElevatedCard(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Main Hero Row: Net Savings + Savings Rate Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column {
                    Text(
                        text = "净结余",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = healthSummary.netSavings.asRmb(),
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontFeatureSettings = "tnum",
                        ),
                        color = savingsColor,
                    )
                }

                SuggestionChip(
                    onClick = {},
                    label = {
                        Text(
                            text = rateLabel,
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                fontFeatureSettings = "tnum",
                            ),
                        )
                    },
                    colors = SuggestionChipDefaults.suggestionChipColors(
                        containerColor = if (isNetPositive) {
                            incomeColor().copy(alpha = 0.12f)
                        } else {
                            expenseColor().copy(alpha = 0.12f)
                        },
                        labelColor = savingsColor,
                    ),
                    border = null,
                )
            }

            // Sub-indicators: Total Income | Total Expense | Daily Average
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Total Income
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(enabled = onSummaryClick != null) {
                            onSummaryClick?.invoke(TransactionType.INCOME)
                        }
                        .padding(horizontal = 4.dp, vertical = 4.dp),
                ) {
                    Text(
                        text = "总收入",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = healthSummary.income.asRmb(),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontFeatureSettings = "tnum",
                        ),
                        color = incomeColor(),
                    )
                }

                VerticalDivider(
                    modifier = Modifier
                        .fillMaxHeight()
                        .padding(horizontal = 4.dp),
                    color = MaterialTheme.colorScheme.outlineVariant,
                )

                // Total Expense
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(enabled = onSummaryClick != null) {
                            onSummaryClick?.invoke(TransactionType.EXPENSE)
                        }
                        .padding(horizontal = 4.dp, vertical = 4.dp),
                ) {
                    Text(
                        text = "总支出",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = healthSummary.expense.asRmb(),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontFeatureSettings = "tnum",
                        ),
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }

                VerticalDivider(
                    modifier = Modifier
                        .fillMaxHeight()
                        .padding(horizontal = 4.dp),
                    color = MaterialTheme.colorScheme.outlineVariant,
                )

                // Daily Average
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 4.dp, vertical = 4.dp),
                ) {
                    Text(
                        text = "日均支出",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = healthSummary.dailyAverageExpense.asRmb(),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontFeatureSettings = "tnum",
                        ),
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        }
    }
}
