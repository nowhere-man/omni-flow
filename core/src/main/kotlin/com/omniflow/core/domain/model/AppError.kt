package com.omniflow.core.domain.model

data class AppError(
    val code: String,
    val message: String,
    val recoverable: Boolean,
)
