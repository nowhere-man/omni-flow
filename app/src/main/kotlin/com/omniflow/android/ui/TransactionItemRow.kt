package com.omniflow.android.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.omniflow.core.domain.model.Money
import com.omniflow.core.domain.model.TransactionType

/**
 * 首页与导入预览共用的记账项展示。首页传的是已有交易，导入预览传的是待入账的明细，
 * 两边的图标、分类显示名、金额语义色和时间排版必须一致，所以统一走这里。
 */
@Composable
internal fun TransactionRow(
    iconKey: String?,
    title: String,
    subtitle: String?,
    amount: Money,
    type: TransactionType?,
    timeText: String?,
    modifier: Modifier = Modifier,
    dimmed: Boolean = false,
    titleMuted: Boolean = false,
    trailing: (@Composable () -> Unit)? = null,
    onClick: (() -> Unit)? = null,
) {
    val alpha = if (dimmed) 0.45f else 1f
    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CategoryIcon(iconKey, dimmed = dimmed)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                title,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = OmniText.bodyRow.copy(fontWeight = FontWeight.Medium),
                color = if (titleMuted) {
                    MaterialTheme.colorScheme.onSurfaceVariant
                } else {
                    MaterialTheme.colorScheme.onSurface.copy(alpha = alpha)
                },
            )
            subtitle?.takeIf(String::isNotBlank)?.let {
                Text(
                    it,
                    color = mutedContent().copy(alpha = alpha),
                    style = OmniText.caption,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Column(horizontalAlignment = Alignment.End) {
            AmountText(amount, type, dimmed = dimmed)
            timeText?.let {
                Text(
                    it,
                    color = mutedContent().copy(alpha = alpha),
                    style = OmniText.caption,
                )
            }
        }
        trailing?.let {
            Spacer(Modifier.width(4.dp))
            it()
        }
    }
}

/** 方框形式，字段与 [TransactionRow] 一致。 */
@Composable
internal fun TransactionTile(
    iconKey: String?,
    title: String,
    subtitle: String?,
    amount: Money,
    type: TransactionType?,
    timeText: String?,
    modifier: Modifier = Modifier,
    dimmed: Boolean = false,
    titleMuted: Boolean = false,
    onClick: (() -> Unit)? = null,
) {
    val alpha = if (dimmed) 0.45f else 1f
    Card(
        modifier = modifier.then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        shape = RoundedCornerShape(OmniRadius.small),
        colors = CardDefaults.cardColors(containerColor = surfaceCard()),
    ) {
        // 高度靠内边距和行距压缩：10dp 内边距 + 6dp 行距，比原来矮 14dp
        Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CategoryIcon(iconKey, dimmed = dimmed, size = 28)
                Spacer(Modifier.width(4.dp))
                // 金额和图标挤在同一行，字号按位数收，否则四位数以上就被省略号吃掉
                AmountText(
                    amount,
                    type,
                    dimmed = dimmed,
                    style = OmniText.amountTile(amount.asPlainAmount().length + 1),
                    modifier = Modifier.weight(1f),
                )
            }
            Text(
                title,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = OmniText.caption.copy(fontSize = 13.sp, fontWeight = FontWeight.Medium),
                color = if (titleMuted) mutedContent() else MaterialTheme.colorScheme.onSurface.copy(alpha = alpha),
            )
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                val note = subtitle?.takeIf(String::isNotBlank)
                if (note != null) {
                    Text(
                        note,
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = mutedContent().copy(alpha = alpha),
                        style = OmniText.caption,
                    )
                } else {
                    Spacer(Modifier.weight(1f))
                }
                timeText?.let {
                    Text(it, color = mutedContent().copy(alpha = alpha), style = OmniText.caption)
                }
            }
        }
    }
}

/**
 * [type] 为 null 时不带正负号，用于「不计收支」这类没有收支方向的明细。
 * 金额不带货币符号：符号在一屏几十行里只是噪音，去掉后同宽度能多放两位数字。
 */
@Composable
internal fun AmountText(
    amount: Money,
    type: TransactionType?,
    dimmed: Boolean = false,
    style: TextStyle? = null,
    modifier: Modifier = Modifier,
) {
    val alpha = if (dimmed) 0.45f else 1f
    val sign = when (type) {
        TransactionType.EXPENSE -> "-"
        TransactionType.INCOME -> "+"
        null -> ""
    }
    Text(
        "$sign${amount.asPlainAmount()}",
        modifier = modifier,
        color = amountColor(type).copy(alpha = alpha),
        style = style ?: OmniText.amountPrimary,
        maxLines = 1,
        textAlign = TextAlign.End,
        overflow = TextOverflow.Ellipsis,
    )
}

/** 明细里支出用中性色，只有收入上色；日历格子和图表另外走 [expenseColor]。 */
@Composable
internal fun amountColor(type: TransactionType?): Color = when (type) {
    TransactionType.EXPENSE -> MaterialTheme.colorScheme.onSurface
    TransactionType.INCOME -> incomeColor()
    null -> MaterialTheme.colorScheme.onSurfaceVariant
}

// 收支语义色固定，不跟随主题色，也不用 error/tertiary——那两个会随主题色漂移，
// 结果日历、首页汇总和明细列表的收支颜色对不上。避开纯红纯绿，深浅模式各一套。
private val IncomeLight = Color(0xFF4E9367)
private val IncomeDark = Color(0xFF7FC49A)
private val ExpenseLight = Color(0xFFC2635C)
private val ExpenseDark = Color(0xFFE09A94)

@Composable
internal fun incomeColor(): Color = if (isDarkColorScheme()) IncomeDark else IncomeLight

@Composable
internal fun expenseColor(): Color = if (isDarkColorScheme()) ExpenseDark else ExpenseLight

@Composable
private fun isDarkColorScheme(): Boolean = MaterialTheme.colorScheme.surface.luminance() < 0.5f

@Composable
internal fun CategoryIcon(iconKey: String?, dimmed: Boolean = false, size: Int = 36) {
    Surface(
        modifier = Modifier.size(size.dp),
        shape = CircleShape,
        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = if (dimmed) 0.45f else 1f),
    ) {
        SvgIcon(iconKey ?: "category", Modifier.padding((size * 0.22).toInt().dp))
    }
}
