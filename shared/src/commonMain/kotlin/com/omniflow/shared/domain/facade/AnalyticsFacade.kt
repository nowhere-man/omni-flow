package com.omniflow.shared.domain.facade

import com.omniflow.shared.domain.model.AnalyticsDashboardState
import com.omniflow.shared.domain.model.AnalyticsQuery
import com.omniflow.shared.domain.model.ChartData
import com.omniflow.shared.domain.model.DateRange
import com.omniflow.shared.domain.model.LedgerScope
import com.omniflow.shared.domain.model.StatementTable
import com.omniflow.shared.domain.model.TimeGranularity
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
