package com.calvintech.churchfinance.administration.domain

import java.util.UUID

class TransactionCategoryNotFoundException(
    id: UUID,
) : RuntimeException("Transaction category not found: $id")
