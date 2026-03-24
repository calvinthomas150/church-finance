package com.calvintech.churchfinance.fund.domain

import com.github.f4b6a3.ulid.UlidCreator
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertNotNull
import org.junit.jupiter.api.assertThrows
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.Currency

class FundTest {
    private fun buildFund(currentBalance: BigDecimal) =
        Fund(
            id = UlidCreator.getUlid(),
            churchId = UlidCreator.getUlid(),
            createdAt = LocalDateTime.now(),
            addedBy = UlidCreator.getUlid(),
            accountName = "My Account",
            accountNumber = "12345678",
            sortCode = "123456",
            accountStartDate = LocalDate.now(),
            fundType = FundType.RESTRICTED,
            currentBalance = currentBalance,
            currency = Currency.getInstance("GBP"),
            fundStatus = FundStatus.CLOSED,
        )

    @Test
    fun `should create a closed fund with zero balance`() {
        val account = buildFund(BigDecimal.ZERO)
        assertNotNull(account)
    }

    @Test
    fun `should throw when creating a closed fund with a non zero balance`() {
        assertThrows<IllegalArgumentException> {
            buildFund(BigDecimal.ONE)
        }
    }
}
