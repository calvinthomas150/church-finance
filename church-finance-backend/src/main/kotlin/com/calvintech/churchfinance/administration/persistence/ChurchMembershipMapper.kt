package com.calvintech.churchfinance.administration.persistence

import com.calvintech.churchfinance.administration.domain.ChurchMembership
import com.github.f4b6a3.ulid.Ulid
import org.springframework.stereotype.Component

@Component
class ChurchMembershipMapper {
    fun toDomain(entity: ChurchMembershipJpaEntity): ChurchMembership =
        ChurchMembership(
            id = Ulid.from(entity.id),
            churchId = Ulid.from(entity.churchId),
            userId = Ulid.from(entity.userId),
            addedBy = Ulid.from(entity.addedBy),
            createdAt = entity.createdAt,
            roles = entity.roles,
            version = entity.version,
        )

    fun toJpaEntity(membership: ChurchMembership): ChurchMembershipJpaEntity =
        ChurchMembershipJpaEntity(
            id = membership.id.toUuid(),
            churchId = membership.churchId.toUuid(),
            userId = membership.userId.toUuid(),
            addedBy = membership.addedBy.toUuid(),
            createdAt = membership.createdAt,
            roles = membership.roles.toMutableSet(),
            version = membership.version,
        )
}
