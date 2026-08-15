package com.omniflow.core.domain.facade

import com.omniflow.core.domain.model.QingziExportRequest
import com.omniflow.core.domain.model.QingziExportResult

interface QingziInteropFacade {
    suspend fun export(request: QingziExportRequest = QingziExportRequest()): Result<QingziExportResult>
}
