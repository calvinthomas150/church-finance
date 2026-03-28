package com.calvintech.churchfinance.financialtransaction.domain

import com.calvintech.churchfinance.shared.domain.FinancialTransactionType
import com.github.f4b6a3.ulid.UlidCreator
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertNotNull
import org.junit.jupiter.api.assertThrows
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.Currency

class FinancialTransactionTest {
    private fun buildTransaction(
        amount: BigDecimal = BigDecimal.TEN,
        categorisations: List<FinancialTransactionCategorisation> = listOf(),
        bankReference: String? = "Expenses",
        description: String? = "Expenses for February",
    ) = FinancialTransaction(
        id = UlidCreator.getUlid(),
        churchId = UlidCreator.getUlid(),
        createdAt = LocalDateTime.now(),
        addedBy = UlidCreator.getUlid(),
        amount = amount,
        currency = Currency.getInstance("GBP"),
        financialTransactionType = FinancialTransactionType.EXPENDITURE,
        transactionDate = LocalDate.now(),
        bankAccountId = UlidCreator.getUlid(),
        bankReference = bankReference,
        description = description,
        categorisations = categorisations,
    )

    private fun buildCategorisation(
        amount: BigDecimal,
        description: String? = "Test categorisation",
    ) = FinancialTransactionCategorisation(
        id = UlidCreator.getUlid(),
        amount = amount,
        description = description,
        fundId = UlidCreator.getUlid(),
        categoryId = UlidCreator.getUlid(),
    )

    @Test
    fun `should create valid transaction without categorisation`() {
        val financialTransaction = buildTransaction(BigDecimal.TEN, listOf())
        assertNotNull(financialTransaction)
    }

    @Test
    fun `should create a valid transaction when sum of amounts in categorisations is equal to amount in financial transaction`() {
        val amount = BigDecimal.valueOf(100)

        val expenses1 = buildCategorisation(amount.divide(BigDecimal.TWO))
        val expenses2 = buildCategorisation(amount.divide(BigDecimal.TWO))

        val financialTransaction = buildTransaction(amount, listOf(expenses1, expenses2))

        assertNotNull(financialTransaction)
    }

    @Test
    fun `should throw when sum of amounts in categorisations is less than amount in financial transaction`() {
        val amount = BigDecimal.valueOf(100)

        val expenses1 = buildCategorisation(amount.divide(BigDecimal.TWO))
        val expenses2 = buildCategorisation(amount.divide(BigDecimal.valueOf(4)))

        assertThrows<IllegalArgumentException> {
            buildTransaction(amount, listOf(expenses1, expenses2))
        }
    }

    @Test
    fun `should throw when sum of amounts in categorisations is greater than amount in financial transaction`() {
        val amount = BigDecimal.valueOf(100)

        val expenses1 = buildCategorisation(amount)
        val expenses2 = buildCategorisation(amount.divide(BigDecimal.valueOf(4)))

        assertThrows<IllegalArgumentException> {
            buildTransaction(amount, listOf(expenses1, expenses2))
        }
    }

    @Test
    fun `should create a transaction when single transaction in categorisation with amount equal to financial transaction`() {
        val amount = BigDecimal.valueOf(100)

        val expenses = buildCategorisation(amount)

        val financialTransaction = buildTransaction(amount, listOf(expenses))

        assertNotNull(financialTransaction)
    }

    @Test
    fun `should create a transaction when single transaction in categorisation with amount equal to financial transaction but different decimals`() {
        val amount = BigDecimal("100")

        val expenses = buildCategorisation(BigDecimal("100.00"))

        val financialTransaction = buildTransaction(amount, listOf(expenses))

        assertNotNull(financialTransaction)
    }

    @Test
    fun `should throw when transaction amount is negative`() {
        assertThrows<IllegalArgumentException> {
            buildTransaction(amount = BigDecimal.TEN.negate())
        }
    }

    @Test
    fun `should throw when transaction amount is zero`() {
        assertThrows<IllegalArgumentException> {
            buildTransaction(amount = BigDecimal.ZERO)
        }
    }

    @Test
    fun `should throw when bank reference is blank`() {
        assertThrows<IllegalArgumentException> {
            buildTransaction(bankReference = "")
        }

        assertThrows<IllegalArgumentException> {
            buildTransaction(bankReference = "   ")
        }
    }

    @Test
    fun `should throw when description is blank`() {
        assertThrows<IllegalArgumentException> {
            buildTransaction(description = "")
        }

        assertThrows<IllegalArgumentException> {
            buildTransaction(description = "   ")
        }
    }

    @Test
    fun `should allow null bank reference and description`() {
        val transaction = buildTransaction(bankReference = null, description = null)
        assertNotNull(transaction)
    }

    @Test
    fun `should throw when categorisation description is blank`() {
        assertThrows<IllegalArgumentException> {
            buildCategorisation(amount = BigDecimal.TEN, description = "")
        }

        assertThrows<IllegalArgumentException> {
            buildCategorisation(amount = BigDecimal.TEN, description = "   ")
        }
    }
}
