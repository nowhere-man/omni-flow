package com.omniflow.shared.domain.model

object AppPreferenceKey {
    const val DefaultLedgerId = "default_ledger_id"
    const val HomeLedgerScope = "home_ledger_scope"
    const val AnalyticsLedgerScope = "analytics_ledger_scope"
    const val TransactionDetailDisplayMode = "transaction_detail_display_mode"
    const val AppearanceMode = "appearance_mode"
    const val SyncTarget = "sync_target"
    const val BackupRetention = "backup_retention"

    internal const val InitialDataSeeded = "initial_data_seeded"
}

const val DEFAULT_BACKUP_RETENTION = 10L
