package com.calvintech.churchfinance.administration.api

import com.calvintech.churchfinance.shared.domain.FinancialTransactionType
import java.util.UUID

data class TransactionCategoryResponse(
    val id: UUID,
    val name: String,
    val type: FinancialTransactionType,
    val version: Long,
)
