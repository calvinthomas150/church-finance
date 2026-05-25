package com.calvintech.churchfinance.administration.service

import com.calvintech.churchfinance.administration.api.CreateTransactionCategoryRequest
import com.calvintech.churchfinance.administration.api.TransactionCategoryResponse
import com.calvintech.churchfinance.administration.domain.ChurchNotFoundException
import com.calvintech.churchfinance.administration.domain.TransactionCategory
import com.calvintech.churchfinance.administration.domain.TransactionCategoryNameConflictException
import com.calvintech.churchfinance.administration.persistence.ChurchRepository
import com.calvintech.churchfinance.administration.persistence.TransactionCategoryMapper
import com.calvintech.churchfinance.administration.persistence.TransactionCategoryRepository
import com.calvintech.churchfinance.shared.domain.FinancialTransactionType
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
    private val churchRepository: ChurchRepository,
) {
    fun create(
        churchId: UUID,
        createTransactionCategoryRequest: CreateTransactionCategoryRequest,
    ): TransactionCategoryResponse {
        requireChurchExists(churchId)
        requireNameAvailable(
            churchId = churchId,
            name = createTransactionCategoryRequest.name,
            type = createTransactionCategoryRequest.type,
            excludingId = null,
        )

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

    private fun requireChurchExists(churchId: UUID) {
        if (!churchRepository.existsById(churchId)) {
            throw ChurchNotFoundException(churchId)
        }
    }

    private fun requireNameAvailable(
        churchId: UUID,
        name: String,
        type: FinancialTransactionType,
        excludingId: UUID?,
    ) {
        val conflict =
            repository.findByChurchIdAndNameAndTransactionType(
                churchId = churchId,
                name = name,
                transactionType = type,
            )
        if (conflict != null && conflict.id != excludingId) {
            throw TransactionCategoryNameConflictException(churchId, name, type)
        }
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
            status = transactionCategory.status,
            version = transactionCategory.version,
        )
}
