package com.omniflow.android.ui.feature.rule

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.RemoveCircleOutline
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.omniflow.android.ui.OmniText
import com.omniflow.android.ui.designsystem.tokens.CategoryBadgeColors
import com.omniflow.android.ui.designsystem.tokens.OmniColorTokens
import com.omniflow.android.ui.designsystem.tokens.categoryBadgeColors
import com.omniflow.android.ui.designsystem.tokens.getCategoryIcon
import com.omniflow.android.ui.mutedContent
import com.omniflow.core.domain.model.Category
import com.omniflow.core.domain.model.Rule
import com.omniflow.core.domain.model.RuleActionType
import com.omniflow.core.domain.model.RuleConditionType
import com.omniflow.core.domain.model.TransactionType

/**
 * 规则句子卡（缺陷 8.3 核心组件）：
 * 左侧 40dp 圆形徽章（带右下优先级药丸），
 * 中部 `当 [条件筹码] → [动作筹码]` 积木句法。
 */
@Composable
internal fun RuleSentenceCard(
    rule: Rule,
    categories: List<Category>,
    dragging: Boolean,
    onEdit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val targetCategory = if (rule.actionType == RuleActionType.SET_CATEGORY) {
        categories.firstOrNull { it.id == rule.actionValue }
    } else null

    val badgeColors = when (rule.actionType) {
        RuleActionType.SET_CATEGORY -> categoryBadgeColors(targetCategory?.iconKey)
        RuleActionType.SET_EXCLUDED, RuleActionType.EXCLUDE -> CategoryBadgeColors(
            container = OmniColorTokens.BadgePlanningContainerLight,
            content = OmniColorTokens.BadgePlanningContentLight,
        )
    }

    val actionIcon = when (rule.actionType) {
        RuleActionType.SET_CATEGORY -> getCategoryIcon(targetCategory?.iconKey)
        RuleActionType.SET_EXCLUDED -> Icons.Default.RemoveCircleOutline
        RuleActionType.EXCLUDE -> Icons.Default.Block
    }

    ElevatedCard(
        onClick = onEdit,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = if (dragging) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surfaceContainerLow,
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = if (dragging) 6.dp else 0.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // 左侧 40dp 圆形徽章 + 右下优先级小药丸
            Box(modifier = Modifier.size(44.dp)) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(badgeColors.container),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = actionIcon,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = badgeColors.content,
                    )
                }

                // 优先级小药丸
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceContainerHighest,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .offset(x = 2.dp, y = 2.dp),
                ) {
                    Text(
                        text = "${rule.priority}",
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Spacer(Modifier.width(14.dp))

            // 中部句子与说明
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    text = rule.name,
                    style = OmniText.titleRow,
                    color = MaterialTheme.colorScheme.onSurface,
                )

                // 当 [条件] → [动作]
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        text = "当",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium),
                        color = mutedContent(),
                    )

                    RuleChip(
                        text = conditionText(rule.conditionType, rule.conditionValue),
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    )

                    Icon(
                        Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        modifier = Modifier.size(12.dp),
                        tint = mutedContent(),
                    )

                    RuleChip(
                        text = actionText(rule.actionType, targetCategory?.name ?: rule.actionValue),
                        containerColor = badgeColors.container,
                        contentColor = badgeColors.content,
                    )
                }
            }

            Icon(
                Icons.Default.DragHandle,
                contentDescription = "拖拽排序",
                tint = MaterialTheme.colorScheme.outlineVariant,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

@Composable
private fun RuleChip(
    text: String,
    containerColor: Color,
    contentColor: Color,
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = containerColor,
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
            color = contentColor,
            maxLines = 1,
        )
    }
}

private fun conditionText(type: RuleConditionType, value: String): String = when (type) {
    RuleConditionType.NOTE_CONTAINS -> "备注含 \"$value\""
    RuleConditionType.TRANSACTION_TYPE -> when (value) {
        TransactionType.INCOME.name -> "类型为收入"
        TransactionType.EXPENSE.name -> "类型为支出"
        TransactionType.TRANSFER.name -> "类型为转账"
        else -> "类型为 $value"
    }
    RuleConditionType.TRANSACTION_SOURCE -> "来源为 $value"
}

private fun actionText(type: RuleActionType, targetCategoryName: String): String = when (type) {
    RuleActionType.SET_CATEGORY -> "归入「$targetCategoryName」"
    RuleActionType.SET_EXCLUDED -> "设为不计入收支"
    RuleActionType.EXCLUDE -> "排除不入账"
}
