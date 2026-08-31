package com.omniflow.core.domain.model

data class QingziExportRequest(
    val transactionIds: Set<TransactionId> = emptySet(),
    val ledgerIds: Set<LedgerId> = emptySet(),
    val dateRange: DateRange? = null,
    val type: TransactionType? = null,
)

data class QingziExportResult(
    val payload: String,
    val exportedTransactions: Int,
    val warnings: List<String>,
)
