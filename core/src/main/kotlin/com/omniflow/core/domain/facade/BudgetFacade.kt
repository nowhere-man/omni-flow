package com.omniflow.core.domain.facade

import com.omniflow.core.domain.model.Budget
import com.omniflow.core.domain.model.BudgetId
import com.omniflow.core.domain.model.CategoryId
import com.omniflow.core.domain.model.LedgerId
import com.omniflow.core.domain.model.Money
import kotlinx.coroutines.flow.Flow

interface BudgetFacade {
    fun observe(ledgerId: LedgerId): Flow<Result<List<Budget>>>

    /** 同一个账本下「总预算」和每个一级分类各自最多一条，重复保存视为更新。 */
    suspend fun save(id: BudgetId?, ledgerId: LedgerId, categoryId: CategoryId?, amount: Money): Result<Unit>
    suspend fun delete(id: BudgetId): Result<Unit>
}
