package com.calvintech.churchfinance.administration.api

import com.calvintech.churchfinance.administration.domain.TransactionCategoryStatus

enum class TransactionCategoryStatusFilter {
    ACTIVE,
    INACTIVE,
    ALL,
    ;

    fun matches(status: TransactionCategoryStatus): Boolean =
        when (this) {
            ALL -> true
            ACTIVE -> status == TransactionCategoryStatus.ACTIVE
            INACTIVE -> status == TransactionCategoryStatus.INACTIVE
        }
}
