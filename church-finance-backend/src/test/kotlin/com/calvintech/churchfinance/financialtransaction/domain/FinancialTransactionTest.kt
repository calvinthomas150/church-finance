package com.calvintech.churchfinance.financialtransaction.domain

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
    ) = FinancialTransaction(
        id = UlidCreator.getUlid(),
        churchId = UlidCreator.getUlid(),
        createdAt = LocalDateTime.now(),
        addedBy = UlidCreator.getUlid(),
        amount = amount,
        currency = Currency.getInstance("GBP"),
        financialTransactionType = FinancialTransactionType.EXPENDITURE,
        transactionDate = LocalDate.now(),
        accountId = UlidCreator.getUlid(),
        bankReference = "Expenses",
        description = "Expenses for February",
        categorisations = categorisations,
    )

    private fun buildCategorisation(amount: BigDecimal) =
        FinancialTransactionCategorisation(
            id = UlidCreator.getUlid(),
            amount = amount,
            description = "Test categorisation",
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
}
