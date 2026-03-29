package com.calvintech.churchfinance.shared.domain

import java.math.BigDecimal
import java.util.Currency

data class Money(
    val amount: BigDecimal,
    val currency: Currency,
) {
    init {
        require(amount.scale() <= 2) {
            "Amount scale must not exceed 2 decimal places"
        }
    }
}
