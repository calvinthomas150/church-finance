package com.calvintech.churchfinance.administration.persistence

import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface TransactionCategoryRepository : JpaRepository<TransactionCategoryJpaEntity, UUID>
