package com.calvintech.churchfinance.administration.service

import com.calvintech.churchfinance.administration.api.CreateTransactionCategoryRequest
import com.calvintech.churchfinance.administration.api.TransactionCategoryResponse
import com.calvintech.churchfinance.administration.domain.TransactionCategory
import com.calvintech.churchfinance.administration.persistence.TransactionCategoryMapper
import com.calvintech.churchfinance.administration.persistence.TransactionCategoryRepository
import com.calvintech.churchfinance.shared.service.CurrentUserProvider
import com.github.f4b6a3.ulid.Ulid
import com.github.f4b6a3.ulid.UlidCreator
import org.springframework.stereotype.Service
import java.time.Instant
import java.util.UUID

@Service
class TransactionCategoryService(
    private val mapper: TransactionCategoryMapper,
    private val repository: TransactionCategoryRepository,
    private val userProvider: CurrentUserProvider,
) {
    fun create(
        churchId: UUID,
        createTransactionCategoryRequest: CreateTransactionCategoryRequest,
    ): TransactionCategoryResponse {
        val transactionCategory =
            TransactionCategory(
                id = UlidCreator.getUlid(),
                churchId = Ulid.from(churchId),
                createdAt = Instant.now(),
                addedBy = userProvider.getCurrentUserId(),
                name = createTransactionCategoryRequest.name,
                transactionType = createTransactionCategoryRequest.type,
            )

        return saveAndRespond(transactionCategory)
    }

    private fun saveAndRespond(transactionCategory: TransactionCategory): TransactionCategoryResponse {
        val persisted = repository.save(mapper.toJpaEntity(transactionCategory))
        return toResponse(mapper.toDomain(persisted))
    }

    private fun toResponse(transactionCategory: TransactionCategory): TransactionCategoryResponse =
        TransactionCategoryResponse(
            id = transactionCategory.id.toUuid(),
            name = transactionCategory.name,
            type = transactionCategory.transactionType,
            version = transactionCategory.version,
        )
}
