package com.omniflow.core.domain.facade

import com.omniflow.core.domain.model.AccountId
import com.omniflow.core.domain.model.AccountRole
import com.omniflow.core.domain.model.Money
import com.omniflow.core.domain.model.PeriodReconciliationSnapshot
import com.omniflow.core.domain.model.ReconciliationAnnualReport
import kotlinx.coroutines.flow.Flow

interface ReconciliationFacade {
    fun observeAnnualReport(year: Int, role: AccountRole?): Flow<Result<ReconciliationAnnualReport>>
    fun observePeriodSnapshot(period: String, role: AccountRole?): Flow<Result<PeriodReconciliationSnapshot?>>
    fun observeAllPeriods(): Flow<List<String>>

    suspend fun recordAccountUpdate(
        accountId: AccountId,
        period: String,
        balance: Money,
        availableLimit: Money?,
        profitLoss: Money,
    ): Result<Unit>

    suspend fun updateMonthlyMeta(
        period: String,
        husbandIncome: Money,
        wifeIncome: Money,
        notes: String,
    ): Result<Unit>

    suspend fun updateMonthlyNotes(
        period: String,
        notes: String,
    ): Result<Unit>
}
