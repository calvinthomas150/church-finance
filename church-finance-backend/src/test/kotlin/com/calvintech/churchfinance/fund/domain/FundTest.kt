package com.calvintech.churchfinance.fund.domain

import com.github.f4b6a3.ulid.UlidCreator
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertNotNull
import org.junit.jupiter.api.assertThrows
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.Currency

class FundTest {
    private fun buildFund(
        currentBalance: BigDecimal = BigDecimal.ZERO,
        fundName: String = "My Fund",
        fundStatus: FundStatus = FundStatus.CLOSED,
    ): Fund =
        Fund(
            id = UlidCreator.getUlid(),
            churchId = UlidCreator.getUlid(),
            createdAt = LocalDateTime.now(),
            addedBy = UlidCreator.getUlid(),
            fundName = fundName,
            fundType = FundType.RESTRICTED,
            currentBalance = currentBalance,
            currency = Currency.getInstance("GBP"),
            fundStatus = fundStatus,
        )

    @Test
    fun `should create a closed fund with zero balance`() {
        val account = buildFund(BigDecimal.ZERO, fundName = "My Fund")
        assertNotNull(account)
    }

    @Test
    fun `should create a closed fund with zero balance of different scale`() {
        val fund = buildFund(BigDecimal("0.00"), fundName = "My Fund")
        assertNotNull(fund)
    }

    @Test
    fun `should throw when creating a closed fund with a non zero balance`() {
        assertThrows<IllegalArgumentException> {
            buildFund(BigDecimal.ONE, fundName = "My Fund")
        }

        assertThrows<IllegalArgumentException> {
            buildFund(BigDecimal.ONE.negate(), fundName = "My Fund")
        }
    }

    @Test
    fun `should throw when creating a fund without a name`() {
        assertThrows<IllegalArgumentException> {
            buildFund(fundName = "", fundStatus = FundStatus.OPEN)
        }

        assertThrows<IllegalArgumentException> {
            buildFund(fundName = "   ", fundStatus = FundStatus.OPEN)
        }
    }
}