package com.calvintech.churchfinance.administration.api

import com.calvintech.churchfinance.administration.domain.ChurchStatus
import java.util.UUID

data class ChurchResponse(
    val id: UUID,
    val name: String,
    val status: ChurchStatus,
    val version: Long,
)
