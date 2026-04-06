package com.calvintech.churchfinance.administration.api

import com.calvintech.churchfinance.shared.domain.FinancialTransactionType
import jakarta.validation.constraints.NotBlank

data class CreateTransactionCategoryRequest(
    @field:NotBlank
    val name: String,
    val type: FinancialTransactionType,
)
