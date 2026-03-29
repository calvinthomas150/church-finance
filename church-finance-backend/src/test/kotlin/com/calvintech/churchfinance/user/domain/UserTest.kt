package com.calvintech.churchfinance.user.domain

import com.github.f4b6a3.ulid.UlidCreator
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertNotNull
import org.junit.jupiter.api.assertThrows
import java.time.LocalDateTime

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
            createdAt = LocalDateTime.now(),
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
    fun `should throw when creating a user with empty firstname`() {
        assertThrows<IllegalArgumentException> {
            buildUser(firstName = "")
        }

        assertThrows<IllegalArgumentException> {
            buildUser(firstName = "   ")
        }
    }

    @Test
    fun `should throw when creating a user with empty lastname`() {
        assertThrows<IllegalArgumentException> {
            buildUser(lastName = "")
        }

        assertThrows<IllegalArgumentException> {
            buildUser(lastName = "   ")
        }
    }

    @Test
    fun `should throw when creating a user where the email address is invalid`() {
        assertThrows<IllegalArgumentException> {
            buildUser(email = "abcde")
        }

        assertThrows<IllegalArgumentException> {
            buildUser(email = "abcde@")
        }

        assertThrows<IllegalArgumentException> {
            buildUser(email = "abcde@xyz.")
        }

        assertThrows<IllegalArgumentException> {
            buildUser(email = ".com")
        }
    }

    @Test
    fun `should throw when creating a user where the email address is invalid and the first name is empty`() {
        assertThrows<IllegalArgumentException> {
            buildUser(firstName = null, email = "abcde")
        }
    }
}
