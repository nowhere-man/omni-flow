package com.omniflow.core.parser

import com.omniflow.core.domain.model.Money
import com.omniflow.core.domain.model.TransactionType
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
    val counterparty: String? = null,
    val sourceLedgerName: String? = null,
    val tags: List<String> = emptyList(),
)
