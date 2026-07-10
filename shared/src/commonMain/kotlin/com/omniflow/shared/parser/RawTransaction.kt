package com.omniflow.shared.parser

import com.omniflow.shared.domain.model.Money
import com.omniflow.shared.domain.model.TransactionType
import kotlinx.datetime.Instant

data class RawTransaction(
    val format: ImportFormat,
    val occurredAt: Instant,
    val amount: Money,
    val type: TransactionType?,
    val isExcluded: Boolean,
    val accountName: String?,
    val note: String?,
    val externalId: String?,
    val sourceCategory: String?,
    val sourceLedgerName: String? = null,
    val tags: List<String> = emptyList(),
)
