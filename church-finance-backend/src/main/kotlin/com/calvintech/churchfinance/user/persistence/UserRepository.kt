package com.calvintech.churchfinance.user.persistence

import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface UserRepository : JpaRepository<UserJpaEntity, UUID>
