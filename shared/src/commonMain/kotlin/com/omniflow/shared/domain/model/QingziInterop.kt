package com.omniflow.shared.domain.model

data class QingziExportRequest(
    val transactionIds: Set<TransactionId> = emptySet(),
    val dateRange: DateRange? = null,
)

data class QingziExportResult(
    val payload: String,
    val exportedTransactions: Int,
    val warnings: List<String>,
)
