package com.calvintech.churchfinance.administration.persistence

import com.calvintech.churchfinance.administration.domain.ChurchStatus
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.Version
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "church", schema = "church_finance")
class ChurchJpaEntity(
    @Id
    var id: UUID,
    var createdAt: Instant,
    var addedBy: UUID,
    var name: String,
    @Enumerated(EnumType.STRING)
    var status: ChurchStatus,
    @Version
    var version: Long = 0,
)
