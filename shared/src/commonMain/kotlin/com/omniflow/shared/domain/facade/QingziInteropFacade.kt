package com.omniflow.shared.domain.facade

import com.omniflow.shared.domain.model.QingziExportRequest
import com.omniflow.shared.domain.model.QingziExportResult

interface QingziInteropFacade {
    suspend fun export(request: QingziExportRequest = QingziExportRequest()): Result<QingziExportResult>
}
