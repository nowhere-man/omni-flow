package com.omniflow.android.ui.designsystem.components

import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import kotlinx.datetime.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OmniDatePickerDialog(
    initialDate: LocalDate,
    onDateSelected: (LocalDate) -> Unit,
    onDismissRequest: () -> Unit,
    confirmText: String = "确定",
    dismissText: String = "取消",
    title: @Composable (() -> Unit)? = null,
) {
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = initialDate.toUtcEpochMillis(),
    )

    DatePickerDialog(
        onDismissRequest = onDismissRequest,
        confirmButton = {
            TextButton(
                onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        onDateSelected(millis.toLocalDateFromUtcMillis())
                    }
                    onDismissRequest()
                },
            ) {
                Text(confirmText)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text(dismissText)
            }
        },
    ) {
        DatePicker(
            state = datePickerState,
            title = title,
        )
    }
}

private fun LocalDate.toUtcEpochMillis(): Long =
    java.time.LocalDate.of(year, monthNumber, dayOfMonth)
        .atStartOfDay(java.time.ZoneOffset.UTC)
        .toInstant()
        .toEpochMilli()

private fun Long.toLocalDateFromUtcMillis(): LocalDate {
    val javaDate = java.time.Instant.ofEpochMilli(this)
        .atZone(java.time.ZoneOffset.UTC)
        .toLocalDate()
    return LocalDate(javaDate.year, javaDate.monthValue, javaDate.dayOfMonth)
}
