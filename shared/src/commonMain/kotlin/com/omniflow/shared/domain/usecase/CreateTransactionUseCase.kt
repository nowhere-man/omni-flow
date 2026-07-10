package com.omniflow.shared.domain.usecase

import com.omniflow.shared.domain.model.AccountId
import com.omniflow.shared.domain.model.CategoryId
import com.omniflow.shared.domain.model.LedgerId
import com.omniflow.shared.domain.model.Money
import com.omniflow.shared.domain.model.Transaction
import com.omniflow.shared.domain.model.TransactionId
import com.omniflow.shared.domain.model.TransactionSource
import com.omniflow.shared.domain.model.TransactionType
import com.omniflow.shared.domain.model.TagId
import com.omniflow.shared.domain.repository.TransactionRepository
import kotlinx.datetime.Instant

data class CreateTransactionCommand(
    val id: TransactionId,
    val ledgerId: LedgerId?,
    val accountId: AccountId?,
    val categoryId: CategoryId?,
    val amount: Money,
    val type: TransactionType,
    val occurredAt: Instant,
    val note: String?,
    val isExcluded: Boolean,
    val source: TransactionSource = TransactionSource.MANUAL,
    val externalId: String? = null,
    val tagIds: Set<TagId> = emptySet(),
)

class CreateTransactionUseCase(
    private val transactions: TransactionRepository,
) {
    suspend operator fun invoke(command: CreateTransactionCommand): Result<Unit> = runCatching {
        require(command.ledgerId != null) { "请选择账本" }
        require(command.accountId != null) { "请选择账户" }
        require(command.categoryId != null) { "请选择分类" }
        require(command.amount > Money.Zero) { "金额必须大于零" }

        transactions.create(
            Transaction(
                id = command.id,
                ledgerId = command.ledgerId,
                accountId = command.accountId,
                categoryId = command.categoryId,
                amount = command.amount,
                type = command.type,
                occurredAt = command.occurredAt,
                note = command.note?.trim()?.takeIf(String::isNotEmpty),
                isExcluded = command.isExcluded,
                source = command.source,
                externalId = command.externalId?.trim()?.takeIf(String::isNotEmpty),
                tagIds = command.tagIds,
            )
        )
    }
}
