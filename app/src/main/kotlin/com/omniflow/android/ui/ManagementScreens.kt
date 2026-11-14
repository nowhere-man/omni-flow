package com.omniflow.android.ui

import com.omniflow.core.domain.model.Money
import com.omniflow.core.domain.model.ReminderScheduleKind
import com.omniflow.core.domain.model.ReminderType
import com.omniflow.core.domain.model.TransactionType

internal val TransactionType.label: String get() = when (this) {
    TransactionType.EXPENSE -> "支出"
    TransactionType.INCOME -> "收入"
    TransactionType.TRANSFER -> "转账"
}

internal val ReminderType.label: String get() = when (this) {
    ReminderType.REPAYMENT -> "账单还款"
    ReminderType.SUBSCRIPTION -> "周期记账"
}

internal val ReminderScheduleKind.label: String get() = when (this) {
    ReminderScheduleKind.FIXED_REPAYMENT_DAY -> "固定还款日"
    ReminderScheduleKind.DAYS_AFTER_STATEMENT -> "账单日后 N 天"
    ReminderScheduleKind.DAILY -> "每天"
    ReminderScheduleKind.WEEKLY -> "每周"
    ReminderScheduleKind.MONTHLY -> "每月"
    ReminderScheduleKind.YEARLY -> "每年"
}

internal fun String.toMoneyOrNull(): Money? {
    val value = trim().toBigDecimalOrNull() ?: return null
    if (value.scale() > 2) return null
    return runCatching { Money(value.movePointRight(2).longValueExact()) }.getOrNull()
}
