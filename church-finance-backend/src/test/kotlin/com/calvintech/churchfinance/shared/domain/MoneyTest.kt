package com.calvintech.churchfinance.shared.domain

import java.math.BigDecimal
import java.util.Currency
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull

class MoneyTest {
    private val gbp = Currency.getInstance("GBP")

    @Test
    fun `should create valid money`() {
        val money = Money(BigDecimal("100.00"), gbp)
        assertNotNull(money)
    }

    @Test
    fun `should create money with zero amount`() {
        val money = Money(BigDecimal.ZERO, gbp)
        assertNotNull(money)
    }

    @Test
    fun `should create money with different scales`() {
        val money1 = Money(BigDecimal("100"), gbp)
        val money2 = Money(BigDecimal("100.00"), gbp)
        assertNotNull(money1)
        assertNotNull(money2)
    }

    @Test
    fun `should throw when scale exceeds 2`() {
        assertFailsWith<IllegalArgumentException> {
            Money(BigDecimal("100.001"), gbp)
        }
    }
}
