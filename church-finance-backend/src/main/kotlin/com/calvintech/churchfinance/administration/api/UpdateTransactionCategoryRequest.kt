package com.calvintech.churchfinance.administration.api

import jakarta.validation.constraints.NotBlank

data class UpdateTransactionCategoryRequest(
    @field:NotBlank
    val name: String,
    val version: Long,
)
