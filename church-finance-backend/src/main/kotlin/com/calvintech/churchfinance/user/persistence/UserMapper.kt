package com.calvintech.churchfinance.user.persistence

import com.calvintech.churchfinance.user.domain.User
import com.github.f4b6a3.ulid.Ulid
import org.springframework.stereotype.Component

@Component
class UserMapper {
    fun toDomain(entity: UserJpaEntity): User =
        User(
            id = Ulid.from(entity.id),
            createdAt = entity.createdAt,
            addedBy = Ulid.from(entity.addedBy),
            email = entity.email,
            firstName = entity.firstName,
            lastName = entity.lastName,
            userStatus = entity.userStatus,
            isSystemAdmin = entity.isSystemAdmin,
        )

    fun toJpaEntity(user: User): UserJpaEntity =
        UserJpaEntity(
            id = user.id.toUuid(),
            createdAt = user.createdAt,
            addedBy = user.addedBy.toUuid(),
            email = user.email,
            firstName = user.firstName,
            lastName = user.lastName,
            userStatus = user.userStatus,
            isSystemAdmin = user.isSystemAdmin,
        )
}
