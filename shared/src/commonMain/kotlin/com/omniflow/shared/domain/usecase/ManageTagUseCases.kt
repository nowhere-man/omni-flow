package com.omniflow.shared.domain.usecase

import com.omniflow.shared.domain.model.Tag
import com.omniflow.shared.domain.model.TagId
import com.omniflow.shared.domain.repository.TagRepository

class CreateTagUseCase(
    private val tags: TagRepository,
) {
    suspend operator fun invoke(tag: Tag): Result<Unit> = runCatching {
        tags.create(tag)
    }
}

class UpdateTagUseCase(
    private val tags: TagRepository,
) {
    suspend operator fun invoke(tag: Tag): Result<Unit> = runCatching {
        tags.update(tag)
    }
}

class DeleteTagUseCase(
    private val tags: TagRepository,
) {
    suspend operator fun invoke(tagId: TagId): Result<Unit> = runCatching {
        tags.archive(tagId)
    }
}
