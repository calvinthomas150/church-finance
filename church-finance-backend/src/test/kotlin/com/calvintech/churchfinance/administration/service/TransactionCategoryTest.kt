package com.calvintech.churchfinance.administration.service

import com.calvintech.churchfinance.administration.api.CreateTransactionCategoryRequest
import com.calvintech.churchfinance.administration.api.TransactionCategoryStatusFilter
import com.calvintech.churchfinance.administration.api.UpdateTransactionCategoryRequest
import com.calvintech.churchfinance.administration.domain.ChurchNotFoundException
import com.calvintech.churchfinance.administration.domain.TransactionCategory
import com.calvintech.churchfinance.administration.domain.TransactionCategoryNameConflictException
import com.calvintech.churchfinance.administration.domain.TransactionCategoryNotFoundException
import com.calvintech.churchfinance.administration.domain.TransactionCategoryStatus
import com.calvintech.churchfinance.administration.persistence.ChurchRepository
import com.calvintech.churchfinance.administration.persistence.TransactionCategoryJpaEntity
import com.calvintech.churchfinance.administration.persistence.TransactionCategoryMapper
import com.calvintech.churchfinance.administration.persistence.TransactionCategoryRepository
import com.calvintech.churchfinance.shared.domain.FinancialTransactionType
import com.calvintech.churchfinance.shared.service.CurrentUserProvider
import com.github.f4b6a3.ulid.Ulid
import com.github.f4b6a3.ulid.UlidCreator
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.slot
import io.mockk.verify
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

    @Test
    fun `update changes name and returns updated response`() {
        val churchId = UUID.randomUUID()
        val id = UUID.randomUUID()
        val existing =
            buildTransactionCategoryJpaEntity(id = id, name = "Old", transactionType = FinancialTransactionType.INCOME)
                .also { it.churchId = churchId }
        val existingDomain =
            buildTransactionCategory(name = "Old", transactionType = FinancialTransactionType.INCOME)
                .copy(id = Ulid.from(id), churchId = Ulid.from(churchId))
        val saved =
            buildTransactionCategoryJpaEntity(id = id, name = "New", transactionType = FinancialTransactionType.INCOME)
                .also {
                    it.churchId = churchId
                    it.version = 1
                }
        val savedDomain =
            buildTransactionCategory(name = "New", transactionType = FinancialTransactionType.INCOME)
                .copy(id = Ulid.from(id), churchId = Ulid.from(churchId), version = 1)

        every { repository.findById(id) } returns java.util.Optional.of(existing)
        every { mapper.toDomain(existing) } returns existingDomain
        every {
            repository.findByChurchIdAndNameAndTransactionType(churchId, "New", FinancialTransactionType.INCOME)
        } returns null
        every { mapper.toJpaEntity(any()) } returns saved
        every { repository.save(saved) } returns saved
        every { mapper.toDomain(saved) } returns savedDomain

        val response =
            transactionCategoryService.update(
                churchId = churchId,
                id = id,
                updateTransactionCategoryRequest = UpdateTransactionCategoryRequest(name = "New", version = 0),
            )

        assertEquals("New", response.name)
    }

    @Test
    fun `update skips uniqueness check when name is unchanged`() {
        val churchId = UUID.randomUUID()
        val id = UUID.randomUUID()
        val existing =
            buildTransactionCategoryJpaEntity(id = id, name = "Same", transactionType = FinancialTransactionType.INCOME)
                .also { it.churchId = churchId }
        val existingDomain =
            buildTransactionCategory(name = "Same", transactionType = FinancialTransactionType.INCOME)
                .copy(id = Ulid.from(id), churchId = Ulid.from(churchId))

        every { repository.findById(id) } returns java.util.Optional.of(existing)
        every { mapper.toDomain(existing) } returns existingDomain
        every { mapper.toJpaEntity(any()) } returns existing
        every { repository.save(existing) } returns existing
        every { mapper.toDomain(existing) } returns existingDomain

        transactionCategoryService.update(
            churchId = churchId,
            id = id,
            updateTransactionCategoryRequest = UpdateTransactionCategoryRequest(name = "Same", version = 0),
        )

        verify(exactly = 0) {
            repository.findByChurchIdAndNameAndTransactionType(any(), any(), any())
        }
    }

    @Test
    fun `update throws conflict when new name collides with another category of the same type`() {
        val churchId = UUID.randomUUID()
        val id = UUID.randomUUID()
        val collidingId = UUID.randomUUID()
        val existing =
            buildTransactionCategoryJpaEntity(id = id, name = "Old", transactionType = FinancialTransactionType.INCOME)
                .also { it.churchId = churchId }
        val existingDomain =
            buildTransactionCategory(name = "Old", transactionType = FinancialTransactionType.INCOME)
                .copy(id = Ulid.from(id), churchId = Ulid.from(churchId))
        val colliding =
            buildTransactionCategoryJpaEntity(id = collidingId, name = "Taken", transactionType = FinancialTransactionType.INCOME)
                .also { it.churchId = churchId }

        every { repository.findById(id) } returns java.util.Optional.of(existing)
        every { mapper.toDomain(existing) } returns existingDomain
        every {
            repository.findByChurchIdAndNameAndTransactionType(churchId, "Taken", FinancialTransactionType.INCOME)
        } returns colliding

        assertFailsWith<TransactionCategoryNameConflictException> {
            transactionCategoryService.update(
                churchId = churchId,
                id = id,
                updateTransactionCategoryRequest = UpdateTransactionCategoryRequest(name = "Taken", version = 0),
            )
        }
    }

    @Test
    fun `update throws not-found on tenant mismatch`() {
        val urlChurchId = UUID.randomUUID()
        val actualChurchId = UUID.randomUUID()
        val id = UUID.randomUUID()
        val existing =
            buildTransactionCategoryJpaEntity(id = id, name = "X", transactionType = FinancialTransactionType.INCOME)
                .also { it.churchId = actualChurchId }
        val existingDomain =
            buildTransactionCategory(name = "X", transactionType = FinancialTransactionType.INCOME)
                .copy(id = Ulid.from(id), churchId = Ulid.from(actualChurchId))

        every { repository.findById(id) } returns java.util.Optional.of(existing)
        every { mapper.toDomain(existing) } returns existingDomain

        assertFailsWith<TransactionCategoryNotFoundException> {
            transactionCategoryService.update(
                churchId = urlChurchId,
                id = id,
                updateTransactionCategoryRequest = UpdateTransactionCategoryRequest(name = "Y", version = 0),
            )
        }
    }

    private fun jpaList(vararg entities: TransactionCategoryJpaEntity) = entities.toList()

    @Test
    fun `list returns only ACTIVE categories sorted by name when no filters supplied`() {
        val churchId = UUID.randomUUID()
        val active = buildTransactionCategoryJpaEntity(name = "Tithes", transactionType = FinancialTransactionType.INCOME)
        val inactive =
            buildTransactionCategoryJpaEntity(name = "Old", transactionType = FinancialTransactionType.INCOME)
                .also { it.status = TransactionCategoryStatus.INACTIVE }

        every { churchRepository.existsById(churchId) } returns true
        every { repository.findAllByChurchId(churchId) } returns jpaList(inactive, active)
        every { mapper.toDomain(active) } returns
            buildTransactionCategory(name = "Tithes", transactionType = FinancialTransactionType.INCOME)
        every { mapper.toDomain(inactive) } returns
            buildTransactionCategory(
                name = "Old",
                transactionType = FinancialTransactionType.INCOME,
            ).copy(status = TransactionCategoryStatus.INACTIVE)

        val result = transactionCategoryService.list(churchId, TransactionCategoryStatusFilter.ACTIVE, type = null)

        assertEquals(1, result.size)
        assertEquals("Tithes", result.first().name)
    }

    @Test
    fun `list with status=ALL returns all rows sorted by name`() {
        val churchId = UUID.randomUUID()
        val a = buildTransactionCategoryJpaEntity(name = "Z", transactionType = FinancialTransactionType.INCOME)
        val b =
            buildTransactionCategoryJpaEntity(name = "A", transactionType = FinancialTransactionType.EXPENDITURE)
                .also { it.status = TransactionCategoryStatus.INACTIVE }

        every { churchRepository.existsById(churchId) } returns true
        every { repository.findAllByChurchId(churchId) } returns jpaList(a, b)
        every { mapper.toDomain(a) } returns buildTransactionCategory(name = "Z", transactionType = FinancialTransactionType.INCOME)
        every { mapper.toDomain(b) } returns
            buildTransactionCategory(name = "A", transactionType = FinancialTransactionType.EXPENDITURE)
                .copy(status = TransactionCategoryStatus.INACTIVE)

        val result = transactionCategoryService.list(churchId, TransactionCategoryStatusFilter.ALL, type = null)

        assertEquals(listOf("A", "Z"), result.map { it.name })
    }

    @Test
    fun `list with status=INACTIVE returns only inactive`() {
        val churchId = UUID.randomUUID()
        val active = buildTransactionCategoryJpaEntity(name = "X", transactionType = FinancialTransactionType.INCOME)
        val inactive =
            buildTransactionCategoryJpaEntity(name = "Y", transactionType = FinancialTransactionType.INCOME)
                .also { it.status = TransactionCategoryStatus.INACTIVE }

        every { churchRepository.existsById(churchId) } returns true
        every { repository.findAllByChurchId(churchId) } returns jpaList(active, inactive)
        every { mapper.toDomain(active) } returns buildTransactionCategory(name = "X", transactionType = FinancialTransactionType.INCOME)
        every { mapper.toDomain(inactive) } returns
            buildTransactionCategory(name = "Y", transactionType = FinancialTransactionType.INCOME)
                .copy(status = TransactionCategoryStatus.INACTIVE)

        val result = transactionCategoryService.list(churchId, TransactionCategoryStatusFilter.INACTIVE, type = null)

        assertEquals(listOf("Y"), result.map { it.name })
    }

    @Test
    fun `list with type filter returns only matching type`() {
        val churchId = UUID.randomUUID()
        val income = buildTransactionCategoryJpaEntity(name = "X", transactionType = FinancialTransactionType.INCOME)
        val expense = buildTransactionCategoryJpaEntity(name = "Y", transactionType = FinancialTransactionType.EXPENDITURE)

        every { churchRepository.existsById(churchId) } returns true
        every { repository.findAllByChurchId(churchId) } returns jpaList(income, expense)
        every { mapper.toDomain(income) } returns buildTransactionCategory(name = "X", transactionType = FinancialTransactionType.INCOME)
        every { mapper.toDomain(expense) } returns
            buildTransactionCategory(name = "Y", transactionType = FinancialTransactionType.EXPENDITURE)

        val result =
            transactionCategoryService.list(
                churchId,
                TransactionCategoryStatusFilter.ACTIVE,
                type = FinancialTransactionType.INCOME,
            )

        assertEquals(listOf("X"), result.map { it.name })
    }

    @Test
    fun `list throws ChurchNotFoundException when church does not exist`() {
        val churchId = UUID.randomUUID()
        every { churchRepository.existsById(churchId) } returns false

        assertFailsWith<ChurchNotFoundException> {
            transactionCategoryService.list(churchId, TransactionCategoryStatusFilter.ACTIVE, type = null)
        }
    }

    @Test
    fun `deactivate sets status to INACTIVE`() {
        val churchId = UUID.randomUUID()
        val id = UUID.randomUUID()
        val existing =
            buildTransactionCategoryJpaEntity(id = id, name = "X", transactionType = FinancialTransactionType.INCOME)
                .also { it.churchId = churchId }
        val existingDomain =
            buildTransactionCategory(name = "X", transactionType = FinancialTransactionType.INCOME)
                .copy(id = Ulid.from(id), churchId = Ulid.from(churchId))
        val deactivated =
            buildTransactionCategoryJpaEntity(id = id, name = "X", transactionType = FinancialTransactionType.INCOME)
                .also {
                    it.churchId = churchId
                    it.status = TransactionCategoryStatus.INACTIVE
                    it.version = 1
                }
        val deactivatedDomain = existingDomain.copy(status = TransactionCategoryStatus.INACTIVE, version = 1)

        every { repository.findById(id) } returns java.util.Optional.of(existing)
        every { mapper.toDomain(existing) } returns existingDomain
        every { mapper.toJpaEntity(any()) } returns deactivated
        every { repository.save(deactivated) } returns deactivated
        every { mapper.toDomain(deactivated) } returns deactivatedDomain

        val response = transactionCategoryService.deactivate(churchId, id, version = 0)

        assertEquals(TransactionCategoryStatus.INACTIVE, response.status)
    }

    @Test
    fun `activate sets status to ACTIVE`() {
        val churchId = UUID.randomUUID()
        val id = UUID.randomUUID()
        val existing =
            buildTransactionCategoryJpaEntity(id = id, name = "X", transactionType = FinancialTransactionType.INCOME)
                .also {
                    it.churchId = churchId
                    it.status = TransactionCategoryStatus.INACTIVE
                }
        val existingDomain =
            buildTransactionCategory(name = "X", transactionType = FinancialTransactionType.INCOME)
                .copy(id = Ulid.from(id), churchId = Ulid.from(churchId), status = TransactionCategoryStatus.INACTIVE)
        val activated =
            buildTransactionCategoryJpaEntity(id = id, name = "X", transactionType = FinancialTransactionType.INCOME)
                .also {
                    it.churchId = churchId
                    it.version = 2
                }
        val activatedDomain = existingDomain.copy(status = TransactionCategoryStatus.ACTIVE, version = 2)

        every { repository.findById(id) } returns java.util.Optional.of(existing)
        every { mapper.toDomain(existing) } returns existingDomain
        every { mapper.toJpaEntity(any()) } returns activated
        every { repository.save(activated) } returns activated
        every { mapper.toDomain(activated) } returns activatedDomain

        val response = transactionCategoryService.activate(churchId, id, version = 1)

        assertEquals(TransactionCategoryStatus.ACTIVE, response.status)
    }

    @Test
    fun `deactivate throws not-found on tenant mismatch`() {
        val urlChurchId = UUID.randomUUID()
        val actualChurchId = UUID.randomUUID()
        val id = UUID.randomUUID()
        val existing =
            buildTransactionCategoryJpaEntity(id = id, name = "X", transactionType = FinancialTransactionType.INCOME)
                .also { it.churchId = actualChurchId }
        val existingDomain =
            buildTransactionCategory(name = "X", transactionType = FinancialTransactionType.INCOME)
                .copy(id = Ulid.from(id), churchId = Ulid.from(actualChurchId))

        every { repository.findById(id) } returns java.util.Optional.of(existing)
        every { mapper.toDomain(existing) } returns existingDomain

        assertFailsWith<TransactionCategoryNotFoundException> {
            transactionCategoryService.deactivate(urlChurchId, id, version = 0)
        }
    }

    @Test
    fun `activate throws not-found on tenant mismatch`() {
        val urlChurchId = UUID.randomUUID()
        val actualChurchId = UUID.randomUUID()
        val id = UUID.randomUUID()
        val existing =
            buildTransactionCategoryJpaEntity(id = id, name = "X", transactionType = FinancialTransactionType.INCOME)
                .also { it.churchId = actualChurchId }
        val existingDomain =
            buildTransactionCategory(name = "X", transactionType = FinancialTransactionType.INCOME)
                .copy(id = Ulid.from(id), churchId = Ulid.from(actualChurchId))

        every { repository.findById(id) } returns java.util.Optional.of(existing)
        every { mapper.toDomain(existing) } returns existingDomain

        assertFailsWith<TransactionCategoryNotFoundException> {
            transactionCategoryService.activate(urlChurchId, id, version = 0)
        }
    }
}
