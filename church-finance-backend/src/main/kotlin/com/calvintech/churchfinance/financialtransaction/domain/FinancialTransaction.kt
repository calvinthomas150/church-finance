package com.calvintech.churchfinance.financialtransaction.domain

import com.calvintech.churchfinance.shared.domain.FinancialTransactionType
import com.github.f4b6a3.ulid.Ulid
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.util.Currency

data class FinancialTransaction(
    val id: Ulid,
    val churchId: Ulid,
    val createdAt: Instant,
    val addedBy: Ulid,
    val amount: BigDecimal,
    val currency: Currency,
    val financialTransactionType: FinancialTransactionType,
    val transactionDate: LocalDate,
    val bankAccountId: Ulid?,
    val bankReference: String?,
    val description: String?,
    val categorisations: List<FinancialTransactionCategorisation>,
) {
    init {
        require(amount > BigDecimal.ZERO) {
            "Transaction amount must be greater than zero"
        }

        bankReference?.let {
            require(it.isNotBlank()) {
                "Bank reference must not be blank"
            }
        }

        description?.let {
            require(it.isNotBlank()) {
                "Description must not be blank"
            }
        }

        require(categorisations.isEmpty() || categorisations.sumOf { it.amount }.compareTo(amount) == 0) {
            "Sum of categorisation amounts must equal transaction amount"
        }
    }
}
