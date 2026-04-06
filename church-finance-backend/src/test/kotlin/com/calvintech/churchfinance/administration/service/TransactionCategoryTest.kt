package com.calvintech.churchfinance.administration.service

import com.calvintech.churchfinance.administration.api.CreateTransactionCategoryRequest
import com.calvintech.churchfinance.administration.domain.TransactionCategory
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

class TransactionCategoryTest {
    @MockK
    lateinit var repository: TransactionCategoryRepository

    @MockK
    lateinit var mapper: TransactionCategoryMapper

    @MockK
    lateinit var userProvider: CurrentUserProvider

    lateinit var transactionCategoryService: TransactionCategoryService

    @BeforeTest
    fun setUp() {
        MockKAnnotations.init(this)
        transactionCategoryService = TransactionCategoryService(mapper, repository, userProvider)
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
        assertNotNull(response.id)
        assertEquals(currentUserId, transactionCategorySlot.captured.addedBy)
    }
}
