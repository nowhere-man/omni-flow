package com.omniflow.core.domain.facade

import com.omniflow.core.domain.model.AnalyticsDashboardState
import com.omniflow.core.domain.model.AnalyticsQuery
import com.omniflow.core.domain.model.ChartData
import com.omniflow.core.domain.model.DateRange
import com.omniflow.core.domain.model.LedgerScope
import com.omniflow.core.domain.model.StatementTable
import com.omniflow.core.domain.model.TimeGranularity
import kotlinx.coroutines.flow.Flow

interface AnalyticsFacade {
    fun observeDashboard(query: AnalyticsQuery): Flow<Result<AnalyticsDashboardState>>
    suspend fun statementTable(scope: LedgerScope, year: Int): Result<StatementTable>
    suspend fun trend(
        scope: LedgerScope,
        range: DateRange,
        granularity: TimeGranularity,
    ): Result<ChartData>
}
