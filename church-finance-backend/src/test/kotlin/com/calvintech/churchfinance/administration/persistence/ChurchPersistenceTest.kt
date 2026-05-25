package com.calvintech.churchfinance.administration.persistence

import com.calvintech.churchfinance.TestcontainersConfiguration
import com.calvintech.churchfinance.administration.domain.Church
import com.calvintech.churchfinance.administration.domain.ChurchStatus
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
class ChurchPersistenceTest {
    @Autowired
    lateinit var churchRepository: ChurchRepository

    @Autowired
    lateinit var churchMapper: ChurchMapper

    @Autowired
    lateinit var entityManager: EntityManager

    @Test
    @Transactional
    fun `Should be able to persist and retrieve a church`() {
        val church =
            Church(
                id = UlidCreator.getUlid(),
                createdAt = Instant.now().truncatedTo(ChronoUnit.MICROS),
                addedBy = UlidCreator.getUlid(),
                name = "Our Saviour Lutheran Church",
                status = ChurchStatus.ACTIVE,
            )

        val churchEntity = churchMapper.toJpaEntity(church)
        churchRepository.save(churchEntity)
        entityManager.flush()
        entityManager.clear()

        val retrievedEntity = churchRepository.findById(churchEntity.id).get()
        val retrievedChurch = churchMapper.toDomain(retrievedEntity)

        assertEquals(church, retrievedChurch)
    }
}
