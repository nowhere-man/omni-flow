package com.omniflow.android.ui.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.omniflow.android.ui.expenseColor
import com.omniflow.android.ui.incomeColor
import com.omniflow.core.domain.model.AppTimeZone
import com.omniflow.core.domain.model.CalendarDaySummary
import com.omniflow.core.domain.model.calendarAmountText
import java.time.LocalDate as JavaLocalDate
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.toLocalDateTime

private val CalendarCellHeight = 58.dp

@Composable
internal fun CalendarMonth(
    month: LocalDate,
    summaries: List<CalendarDaySummary>,
    onDateSelected: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
) {
    val totals = remember(summaries) { summaries.associateBy(CalendarDaySummary::date) }
    val today = remember { Clock.System.now().toLocalDateTime(AppTimeZone).date }
    val first = JavaLocalDate.of(month.year, month.monthNumber, 1)
    val leading = first.dayOfWeek.value % 7
    val cells = List(leading) { null } + (1..first.lengthOfMonth()).map { day ->
        LocalDate(month.year, month.monthNumber, day)
    }
    Column(modifier, verticalArrangement = Arrangement.spacedBy(1.dp)) {
        Row(Modifier.fillMaxWidth()) {
            listOf("日", "一", "二", "三", "四", "五", "六").forEach { weekday ->
                Text(
                    weekday,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.labelSmall,
                )
            }
        }
        cells.chunked(7).forEach { week ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                week.forEach { date ->
                    CalendarCell(date, date == today, date?.let(totals::get), onDateSelected, Modifier.weight(1f))
                }
                repeat(7 - week.size) { Spacer(Modifier.weight(1f).height(CalendarCellHeight)) }
            }
        }
    }
}

@Composable
private fun CalendarCell(
    date: LocalDate?,
    isToday: Boolean,
    summary: CalendarDaySummary?,
    onDateSelected: (LocalDate) -> Unit,
    modifier: Modifier,
) {
    if (date == null) {
        Spacer(modifier.height(CalendarCellHeight))
        return
    }
    Column(
        modifier = modifier
            .height(CalendarCellHeight)
            .clip(RoundedCornerShape(6.dp))
            .clickable { onDateSelected(date) }
            .padding(vertical = 1.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(26.dp)
                .background(
                    if (isToday) MaterialTheme.colorScheme.primary else Color.Transparent,
                    CircleShape,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                date.dayOfMonth.toString(),
                color = if (isToday) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.labelLarge.copy(fontSize = 15.sp),
                fontWeight = FontWeight.SemiBold,
            )
        }
        // 收入在上、支出在下，只显示整数，超过 1000 走 x.xk；金额为 0 的那一行不占位
        summary?.incomeTotal?.takeIf { it.minor > 0 }?.let {
            CalendarAmount(it.calendarAmountText(), incomeColor())
        }
        summary?.expenseTotal?.takeIf { it.minor > 0 }?.let {
            CalendarAmount(it.calendarAmountText(), expenseColor())
        }
    }
}

@Composable
private fun CalendarAmount(text: String, color: Color) {
    Text(
        text,
        color = color,
        maxLines = 1,
        softWrap = false,
        overflow = TextOverflow.Clip,
        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, lineHeight = 12.sp),
    )
}
