package com.calvintech.churchfinance.administration.domain

import com.github.f4b6a3.ulid.Ulid
import java.time.LocalDateTime

data class ChurchMembership(
    val id: Ulid,
    val churchId: Ulid,
    val userId: Ulid,
    val addedBy: Ulid,
    val createdAt: LocalDateTime,
    val roles: Set<ChurchRole>,
) {
    init {
        require(roles.isNotEmpty()) {
            "At least one role must be specified."
        }
    }
}
