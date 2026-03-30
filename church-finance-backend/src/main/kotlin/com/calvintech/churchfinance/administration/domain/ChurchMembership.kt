package com.calvintech.churchfinance.administration.domain

import com.github.f4b6a3.ulid.Ulid
import java.time.Instant

data class ChurchMembership(
    val id: Ulid,
    val churchId: Ulid,
    val userId: Ulid,
    val addedBy: Ulid,
    val createdAt: Instant,
    val roles: Set<ChurchRole>,
    val version: Long = 0,
) {
    init {
        require(roles.isNotEmpty()) {
            "At least one role must be specified."
        }
    }
}
