package com.calvintech.churchfinance.administration.persistence

import com.calvintech.churchfinance.administration.domain.TransactionCategory
import com.github.f4b6a3.ulid.Ulid
import org.springframework.stereotype.Component

@Component
class TransactionCategoryMapper {
    fun toDomain(entity: TransactionCategoryJpaEntity): TransactionCategory =
        TransactionCategory(
            id = Ulid.from(entity.id),
            churchId = Ulid.from(entity.churchId),
            createdAt = entity.createdAt,
            name = entity.name,
            addedBy = Ulid.from(entity.addedBy),
            transactionType = entity.transactionType,
            version = entity.version,
        )

    fun toJpaEntity(category: TransactionCategory): TransactionCategoryJpaEntity =
        TransactionCategoryJpaEntity(
            id = category.id.toUuid(),
            churchId = category.churchId.toUuid(),
            createdAt = category.createdAt,
            addedBy = category.addedBy.toUuid(),
            name = category.name,
            transactionType = category.transactionType,
            version = category.version,
        )
}
