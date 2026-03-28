package com.calvintech.churchfinance.fund.domain

import com.github.f4b6a3.ulid.Ulid
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.Currency

data class Fund(
    val id: Ulid,
    val churchId: Ulid,
    val createdAt: LocalDateTime,
    val addedBy: Ulid,
    val fundName: String,
    val fundType: FundType,
    val currentBalance: BigDecimal,
    val currency: Currency,
    val fundStatus: FundStatus,
) {
    init {
        require(fundStatus == FundStatus.OPEN || fundStatus == FundStatus.CLOSED && currentBalance.compareTo(BigDecimal.ZERO) == 0) {
            "Funds must be open or closed with zero balance"
        }

        require(fundName.isNotBlank()) {
            "Funds must have a name"
        }
    }
}
