package com.omniflow.core.domain.facade

import com.omniflow.core.domain.model.HomeQuery
import com.omniflow.core.domain.model.HomeState
import com.omniflow.core.domain.model.TransactionDetailQuery
import com.omniflow.core.domain.model.TransactionDetailState
import kotlinx.coroutines.flow.Flow

interface HomeFacade {
    fun observeHome(query: HomeQuery): Flow<Result<HomeState>>
    fun observeTransactionDetails(query: TransactionDetailQuery): Flow<Result<TransactionDetailState>>
}
