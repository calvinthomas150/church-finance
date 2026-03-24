package com.calvintech.churchfinance.account.domain

import com.github.f4b6a3.ulid.Ulid
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.Currency

data class BankAccount(
    val id: Ulid,
    val churchId: Ulid,
    val createdAt: LocalDateTime,
    val addedBy: Ulid,
    val accountName: String,
    val accountNumber: String?,
    val sortCode: String?,
    val accountStartDate: LocalDate,
    val bankAccountType: BankAccountType,
    val currentBalance: BigDecimal,
    val currency: Currency,
    val bankAccountStatus: BankAccountStatus,
) {
    init {
        require(
            bankAccountStatus == BankAccountStatus.OPEN ||
                bankAccountStatus == BankAccountStatus.CLOSED &&
                currentBalance == BigDecimal.ZERO,
        ) {
            "Account must be open or closed with zero balance"
        }
    }
}
