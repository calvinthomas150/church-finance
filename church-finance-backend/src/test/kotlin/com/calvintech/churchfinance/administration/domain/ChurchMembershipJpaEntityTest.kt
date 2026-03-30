package com.calvintech.churchfinance.administration.domain

import com.github.f4b6a3.ulid.UlidCreator
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull

class ChurchMembershipJpaEntityTest {
    private fun buildMembership(roles: Set<ChurchRole> = setOf(ChurchRole.SUBMITTER)): ChurchMembership =
        ChurchMembership(
            id = UlidCreator.getUlid(),
            churchId = UlidCreator.getUlid(),
            userId = UlidCreator.getUlid(),
            addedBy = UlidCreator.getUlid(),
            createdAt = Instant.now(),
            roles = roles,
        )

    @Test
    fun `should create a valid membership`() {
        val membership = buildMembership()
        assertNotNull(membership)
    }

    @Test
    fun `should create a membership with multiple roles`() {
        val membership = buildMembership(roles = setOf(ChurchRole.ADMIN, ChurchRole.TREASURER))
        assertNotNull(membership)
    }

    @Test
    fun `should throw when creating a membership with no roles`() {
        assertFailsWith<IllegalArgumentException> {
            buildMembership(roles = emptySet())
        }
    }
}
