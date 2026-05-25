package com.calvintech.churchfinance.administration.persistence

import com.calvintech.churchfinance.TestcontainersConfiguration
import com.calvintech.churchfinance.administration.domain.ChurchStatus
import com.calvintech.churchfinance.administration.domain.TransactionCategory
import com.calvintech.churchfinance.administration.domain.TransactionCategoryStatus
import com.calvintech.churchfinance.shared.domain.FinancialTransactionType
import com.github.f4b6a3.ulid.Ulid
import com.github.f4b6a3.ulid.UlidCreator
import jakarta.persistence.EntityManager
import jakarta.transaction.Transactional
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.testcontainers.context.ImportTestcontainers
import org.springframework.dao.DataIntegrityViolationException
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

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
        assertEquals(TransactionCategoryStatus.ACTIVE, retrievedCategory.status)
    }

    @Test
    fun `status field round-trips through JPA`() {
        val churchId = persistChurch()
        val entity =
            buildJpaEntity(churchId = churchId, name = "Offerings", type = FinancialTransactionType.INCOME)
                .also { it.status = TransactionCategoryStatus.ACTIVE }

        transactionCategoryRepository.saveAndFlush(entity)
        val loaded = transactionCategoryRepository.findById(entity.id).orElseThrow()
        assertEquals(TransactionCategoryStatus.ACTIVE, loaded.status)

        loaded.status = TransactionCategoryStatus.INACTIVE
        transactionCategoryRepository.saveAndFlush(loaded)
        val reloaded = transactionCategoryRepository.findById(entity.id).orElseThrow()
        assertEquals(TransactionCategoryStatus.INACTIVE, reloaded.status)
    }

    @Test
    fun `unique constraint allows same name with different transaction_type`() {
        val churchId = persistChurch()
        val income = buildJpaEntity(churchId = churchId, name = "Tithes", type = FinancialTransactionType.INCOME)
        val expense = buildJpaEntity(churchId = churchId, name = "Tithes", type = FinancialTransactionType.EXPENDITURE)

        transactionCategoryRepository.saveAndFlush(income)
        transactionCategoryRepository.saveAndFlush(expense)

        val all = transactionCategoryRepository.findAll().filter { it.churchId == churchId }
        assertEquals(2, all.size)
    }

    @Test
    fun `unique constraint allows same name in different churches`() {
        val churchOne = persistChurch(name = "Church One")
        val churchTwo = persistChurch(name = "Church Two")
        transactionCategoryRepository.saveAndFlush(
            buildJpaEntity(churchId = churchOne, name = "Tithes", type = FinancialTransactionType.INCOME),
        )
        transactionCategoryRepository.saveAndFlush(
            buildJpaEntity(churchId = churchTwo, name = "Tithes", type = FinancialTransactionType.INCOME),
        )

        val all =
            transactionCategoryRepository
                .findAll()
                .filter { it.churchId == churchOne || it.churchId == churchTwo }
        assertEquals(2, all.count { it.name == "Tithes" })
    }

    @Test
    fun `unique constraint rejects duplicate church_id, name, transaction_type`() {
        val churchId = persistChurch()
        transactionCategoryRepository.saveAndFlush(
            buildJpaEntity(churchId = churchId, name = "Tithes", type = FinancialTransactionType.INCOME),
        )

        assertFailsWith<DataIntegrityViolationException> {
            transactionCategoryRepository.saveAndFlush(
                buildJpaEntity(churchId = churchId, name = "Tithes", type = FinancialTransactionType.INCOME),
            )
        }
    }

    private fun persistChurch(name: String = "Test Church"): UUID {
        val church =
            ChurchJpaEntity(
                id = UUID.randomUUID(),
                createdAt = Instant.now(),
                addedBy = UUID.randomUUID(),
                name = name,
                status = ChurchStatus.ACTIVE,
            )
        churchRepository.saveAndFlush(church)
        return church.id
    }

    private fun buildJpaEntity(
        churchId: UUID,
        name: String,
        type: FinancialTransactionType,
    ): TransactionCategoryJpaEntity =
        TransactionCategoryJpaEntity(
            id = UUID.randomUUID(),
            churchId = churchId,
            createdAt = Instant.now(),
            addedBy = UUID.randomUUID(),
            name = name,
            transactionType = type,
        )
}
