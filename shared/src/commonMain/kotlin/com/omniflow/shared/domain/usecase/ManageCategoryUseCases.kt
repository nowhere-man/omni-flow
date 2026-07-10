package com.omniflow.shared.domain.usecase

import com.omniflow.shared.domain.model.Category
import com.omniflow.shared.domain.model.CategoryId
import com.omniflow.shared.domain.repository.CategoryRepository

class CreateCategoryUseCase(
    private val categories: CategoryRepository,
) {
    suspend operator fun invoke(category: Category): Result<Unit> = runCatching {
        categories.create(category)
    }
}

class UpdateCategoryUseCase(
    private val categories: CategoryRepository,
) {
    suspend operator fun invoke(category: Category): Result<Unit> = runCatching {
        categories.update(category)
    }
}

class DeleteCategoryUseCase(
    private val categories: CategoryRepository,
) {
    suspend operator fun invoke(categoryId: CategoryId): Result<Unit> = runCatching {
        categories.archive(categoryId)
    }
}
