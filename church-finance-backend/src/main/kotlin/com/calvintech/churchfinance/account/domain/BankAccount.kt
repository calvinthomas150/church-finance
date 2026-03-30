package com.calvintech.churchfinance.account.domain

import com.github.f4b6a3.ulid.Ulid
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.util.Currency

data class BankAccount(
    val id: Ulid,
    val churchId: Ulid,
    val createdAt: Instant,
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

        require(accountName.isNotBlank()) {
            "account name cannot be blank"
        }

        sortCode?.let {
            require(it.matches(Regex("^\\d{6}$"))) {
                "Sort code must be 6 digits"
            }
        }

        accountNumber?.let {
            require(it.matches(Regex("^\\d{8,9}$"))) {
                "Account number must be 8 or 9 digits"
            }
        }

        require(
            bankAccountStatus == BankAccountStatus.OPEN ||
                bankAccountStatus == BankAccountStatus.CLOSED &&
                currentBalance.compareTo(BigDecimal.ZERO) == 0,
        ) {
            "Account must be open or closed with zero balance"
        }
    }
}
