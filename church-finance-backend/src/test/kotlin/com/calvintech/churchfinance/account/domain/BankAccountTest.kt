package com.calvintech.churchfinance.account.domain

import com.github.f4b6a3.ulid.UlidCreator
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.util.Currency
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull

class BankAccountTest {
    private fun buildAccount(
        currentBalance: BigDecimal = BigDecimal.ZERO,
        accountName: String = "My Account",
        accountNumber: String? = "12345678",
        sortCode: String? = "123456",
        bankAccountStatus: BankAccountStatus = BankAccountStatus.CLOSED,
    ) = BankAccount(
        id = UlidCreator.getUlid(),
        churchId = UlidCreator.getUlid(),
        createdAt = Instant.now(),
        addedBy = UlidCreator.getUlid(),
        accountName = accountName,
        accountNumber = accountNumber,
        sortCode = sortCode,
        accountStartDate = LocalDate.now(),
        bankAccountType = BankAccountType.CURRENT,
        currentBalance = currentBalance,
        currency = Currency.getInstance("GBP"),
        bankAccountStatus = bankAccountStatus,
    )

    @Test
    fun `should create a closed account with zero balance`() {
        val account = buildAccount(currentBalance = BigDecimal.ZERO)
        assertNotNull(account)
    }

    @Test
    fun `should create a closed account with zero balance of different scale`() {
        val account = buildAccount(currentBalance = BigDecimal("0.00"))
        assertNotNull(account)
    }

    @Test
    fun `should throw when creating a closed account with a non zero balance`() {
        assertFailsWith<IllegalArgumentException> {
            buildAccount(currentBalance = BigDecimal.ONE)
        }
    }

    @Test
    fun `should throw when creating an account without a name`() {
        assertFailsWith<IllegalArgumentException> {
            buildAccount(accountName = "")
        }

        assertFailsWith<IllegalArgumentException> {
            buildAccount(accountName = "   ")
        }
    }

    @Test
    fun `should throw when creating an account with a sort code not equal to 6 digits`() {
        assertFailsWith<IllegalArgumentException> {
            buildAccount(sortCode = "1234567")
        }

        assertFailsWith<IllegalArgumentException> {
            buildAccount(sortCode = "12345")
        }

        assertFailsWith<IllegalArgumentException> {
            buildAccount(sortCode = "abcde1")
        }
    }

    @Test
    fun `should accept sort code with leading zeros`() {
        val account = buildAccount(sortCode = "006123")
        assertNotNull(account)
    }

    @Test
    fun `should throw when sort code is blank`() {
        assertFailsWith<IllegalArgumentException> {
            buildAccount(sortCode = "")
        }

        assertFailsWith<IllegalArgumentException> {
            buildAccount(sortCode = "   ")
        }
    }

    @Test
    fun `should throw when creating an account with a account number not equal to 8 or 9 digits`() {
        assertFailsWith<IllegalArgumentException> {
            buildAccount(accountNumber = "0123456789")
        }

        assertFailsWith<IllegalArgumentException> {
            buildAccount(accountNumber = "1234567")
        }

        assertFailsWith<IllegalArgumentException> {
            buildAccount(accountNumber = "abcdefghi")
        }
    }

    @Test
    fun `should throw when account number is blank`() {
        assertFailsWith<IllegalArgumentException> {
            buildAccount(accountNumber = "")
        }

        assertFailsWith<IllegalArgumentException> {
            buildAccount(accountNumber = "   ")
        }
    }
}
