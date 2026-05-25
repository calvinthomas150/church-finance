package com.calvintech.churchfinance.administration.service

import com.calvintech.churchfinance.administration.api.CreateTransactionCategoryRequest
import com.calvintech.churchfinance.administration.api.TransactionCategoryResponse
import com.calvintech.churchfinance.administration.api.TransactionCategoryStatusFilter
import com.calvintech.churchfinance.administration.api.UpdateTransactionCategoryRequest
import com.calvintech.churchfinance.administration.domain.ChurchNotFoundException
import com.calvintech.churchfinance.administration.domain.TransactionCategory
import com.calvintech.churchfinance.administration.domain.TransactionCategoryNameConflictException
import com.calvintech.churchfinance.administration.domain.TransactionCategoryNotFoundException
import com.calvintech.churchfinance.administration.domain.TransactionCategoryStatus
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

    fun get(
        churchId: UUID,
        id: UUID,
    ): TransactionCategoryResponse = toResponse(findCategoryScopedToChurch(churchId, id))

    fun list(
        churchId: UUID,
        status: TransactionCategoryStatusFilter,
        type: FinancialTransactionType?,
    ): List<TransactionCategoryResponse> {
        requireChurchExists(churchId)
        return repository
            .findAllByChurchId(churchId)
            .map(mapper::toDomain)
            .filter { status.matches(it.status) }
            .filter { type == null || it.transactionType == type }
            .sortedBy { it.name }
            .map(::toResponse)
    }

    fun update(
        churchId: UUID,
        id: UUID,
        updateTransactionCategoryRequest: UpdateTransactionCategoryRequest,
    ): TransactionCategoryResponse {
        val existing = findCategoryScopedToChurch(churchId, id)
        if (existing.name != updateTransactionCategoryRequest.name) {
            requireNameAvailable(
                churchId = churchId,
                name = updateTransactionCategoryRequest.name,
                type = existing.transactionType,
                excludingId = id,
            )
        }
        val updated = existing.copy(name = updateTransactionCategoryRequest.name, version = updateTransactionCategoryRequest.version)
        return saveAndRespond(updated)
    }

    fun deactivate(
        churchId: UUID,
        id: UUID,
        version: Long,
    ): TransactionCategoryResponse {
        val existing = findCategoryScopedToChurch(churchId, id)
        val updated = existing.copy(status = TransactionCategoryStatus.INACTIVE, version = version)
        return saveAndRespond(updated)
    }

    fun activate(
        churchId: UUID,
        id: UUID,
        version: Long,
    ): TransactionCategoryResponse {
        val existing = findCategoryScopedToChurch(churchId, id)
        val updated = existing.copy(status = TransactionCategoryStatus.ACTIVE, version = version)
        return saveAndRespond(updated)
    }

    private fun findCategoryScopedToChurch(
        churchId: UUID,
        id: UUID,
    ): TransactionCategory {
        val entity = repository.findById(id).orElseThrow { TransactionCategoryNotFoundException(id) }
        val domain = mapper.toDomain(entity)
        if (domain.churchId.toUuid() != churchId) {
            throw TransactionCategoryNotFoundException(id)
        }
        return domain
    }

    private fun requireChurchExists(churchId: UUID) {
        if (!churchRepository.existsById(churchId)) {
            throw ChurchNotFoundException(churchId)
        }
    }

    // `excludingId` lets the update flow ignore the row being updated when it queries by the new name.
    // Today the caller (update) only invokes this helper when the name actually changes, so the query
    // cannot return the row being updated — but keeping the parameter preserves the rule's semantics
    // and the DB constraint remains the safety net for the TOCTOU race regardless.
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
