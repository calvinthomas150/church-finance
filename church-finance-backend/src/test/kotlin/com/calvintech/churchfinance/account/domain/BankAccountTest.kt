package com.calvintech.churchfinance.account.domain

import com.github.f4b6a3.ulid.UlidCreator
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertNotNull
import org.junit.jupiter.api.assertThrows
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.Currency

class BankAccountTest {
    private fun buildAccount(currentBalance: BigDecimal) =
        BankAccount(
            id = UlidCreator.getUlid(),
            churchId = UlidCreator.getUlid(),
            createdAt = LocalDateTime.now(),
            addedBy = UlidCreator.getUlid(),
            accountName = "My Account",
            accountNumber = "12345678",
            sortCode = "123456",
            accountStartDate = LocalDate.now(),
            bankAccountType = BankAccountType.CURRENT,
            currentBalance = currentBalance,
            currency = Currency.getInstance("GBP"),
            bankAccountStatus = BankAccountStatus.CLOSED,
        )

    @Test
    fun `should create a closed account with zero balance`() {
        val account = buildAccount(BigDecimal.ZERO)
        assertNotNull(account)
    }

    @Test
    fun `should throw when creating a closed account with a non zero balance`() {
        assertThrows<IllegalArgumentException> {
            buildAccount(BigDecimal.ONE)
        }
    }
}
