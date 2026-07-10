package com.omniflow.shared.domain.repository

interface InitialDataRepository {
    suspend fun seedIfNeeded()
}
