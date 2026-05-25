package com.calvintech.churchfinance.administration.service

import com.calvintech.churchfinance.administration.api.CreateTransactionCategoryRequest
import com.calvintech.churchfinance.administration.domain.ChurchNotFoundException
import com.calvintech.churchfinance.administration.domain.TransactionCategory
import com.calvintech.churchfinance.administration.domain.TransactionCategoryNameConflictException
import com.calvintech.churchfinance.administration.domain.TransactionCategoryNotFoundException
import com.calvintech.churchfinance.administration.domain.TransactionCategoryStatus
import com.github.f4b6a3.ulid.Ulid
import com.calvintech.churchfinance.administration.persistence.ChurchRepository
import com.calvintech.churchfinance.administration.persistence.TransactionCategoryJpaEntity
import com.calvintech.churchfinance.administration.persistence.TransactionCategoryMapper
import com.calvintech.churchfinance.administration.persistence.TransactionCategoryRepository
import com.calvintech.churchfinance.shared.domain.FinancialTransactionType
import com.calvintech.churchfinance.shared.service.CurrentUserProvider
import com.github.f4b6a3.ulid.UlidCreator
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.slot
import org.junit.jupiter.api.Assertions.assertNotNull
import java.time.Instant
import java.util.UUID
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class TransactionCategoryTest {
    @MockK
    lateinit var repository: TransactionCategoryRepository

    @MockK
    lateinit var mapper: TransactionCategoryMapper

    @MockK
    lateinit var userProvider: CurrentUserProvider

    @MockK
    lateinit var churchRepository: ChurchRepository

    lateinit var transactionCategoryService: TransactionCategoryService

    @BeforeTest
    fun setUp() {
        MockKAnnotations.init(this)
        transactionCategoryService = TransactionCategoryService(mapper, repository, userProvider, churchRepository)
    }

    private fun buildTransactionCategory(
        name: String,
        transactionType: FinancialTransactionType,
        version: Long = 0L,
    ): TransactionCategory =
        TransactionCategory(
            id = UlidCreator.getUlid(),
            churchId = UlidCreator.getUlid(),
            addedBy = UlidCreator.getUlid(),
            createdAt = Instant.now(),
            name = name,
            transactionType = transactionType,
            version = version,
        )

    private fun buildTransactionCategoryJpaEntity(
        id: UUID = UUID.randomUUID(),
        name: String,
        transactionType: FinancialTransactionType,
        version: Long = 0L,
    ): TransactionCategoryJpaEntity =
        TransactionCategoryJpaEntity(
            id = id,
            churchId = UUID.randomUUID(),
            addedBy = UUID.randomUUID(),
            createdAt = Instant.now(),
            name = name,
            transactionType = transactionType,
            version = version,
        )

    @Test
    fun `create should persist and return a new transaction category`() {
        val churchId = UUID.randomUUID()
        val currentUserId = UlidCreator.getUlid()
        val transactionCategorySlot = slot<TransactionCategory>()
        val savedEntity = buildTransactionCategoryJpaEntity(name = "Offerings", transactionType = FinancialTransactionType.INCOME)
        val savedTransactionCategory = buildTransactionCategory(name = "Offerings", transactionType = FinancialTransactionType.INCOME)

        every { churchRepository.existsById(churchId) } returns true
        every {
            repository.findByChurchIdAndNameAndTransactionType(
                churchId = churchId,
                name = "Offerings",
                transactionType = FinancialTransactionType.INCOME,
            )
        } returns null
        every { userProvider.getCurrentUserId() } returns currentUserId
        every { mapper.toJpaEntity(capture(transactionCategorySlot)) } returns savedEntity
        every { repository.save(savedEntity) } returns savedEntity
        every { mapper.toDomain(savedEntity) } returns savedTransactionCategory

        val response =
            transactionCategoryService.create(
                churchId,
                CreateTransactionCategoryRequest(name = "Offerings", type = FinancialTransactionType.INCOME),
            )

        assertEquals("Offerings", response.name)
        assertEquals(FinancialTransactionType.INCOME, response.type)
        assertEquals(TransactionCategoryStatus.ACTIVE, response.status)
        assertNotNull(response.id)
        assertEquals(currentUserId, transactionCategorySlot.captured.addedBy)
    }

    @Test
    fun `create throws ChurchNotFoundException when church does not exist`() {
        val churchId = UUID.randomUUID()
        every { churchRepository.existsById(churchId) } returns false

        assertFailsWith<ChurchNotFoundException> {
            transactionCategoryService.create(
                churchId,
                CreateTransactionCategoryRequest(name = "Offerings", type = FinancialTransactionType.INCOME),
            )
        }
    }

    @Test
    fun `create throws TransactionCategoryNameConflictException when (church, name, type) already exists`() {
        val churchId = UUID.randomUUID()
        val existing = buildTransactionCategoryJpaEntity(name = "Offerings", transactionType = FinancialTransactionType.INCOME)

        every { churchRepository.existsById(churchId) } returns true
        every {
            repository.findByChurchIdAndNameAndTransactionType(
                churchId = churchId,
                name = "Offerings",
                transactionType = FinancialTransactionType.INCOME,
            )
        } returns existing

        assertFailsWith<TransactionCategoryNameConflictException> {
            transactionCategoryService.create(
                churchId,
                CreateTransactionCategoryRequest(name = "Offerings", type = FinancialTransactionType.INCOME),
            )
        }
    }

    @Test
    fun `get returns category when it belongs to the church`() {
        val churchId = UUID.randomUUID()
        val id = UUID.randomUUID()
        val entity =
            buildTransactionCategoryJpaEntity(id = id, name = "Offerings", transactionType = FinancialTransactionType.INCOME)
                .also { it.churchId = churchId }
        val domain = buildTransactionCategory(name = "Offerings", transactionType = FinancialTransactionType.INCOME)

        every { repository.findById(id) } returns java.util.Optional.of(entity)
        every { mapper.toDomain(entity) } returns domain.copy(churchId = Ulid.from(churchId))

        val response = transactionCategoryService.get(churchId, id)

        assertEquals("Offerings", response.name)
    }

    @Test
    fun `get throws TransactionCategoryNotFoundException when id unknown`() {
        val churchId = UUID.randomUUID()
        val id = UUID.randomUUID()
        every { repository.findById(id) } returns java.util.Optional.empty()

        assertFailsWith<TransactionCategoryNotFoundException> {
            transactionCategoryService.get(churchId, id)
        }
    }

    @Test
    fun `get throws TransactionCategoryNotFoundException when category belongs to a different church`() {
        val urlChurchId = UUID.randomUUID()
        val actualChurchId = UUID.randomUUID()
        val id = UUID.randomUUID()
        val entity =
            buildTransactionCategoryJpaEntity(id = id, name = "Offerings", transactionType = FinancialTransactionType.INCOME)
                .also { it.churchId = actualChurchId }
        val domain =
            buildTransactionCategory(name = "Offerings", transactionType = FinancialTransactionType.INCOME)
                .copy(churchId = Ulid.from(actualChurchId))

        every { repository.findById(id) } returns java.util.Optional.of(entity)
        every { mapper.toDomain(entity) } returns domain

        assertFailsWith<TransactionCategoryNotFoundException> {
            transactionCategoryService.get(urlChurchId, id)
        }
    }
}
