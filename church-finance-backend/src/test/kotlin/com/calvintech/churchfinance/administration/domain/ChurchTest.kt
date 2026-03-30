package com.calvintech.churchfinance.administration.domain

import com.github.f4b6a3.ulid.UlidCreator
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull

class ChurchTest {
    private fun buildChurch(
        name: String = "Grace Church",
        status: ChurchStatus = ChurchStatus.ACTIVE,
    ): Church =
        Church(
            id = UlidCreator.getUlid(),
            createdAt = Instant.now(),
            addedBy = UlidCreator.getUlid(),
            name = name,
            status = status,
        )

    @Test
    fun `should create a valid church`() {
        val church = buildChurch()
        assertNotNull(church)
    }

    @Test
    fun `should create an inactive church`() {
        val church = buildChurch(status = ChurchStatus.INACTIVE)
        assertNotNull(church)
    }

    @Test
    fun `should throw when creating a church with blank name`() {
        assertFailsWith<IllegalArgumentException> {
            buildChurch(name = "")
        }

        assertFailsWith<IllegalArgumentException> {
            buildChurch(name = "   ")
        }
    }
}
