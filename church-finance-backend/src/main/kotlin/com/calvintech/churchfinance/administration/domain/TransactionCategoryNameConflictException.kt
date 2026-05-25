package com.calvintech.churchfinance.administration.domain

import com.calvintech.churchfinance.shared.domain.FinancialTransactionType
import java.util.UUID

class TransactionCategoryNameConflictException(
    val churchId: UUID,
    val name: String,
    val type: FinancialTransactionType,
) : RuntimeException(
        "A category named '$name' already exists for $type in this church.",
    )
