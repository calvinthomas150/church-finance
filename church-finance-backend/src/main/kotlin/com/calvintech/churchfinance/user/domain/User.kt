package com.calvintech.churchfinance.user.domain

import com.github.f4b6a3.ulid.Ulid
import java.time.Instant

data class User(
    val id: Ulid,
    val createdAt: Instant,
    val addedBy: Ulid,
    val email: String,
    val firstName: String?,
    val lastName: String?,
    val userStatus: UserStatus,
    val isSystemAdmin: Boolean,
) {
    init {
        firstName?.let {
            require(it.isNotBlank()) {
                "First name must not be blank"
            }
        }

        lastName?.let {
            require(it.isNotBlank()) {
                "Last name must not be blank"
            }
        }

        require(email.matches(EMAIL_REGEX)) {
            "Email must be a valid email address"
        }
    }

    companion object {
        private val EMAIL_REGEX = Regex("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")
    }
}
