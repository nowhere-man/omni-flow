package com.omniflow.shared.domain.model

import kotlinx.datetime.Instant

typealias ReminderId = String

enum class ReminderType { REPAYMENT, SUBSCRIPTION }

enum class ReminderScheduleKind {
    FIXED_REPAYMENT_DAY,
    DAYS_AFTER_STATEMENT,
    DAILY,
    WEEKLY,
    MONTHLY,
    YEARLY,
}

data class ReminderSchedule(
    val kind: ReminderScheduleKind,
    val dayOfMonth: Int? = null,
    val daysAfter: Int? = null,
    val dayOfWeek: Int? = null,
    val month: Int? = null,
) {
    init {
        when (kind) {
            ReminderScheduleKind.FIXED_REPAYMENT_DAY,
            ReminderScheduleKind.MONTHLY -> require(dayOfMonth in 1..31) { "日期必须为 1 到 31" }
            ReminderScheduleKind.DAYS_AFTER_STATEMENT -> {
                require(dayOfMonth in 1..31) { "账单日必须为 1 到 31" }
                require(daysAfter != null && daysAfter >= 0) { "账单日后天数不能小于零" }
            }
            ReminderScheduleKind.WEEKLY -> require(dayOfWeek in 1..7) { "星期必须为 1 到 7" }
            ReminderScheduleKind.YEARLY -> {
                require(month in 1..12) { "月份必须为 1 到 12" }
                require(dayOfMonth in 1..31) { "日期必须为 1 到 31" }
            }
            ReminderScheduleKind.DAILY -> Unit
        }
    }
}

data class Reminder(
    val id: ReminderId,
    val type: ReminderType,
    val name: String,
    val amount: Money?,
    val schedule: ReminderSchedule,
    val paused: Boolean = false,
    val deletedAt: Instant? = null,
) {
    init {
        require(name.isNotBlank()) { "提醒名称不能为空" }
        require(amount == null || amount >= Money.Zero) { "提醒金额不能小于零" }
        require(
            when (type) {
                ReminderType.REPAYMENT -> schedule.kind in setOf(
                    ReminderScheduleKind.FIXED_REPAYMENT_DAY,
                    ReminderScheduleKind.DAYS_AFTER_STATEMENT,
                )
                ReminderType.SUBSCRIPTION -> schedule.kind in setOf(
                    ReminderScheduleKind.DAILY,
                    ReminderScheduleKind.WEEKLY,
                    ReminderScheduleKind.MONTHLY,
                    ReminderScheduleKind.YEARLY,
                )
            },
        ) { "提醒类型与重复规则不匹配" }
    }
}
