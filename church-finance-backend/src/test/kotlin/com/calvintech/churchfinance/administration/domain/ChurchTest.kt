package com.calvintech.churchfinance.administration.domain

import com.github.f4b6a3.ulid.UlidCreator
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertNotNull
import org.junit.jupiter.api.assertThrows
import java.time.Instant

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
        assertThrows<IllegalArgumentException> {
            buildChurch(name = "")
        }

        assertThrows<IllegalArgumentException> {
            buildChurch(name = "   ")
        }
    }
}
