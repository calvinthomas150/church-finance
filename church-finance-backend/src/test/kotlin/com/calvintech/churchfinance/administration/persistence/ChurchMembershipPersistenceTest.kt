package com.calvintech.churchfinance.administration.persistence

import com.calvintech.churchfinance.TestcontainersConfiguration
import com.calvintech.churchfinance.administration.domain.ChurchMembership
import com.calvintech.churchfinance.administration.domain.ChurchRole
import com.calvintech.churchfinance.administration.domain.ChurchStatus
import com.calvintech.churchfinance.user.domain.UserStatus
import com.calvintech.churchfinance.user.persistence.UserJpaEntity
import com.calvintech.churchfinance.user.persistence.UserRepository
import com.github.f4b6a3.ulid.Ulid
import com.github.f4b6a3.ulid.UlidCreator
import jakarta.persistence.EntityManager
import jakarta.transaction.Transactional
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.testcontainers.context.ImportTestcontainers
import java.time.Instant
import java.time.temporal.ChronoUnit
import kotlin.test.Test
import kotlin.test.assertEquals

@SpringBootTest
@ImportTestcontainers(TestcontainersConfiguration::class)
class ChurchMembershipPersistenceTest {
    @Autowired
    lateinit var churchMembershipRepository: ChurchMembershipRepository

    @Autowired
    lateinit var churchMembershipMapper: ChurchMembershipMapper

    @Autowired
    lateinit var churchRepository: ChurchRepository

    @Autowired
    lateinit var userRepository: UserRepository

    @Autowired
    lateinit var entityManager: EntityManager

    @Test
    @Transactional
    fun `Should be able to persist and retrieve a church membership`() {
        val now = Instant.now().truncatedTo(ChronoUnit.MICROS)

        val church =
            ChurchJpaEntity(
                id = UlidCreator.getUlid().toUuid(),
                createdAt = now,
                addedBy = UlidCreator.getUlid().toUuid(),
                name = "Our Saviour Lutheran Church",
                status = ChurchStatus.ACTIVE,
            )
        churchRepository.save(church)

        val user =
            UserJpaEntity(
                id = UlidCreator.getUlid().toUuid(),
                createdAt = now,
                addedBy = UlidCreator.getUlid().toUuid(),
                email = "test@test.com",
                firstName = "FirstName",
                lastName = "LastName",
                userStatus = UserStatus.ACTIVE,
                isSystemAdmin = false,
            )
        userRepository.save(user)

        val membership =
            ChurchMembership(
                id = UlidCreator.getUlid(),
                churchId = Ulid.from(church.id),
                userId = Ulid.from(user.id),
                addedBy = UlidCreator.getUlid(),
                createdAt = now,
                roles = setOf(ChurchRole.ADMIN, ChurchRole.TREASURER),
            )

        val membershipEntity = churchMembershipMapper.toJpaEntity(membership)
        churchMembershipRepository.save(membershipEntity)
        entityManager.flush()
        entityManager.clear()

        val retrievedEntity = churchMembershipRepository.findById(membershipEntity.id).get()
        val retrievedMembership = churchMembershipMapper.toDomain(retrievedEntity)

        // Hibernate bumps version to 1 on initial persist because @ElementCollection
        // triggers an additional UPDATE on the parent entity when managing the collection table rows
        assertEquals(membership.copy(version = 1), retrievedMembership)
    }
}
