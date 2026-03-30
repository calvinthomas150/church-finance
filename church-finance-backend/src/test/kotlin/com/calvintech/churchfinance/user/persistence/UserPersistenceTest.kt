package com.calvintech.churchfinance.user.persistence

import com.calvintech.churchfinance.TestcontainersConfiguration
import com.calvintech.churchfinance.user.domain.User
import com.calvintech.churchfinance.user.domain.UserStatus
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
class UserPersistenceTest {
    @Autowired
    lateinit var userRepository: UserRepository

    @Autowired
    lateinit var userMapper: UserMapper

    @Autowired
    lateinit var entityManager: EntityManager

    @Test
    @Transactional
    fun `Should be able to persist and retrieve a user`() {
        val user =
            User(
                id = UlidCreator.getUlid(),
                createdAt = Instant.now().truncatedTo(ChronoUnit.MICROS),
                addedBy = UlidCreator.getUlid(),
                email = "test@test.com",
                firstName = "FirstName",
                lastName = "LastName",
                userStatus = UserStatus.ACTIVE,
                isSystemAdmin = false,
            )

        val userEntity = userMapper.toJpaEntity(user)
        userRepository.save(userEntity)
        entityManager.flush()
        entityManager.clear()

        val retrievedUserEntity = userRepository.findById(userEntity.id).get()
        val retrievedUser = userMapper.toDomain(retrievedUserEntity)

        assertEquals(user, retrievedUser)
    }
}
