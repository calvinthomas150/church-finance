package com.calvintech.churchfinance.administration.persistence

import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface ChurchMembershipRepository : JpaRepository<ChurchMembershipJpaEntity, UUID>
