package com.calvintech.churchfinance.financialtransaction.domain

import com.github.f4b6a3.ulid.Ulid
import java.math.BigDecimal

data class FinancialTransactionCategorisation(
    val id: Ulid,
    val amount: BigDecimal,
    val categoryId: Ulid?,
    val description: String?
)
