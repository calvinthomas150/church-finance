package com.calvintech.churchfinance.user.persistence

import com.calvintech.churchfinance.user.domain.UserStatus
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.Version
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "users", schema = "church_finance")
class UserJpaEntity(
    @Id
    var id: UUID,
    var createdAt: Instant,
    var addedBy: UUID,
    var email: String,
    var firstName: String?,
    var lastName: String?,
    @Enumerated(EnumType.STRING)
    var userStatus: UserStatus,
    var isSystemAdmin: Boolean,
    @Version
    var version: Long = 0,
)
