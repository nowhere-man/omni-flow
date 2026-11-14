package com.omniflow.core.domain.model

data class BatchOperationResult(
    val totalCount: Int,
    val processedCount: Int,
    val skippedCount: Int,
) {
    val isAllProcessed: Boolean get() = skippedCount == 0 && processedCount == totalCount
}
