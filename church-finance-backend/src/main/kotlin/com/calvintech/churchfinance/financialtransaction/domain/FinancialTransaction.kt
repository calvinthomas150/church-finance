package com.calvintech.churchfinance.financialtransaction.domain

import com.github.f4b6a3.ulid.Ulid
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.Currency

data class FinancialTransaction(
    val id: Ulid,
    val churchId: Ulid,
    val createdAt: LocalDateTime,
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
        require(categorisations.isEmpty() || categorisations.sumOf { it.amount }.compareTo(amount) == 0) {
            "Sum of categorisation amounts must equal transaction amount"
        }
    }
}
