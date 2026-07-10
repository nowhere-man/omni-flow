package com.omniflow.shared.domain.facade

import com.omniflow.shared.domain.model.ImportCategoryBatchEdit
import com.omniflow.shared.domain.model.ImportCommitResult
import com.omniflow.shared.domain.model.ImportExcludeBatchEdit
import com.omniflow.shared.domain.model.ImportPreviewEdit
import com.omniflow.shared.domain.model.ImportPreviewState
import com.omniflow.shared.domain.model.ImportRequest
import com.omniflow.shared.domain.model.ImportSessionId
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
    suspend fun commit(sessionId: ImportSessionId): Result<ImportCommitResult>
}
