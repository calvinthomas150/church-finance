package com.calvintech.churchfinance.administration.persistence

import com.calvintech.churchfinance.administration.domain.Church
import com.github.f4b6a3.ulid.Ulid
import org.springframework.stereotype.Component

@Component
class ChurchMapper {
    fun toDomain(entity: ChurchJpaEntity): Church =
        Church(
            id = Ulid.from(entity.id),
            createdAt = entity.createdAt,
            addedBy = Ulid.from(entity.addedBy),
            name = entity.name,
            status = entity.status,
        )

    fun toJpaEntity(church: Church): ChurchJpaEntity =
        ChurchJpaEntity(
            id = church.id.toUuid(),
            createdAt = church.createdAt,
            addedBy = church.addedBy.toUuid(),
            name = church.name,
            status = church.status,
        )
}
