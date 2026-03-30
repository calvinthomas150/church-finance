package com.calvintech.churchfinance.administration.persistence

import com.calvintech.churchfinance.shared.domain.FinancialTransactionType
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.Version
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "transaction_category", schema = "church_finance")
class TransactionCategoryJpaEntity(
    @Id
    var id: UUID,
    var churchId: UUID,
    var createdAt: Instant,
    var addedBy: UUID,
    var name: String,
    @Enumerated(EnumType.STRING)
    var transactionType: FinancialTransactionType,
    @Version
    var version: Long = 0,
)
