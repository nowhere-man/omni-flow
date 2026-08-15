package com.omniflow.core.domain.repository

interface InitialDataRepository {
    suspend fun seedIfNeeded()
}
