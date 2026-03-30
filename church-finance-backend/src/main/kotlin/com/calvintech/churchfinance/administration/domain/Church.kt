package com.calvintech.churchfinance.administration.domain

import com.github.f4b6a3.ulid.Ulid
import java.time.Instant

data class Church(
    val id: Ulid,
    val createdAt: Instant,
    val addedBy: Ulid,
    val name: String,
    val status: ChurchStatus,
) {
    init {
        require(name.isNotBlank()) {
            "Church name must not be blank"
        }
    }
}
