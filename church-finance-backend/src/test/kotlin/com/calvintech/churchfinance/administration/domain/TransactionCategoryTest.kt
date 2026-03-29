package com.calvintech.churchfinance.administration.domain

import com.calvintech.churchfinance.shared.domain.FinancialTransactionType
import com.github.f4b6a3.ulid.Ulid
import com.github.f4b6a3.ulid.UlidCreator
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertNotNull
import org.junit.jupiter.api.assertThrows
import java.time.LocalDateTime

class TransactionCategoryTest {
    private fun buildTransactionCategory(
        churchId: Ulid = UlidCreator.getUlid(),
        name: String = "OFFERINGS",
    ) = TransactionCategory(
        id = UlidCreator.getUlid(),
        churchId = churchId,
        createdAt = LocalDateTime.now(),
        addedBy = UlidCreator.getUlid(),
        name = name,
        transactionType = FinancialTransactionType.INCOME,
    )

    @Test
    fun `should create a valid transaction category`() {
        val category = buildTransactionCategory()
        assertNotNull(category)
    }

    @Test
    fun `should throw when name of category is empty`() {
        assertThrows<IllegalArgumentException> {
            buildTransactionCategory(name = "")
        }

        assertThrows<IllegalArgumentException> {
            buildTransactionCategory(name = "   ")
        }
    }
}
