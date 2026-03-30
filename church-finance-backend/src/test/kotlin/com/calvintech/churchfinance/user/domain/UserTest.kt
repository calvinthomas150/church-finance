package com.calvintech.churchfinance.user.domain

import com.github.f4b6a3.ulid.UlidCreator
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull

class UserTest {
    private fun buildUser(
        email: String = "test@test.com",
        firstName: String? = "FirstName",
        lastName: String? = "LastName",
        userStatus: UserStatus = UserStatus.ACTIVE,
        isSystemAdmin: Boolean = false,
    ): User =
        User(
            id = UlidCreator.getUlid(),
            createdAt = Instant.now(),
            addedBy = UlidCreator.getUlid(),
            email = email,
            firstName = firstName,
            lastName = lastName,
            userStatus = userStatus,
            isSystemAdmin = isSystemAdmin,
        )

    @Test
    fun `should create a valid user`() {
        val user = buildUser()
        assertNotNull(user)
    }

    @Test
    fun `should create a user with null names`() {
        val user = buildUser(firstName = null, lastName = null)
        assertNotNull(user)
    }

    @Test
    fun `should throw when creating a user with blank firstname`() {
        assertFailsWith<IllegalArgumentException> {
            buildUser(firstName = "")
        }

        assertFailsWith<IllegalArgumentException> {
            buildUser(firstName = "   ")
        }
    }

    @Test
    fun `should throw when creating a user with blank lastname`() {
        assertFailsWith<IllegalArgumentException> {
            buildUser(lastName = "")
        }

        assertFailsWith<IllegalArgumentException> {
            buildUser(lastName = "   ")
        }
    }

    @Test
    fun `should throw when creating a user where the email address is invalid`() {
        assertFailsWith<IllegalArgumentException> {
            buildUser(email = "abcde")
        }

        assertFailsWith<IllegalArgumentException> {
            buildUser(email = "abcde@")
        }

        assertFailsWith<IllegalArgumentException> {
            buildUser(email = "abcde@xyz.")
        }

        assertFailsWith<IllegalArgumentException> {
            buildUser(email = ".com")
        }
    }

    @Test
    fun `should throw when creating a user where the email address is invalid and the first name is empty`() {
        assertFailsWith<IllegalArgumentException> {
            buildUser(firstName = null, email = "abcde")
        }
    }
}
