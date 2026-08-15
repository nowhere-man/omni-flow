package com.omniflow.core.domain.facade

import com.omniflow.core.domain.model.ImportCategoryBatchEdit
import com.omniflow.core.domain.model.ImportCommitResult
import com.omniflow.core.domain.model.ImportExcludeBatchEdit
import com.omniflow.core.domain.model.ImportPreviewEdit
import com.omniflow.core.domain.model.ImportPreviewState
import com.omniflow.core.domain.model.ImportRequest
import com.omniflow.core.domain.model.ImportSessionId
import kotlinx.coroutines.flow.Flow

interface ImportWorkflow {
    fun preview(request: ImportRequest): Flow<Result<ImportPreviewState>>
    fun observe(sessionId: ImportSessionId): Flow<Result<ImportPreviewState>>
    suspend fun editItem(edit: ImportPreviewEdit): Result<ImportPreviewState>
    suspend fun editCategories(
        sessionId: ImportSessionId,
        edit: ImportCategoryBatchEdit,
    ): Result<ImportPreviewState>
    suspend fun editSkipped(
        sessionId: ImportSessionId,
        edit: ImportExcludeBatchEdit,
    ): Result<ImportPreviewState>
    suspend fun cancel(sessionId: ImportSessionId): Result<Unit>
    suspend fun commit(sessionId: ImportSessionId): Result<ImportCommitResult>
}
