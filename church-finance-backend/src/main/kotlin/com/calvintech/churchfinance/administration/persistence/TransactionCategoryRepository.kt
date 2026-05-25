package com.calvintech.churchfinance.administration.persistence

import com.calvintech.churchfinance.shared.domain.FinancialTransactionType
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface TransactionCategoryRepository : JpaRepository<TransactionCategoryJpaEntity, UUID> {
    fun findByChurchIdAndNameAndTransactionType(
        churchId: UUID,
        name: String,
        transactionType: FinancialTransactionType,
    ): TransactionCategoryJpaEntity?

    fun findAllByChurchId(churchId: UUID): List<TransactionCategoryJpaEntity>
}
