package com.calvintech.churchfinance.administration.api

import jakarta.validation.constraints.NotBlank

data class CreateChurchRequest(
    @field:NotBlank
    val name: String,
)
