package com.calvintech.churchfinance.administration.domain

import com.calvintech.churchfinance.shared.domain.FinancialTransactionType
import com.github.f4b6a3.ulid.Ulid
import java.time.Instant

data class TransactionCategory(
    val id: Ulid,
    val churchId: Ulid,
    val createdAt: Instant,
    val addedBy: Ulid,
    val name: String,
    val transactionType: FinancialTransactionType,
) {
    init {
        require(name.isNotBlank()) {
            "Name must not be blank"
        }
    }
}
