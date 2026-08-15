package com.omniflow.core.domain.usecase

import com.omniflow.core.domain.model.Category
import com.omniflow.core.domain.model.CategoryId
import com.omniflow.core.domain.model.LedgerId
import com.omniflow.core.domain.model.TransactionType
import com.omniflow.core.domain.repository.CategoryRepository

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

class ReorderPrimaryCategoriesUseCase(
    private val categories: CategoryRepository,
) {
    suspend operator fun invoke(
        ledgerId: LedgerId,
        type: TransactionType,
        categoryIds: List<CategoryId>,
    ): Result<Unit> = runCatching {
        categories.reorderPrimary(ledgerId, type, categoryIds)
    }
}
