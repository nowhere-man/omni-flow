package com.omniflow.android.ui.feature.analytics

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.omniflow.android.ui.designsystem.tokens.expenseColor
import com.omniflow.android.ui.designsystem.tokens.incomeColor
import com.omniflow.core.domain.model.FinancialInsight
import com.omniflow.core.domain.model.InsightType

@Composable
fun FinancialInsightCard(
    insight: FinancialInsight,
    modifier: Modifier = Modifier,
) {
    val isAlert = insight.type in listOf(
        InsightType.ANOMALY_INCREASE,
        InsightType.LARGE_EXPENSE_ALERT,
        InsightType.DEFICIT_WARNING,
    )
    val isAchievement = insight.type == InsightType.SAVINGS_ACHIEVEMENT

    val containerColor = when {
        isAlert -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
        isAchievement -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
        else -> MaterialTheme.colorScheme.surfaceContainerLow
    }

    val icon: ImageVector = when (insight.type) {
        InsightType.ANOMALY_INCREASE -> Icons.AutoMirrored.Filled.TrendingUp
        InsightType.SAVINGS_ACHIEVEMENT -> Icons.Default.CheckCircle
        InsightType.LARGE_EXPENSE_ALERT -> Icons.Default.Warning
        InsightType.CONCENTRATED_CATEGORY -> Icons.Default.Info
        InsightType.DEFICIT_WARNING -> Icons.AutoMirrored.Filled.TrendingDown
        InsightType.BALANCED_DEFAULT -> Icons.Default.Info
    }

    val iconTint: Color = when {
        isAlert -> expenseColor()
        isAchievement -> incomeColor()
        else -> MaterialTheme.colorScheme.primary
    }

    ElevatedCard(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = containerColor),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 0.5.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(iconTint.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconTint,
                        modifier = Modifier.size(18.dp),
                    )
                }
                Text(
                    text = insight.title,
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }

            Text(
                text = insight.message,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = MaterialTheme.typography.bodyMedium.lineHeight,
            )
        }
    }
}
