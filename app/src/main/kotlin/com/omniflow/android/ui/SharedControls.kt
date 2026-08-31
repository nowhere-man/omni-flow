package com.omniflow.android.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.omniflow.android.R
import com.omniflow.core.domain.model.Ledger
import com.omniflow.core.domain.model.LedgerScope
import com.omniflow.core.domain.model.Money
import com.omniflow.core.parser.ImportFormat

/**
 * 账本选择药丸。首页、统计、记账页都是「账本单独占一行」，
 * 三个页面共用同一个控件，样式和点击区域才不会各写一套。
 */
@Composable
internal fun LedgerScopePill(
    label: String,
    ledgers: List<Ledger>,
    onAll: (() -> Unit)?,
    onSelected: (Ledger) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier) {
        Surface(
            onClick = { expanded = true },
            shape = CircleShape,
            color = surfaceInset(),
        ) {
            Row(
                Modifier.padding(start = 12.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Icon(Icons.AutoMirrored.Filled.MenuBook, null, Modifier.size(18.dp), tint = mutedContent())
                Text(label, style = MaterialTheme.typography.labelLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Icon(Icons.Default.ExpandMore, null, Modifier.size(18.dp), tint = mutedContent())
            }
        }
        DropdownMenu(expanded, onDismissRequest = { expanded = false }) {
            onAll?.let { all ->
                DropdownMenuItem({ Text("所有账本") }, onClick = { expanded = false; all() })
            }
            ledgers.forEach { ledger ->
                DropdownMenuItem(
                    { Text(ledger.name) },
                    onClick = { expanded = false; onSelected(ledger) },
                    leadingIcon = { LedgerCoverBox(ledger.coverKey, Modifier.size(26.dp), iconSize = 15) },
                )
            }
        }
    }
}

@Composable
internal fun LedgerScopePill(
    scope: LedgerScope,
    ledgers: List<Ledger>,
    onScope: (LedgerScope) -> Unit,
    modifier: Modifier = Modifier,
) = LedgerScopePill(
    label = when (scope) {
        LedgerScope.All -> "所有账本"
        is LedgerScope.Single -> ledgers.firstOrNull { it.id == scope.ledgerId }?.name ?: "账本"
    },
    ledgers = ledgers,
    onAll = { onScope(LedgerScope.All) },
    onSelected = { onScope(LedgerScope.Single(it.id)) },
    modifier = modifier,
)

/** 周期切换：两个箭头顶在两端，标签居中，首页和统计页同一套。 */
@Composable
internal fun RangeStepper(
    label: String,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier,
    onLabel: (() -> Unit)? = null,
) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onPrevious) {
            Icon(Icons.Default.ArrowBackIosNew, "上一个周期", Modifier.size(18.dp), tint = mutedContent())
        }
        val text = @Composable {
            Text(
                label,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                style = OmniText.titleRow,
                maxLines = 1,
            )
        }
        Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
            if (onLabel == null) {
                text()
            } else {
                Surface(onClick = onLabel, shape = CircleShape, color = Color.Transparent) {
                    Box(Modifier.padding(horizontal = 12.dp, vertical = 6.dp)) { text() }
                }
            }
        }
        IconButton(onClick = onNext) {
            Icon(Icons.AutoMirrored.Filled.ArrowForwardIos, "下一个周期", Modifier.size(18.dp), tint = mutedContent())
        }
    }
}

/**
 * 三张汇总卡：总支出 / 总收入 / 总结余。首页和统计页共用，
 * 统计页原来那张「收支汇总」卡是同一份信息的第二种排版，删掉换成这里。
 *
 * 金额只保留整数、不带货币符号：一行三张卡各占三分之一屏宽，
 * `¥` 和 `.00` 会先把五位数以上的金额挤成省略号。
 */
@Composable
internal fun SummaryCardRow(
    expense: Money,
    income: Money,
    modifier: Modifier = Modifier,
    onExpense: (() -> Unit)? = null,
    onIncome: (() -> Unit)? = null,
    onNet: (() -> Unit)? = null,
) {
    val net = income - expense
    val texts = listOf(expense, income, net).map(Money::asWholeAmount)
    // 字号按最长的那个数算，三张卡共用：各算各的会让并排的三个数字大小不一
    val style = OmniText.amountCard(texts.maxOf(String::length))
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        // 支出占绝大多数，全标红会让整屏发碎；只给收入和结余语义色
        SummaryCard("总支出", texts[0], style, MaterialTheme.colorScheme.onSurface, onExpense, Modifier.weight(1f))
        SummaryCard("总收入", texts[1], style, incomeColor(), onIncome, Modifier.weight(1f))
        SummaryCard(
            "总结余",
            texts[2],
            style,
            if (net.minor >= 0) incomeColor() else expenseColor(),
            onNet,
            Modifier.weight(1f),
        )
    }
}

@Composable
private fun SummaryCard(
    label: String,
    amount: String,
    style: TextStyle,
    color: Color,
    onClick: (() -> Unit)?,
    modifier: Modifier,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(OmniRadius.medium),
        color = surfaceCard(),
        onClick = onClick ?: {},
        enabled = onClick != null,
    ) {
        Column(
            Modifier.padding(horizontal = OmniSpace.s, vertical = OmniSpace.m),
            verticalArrangement = Arrangement.spacedBy(OmniSpace.xs),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(label, style = OmniText.caption, color = mutedContent(), maxLines = 1)
            Text(
                amount,
                style = style,
                color = color,
                maxLines = 1,
                textAlign = TextAlign.Center,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/**
 * 账单来源图标。品牌图标比统一的灰色文件图标好认得多，
 * 没有官方图标的来源（美团、中国银行）退回品牌色首字块，不留一个空位。
 */
@Composable
internal fun ImportSourceIcon(format: ImportFormat, modifier: Modifier = Modifier) {
    val drawable = when (format) {
        ImportFormat.ALIPAY -> R.drawable.brand_alipay
        ImportFormat.WECHAT -> R.drawable.brand_wechat
        ImportFormat.JD -> R.drawable.brand_jd
        ImportFormat.CCB -> R.drawable.brand_ccb
        ImportFormat.QINGZI -> R.drawable.brand_qingzi
        ImportFormat.MEITUAN -> R.drawable.brand_meituan
        ImportFormat.BOC -> R.drawable.brand_boc
        ImportFormat.CMB -> R.drawable.brand_cmb
    }
    val shape = RoundedCornerShape(OmniRadius.small)
    if (drawable != null) {
        Image(
            painter = painterResource(drawable),
            contentDescription = null,
            modifier = modifier.clip(shape),
        )
        return
    }
    val (background, content) = when (format) {
        ImportFormat.MEITUAN -> Color(0xFFFFD100) to Color(0xFF1A1A1A)
        else -> Color(0xFFAF1E24) to Color.White
    }
    Box(modifier.clip(shape).background(background), contentAlignment = Alignment.Center) {
        Text(format.label.take(1), color = content, style = OmniText.titleRow, fontWeight = FontWeight.Bold)
    }
}

internal val ImportFormat.label: String
    get() = when (this) {
        ImportFormat.ALIPAY -> "支付宝"
        ImportFormat.WECHAT -> "微信"
        ImportFormat.JD -> "京东"
        ImportFormat.MEITUAN -> "美团"
        ImportFormat.CCB -> "建设银行"
        ImportFormat.BOC -> "中国银行"
        ImportFormat.CMB -> "招商银行"
        ImportFormat.QINGZI -> "青子记账"
    }
