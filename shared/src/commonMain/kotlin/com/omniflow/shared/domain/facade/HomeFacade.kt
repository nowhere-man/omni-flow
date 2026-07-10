package com.omniflow.shared.domain.facade

import com.omniflow.shared.domain.model.HomeQuery
import com.omniflow.shared.domain.model.HomeState
import com.omniflow.shared.domain.model.TransactionDetailQuery
import com.omniflow.shared.domain.model.TransactionDetailState
import kotlinx.coroutines.flow.Flow

interface HomeFacade {
    fun observeHome(query: HomeQuery): Flow<Result<HomeState>>
    fun observeTransactionDetails(query: TransactionDetailQuery): Flow<Result<TransactionDetailState>>
}
