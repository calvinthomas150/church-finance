package com.calvintech.churchfinance.financialtransaction.domain

import com.github.f4b6a3.ulid.Ulid
import java.math.BigDecimal

data class FinancialTransactionCategorisation(
    val id: Ulid,
    val amount: BigDecimal,
    val categoryId: Ulid?,
    val fundId: Ulid?,
    val description: String?,
) {
    init {
        require(amount > BigDecimal.ZERO) {
            "Amount must be positive"
        }

        description?.let {
            require(it.isNotBlank()) {
                "Description must not be blank"
            }
        }
    }
}
