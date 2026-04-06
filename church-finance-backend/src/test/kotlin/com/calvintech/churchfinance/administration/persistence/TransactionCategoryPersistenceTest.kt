package com.calvintech.churchfinance.administration.persistence

import com.calvintech.churchfinance.TestcontainersConfiguration
import com.calvintech.churchfinance.administration.domain.ChurchStatus
import com.calvintech.churchfinance.administration.domain.TransactionCategory
import com.calvintech.churchfinance.shared.domain.FinancialTransactionType
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
class TransactionCategoryPersistenceTest {
    @Autowired
    lateinit var transactionCategoryRepository: TransactionCategoryRepository

    @Autowired
    lateinit var transactionCategoryMapper: TransactionCategoryMapper

    @Autowired
    lateinit var churchRepository: ChurchRepository

    @Autowired
    lateinit var entityManager: EntityManager

    @Test
    @Transactional
    fun `Should be able to persist and retrieve a transaction category`() {
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

        val category =
            TransactionCategory(
                id = UlidCreator.getUlid(),
                churchId = Ulid.from(church.id),
                createdAt = now,
                addedBy = UlidCreator.getUlid(),
                name = "Offerings",
                transactionType = FinancialTransactionType.INCOME,
            )

        val categoryEntity = transactionCategoryMapper.toJpaEntity(category)
        transactionCategoryRepository.save(categoryEntity)
        entityManager.flush()
        entityManager.clear()

        val retrievedEntity = transactionCategoryRepository.findById(categoryEntity.id).get()
        val retrievedCategory = transactionCategoryMapper.toDomain(retrievedEntity)

        assertEquals(category, retrievedCategory)
    }
}
