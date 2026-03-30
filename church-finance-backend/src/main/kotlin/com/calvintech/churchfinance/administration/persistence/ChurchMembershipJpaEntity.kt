package com.calvintech.churchfinance.administration.persistence

import com.calvintech.churchfinance.administration.domain.ChurchRole
import jakarta.persistence.CollectionTable
import jakarta.persistence.Column
import jakarta.persistence.ElementCollection
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.Table
import jakarta.persistence.Version
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "church_membership", schema = "church_finance")
class ChurchMembershipJpaEntity(
    @Id
    var id: UUID,
    var churchId: UUID,
    var userId: UUID,
    var addedBy: UUID,
    var createdAt: Instant,
    @ElementCollection(fetch = FetchType.EAGER)
    @Enumerated(EnumType.STRING)
    @CollectionTable(
        name = "church_membership_roles",
        schema = "church_finance",
        joinColumns = [JoinColumn(name = "church_membership_id")],
    )
    @Column(name = "role")
    var roles: MutableSet<ChurchRole>,
    @Version
    var version: Long = 0,
)
