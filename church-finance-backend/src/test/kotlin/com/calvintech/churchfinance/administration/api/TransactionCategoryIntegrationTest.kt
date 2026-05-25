package com.calvintech.churchfinance.administration.api

import com.calvintech.churchfinance.TestcontainersConfiguration
import com.calvintech.churchfinance.shared.domain.FinancialTransactionType
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.testcontainers.context.ImportTestcontainers
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import tools.jackson.databind.ObjectMapper
import java.util.UUID
import kotlin.test.Test

@SpringBootTest
@AutoConfigureMockMvc
@ImportTestcontainers(TestcontainersConfiguration::class)
class TransactionCategoryIntegrationTest {
    @Autowired
    lateinit var mockMvc: MockMvc

    @Autowired
    lateinit var objectMapper: ObjectMapper

    private fun postCategory(
        churchId: UUID,
        name: String,
        type: String,
    ) = mockMvc.perform(
        post("/api/v1/churches/{churchId}/transaction-categories", churchId)
            .contentType(MediaType.APPLICATION_JSON)
            .content("""{"name": "$name", "type": "$type"}"""),
    )

    private fun createCategoryAndReturnBody(
        churchId: UUID,
        name: String = "Offerings",
        type: String = "INCOME",
    ): String =
        mockMvc
            .perform(
                post("/api/v1/churches/{churchId}/transaction-categories", churchId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"name": "$name", "type": "$type"}"""),
            ).andExpect(status().isCreated)
            .andReturn()
            .response.contentAsString

    private fun extractId(body: String): String = objectMapper.readTree(body).get("id").asString()

    private fun createChurch(name: String = "Our Saviour Lutheran Church"): String {
        val result =
            mockMvc
                .perform(
                    post("/api/v1/churches")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""{"name": "$name"}"""),
                ).andExpect(status().isCreated)
                .andReturn()
        return objectMapper.readTree(result.response.contentAsString).get("id").asString()
    }

    @Test
    fun `POST should return 404 when church does not exist`() {
        val unknownChurchId = UUID.randomUUID()
        mockMvc
            .perform(
                post("/api/v1/churches/{churchId}/transaction-categories", unknownChurchId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"name": "Offerings", "type": "INCOME"}"""),
            ).andExpect(status().isNotFound)
            .andExpect(jsonPath("$.message").exists())
    }

    @Test
    fun `POST should create a transaction category and return 201`() {
        val churchId = UUID.fromString(createChurch())
        mockMvc
            .perform(
                post("/api/v1/churches/{churchId}/transaction-categories", churchId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"name": "Offerings", "type": "INCOME"}"""),
            ).andExpect(status().isCreated)
            .andExpect(jsonPath("$.name").value("Offerings"))
            .andExpect(jsonPath("$.type").value(FinancialTransactionType.INCOME.name))
            .andExpect(jsonPath("$.status").value("ACTIVE"))
            .andExpect(jsonPath("$.id").exists())
            .andExpect(jsonPath("$.version").value(0))
    }

    @Test
    fun `POST should return 409 when name already exists for the same type in the same church`() {
        val churchId = UUID.fromString(createChurch())
        postCategory(churchId, "Tithes", "INCOME").andExpect(status().isCreated)

        postCategory(churchId, "Tithes", "INCOME")
            .andExpect(status().isConflict)
            .andExpect(jsonPath("$.message").exists())
    }

    @Test
    fun `POST should return 201 when name exists but for a different type`() {
        val churchId = UUID.fromString(createChurch())
        postCategory(churchId, "Tithes", "INCOME").andExpect(status().isCreated)

        postCategory(churchId, "Tithes", "EXPENDITURE").andExpect(status().isCreated)
    }

    @Test
    fun `GET by id returns the category`() {
        val churchId = UUID.fromString(createChurch())
        val id = extractId(createCategoryAndReturnBody(churchId, "Tithes", "INCOME"))

        mockMvc
            .perform(get("/api/v1/churches/{churchId}/transaction-categories/{id}", churchId, id))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.id").value(id))
            .andExpect(jsonPath("$.name").value("Tithes"))
            .andExpect(jsonPath("$.type").value("INCOME"))
            .andExpect(jsonPath("$.status").value("ACTIVE"))
    }

    @Test
    fun `GET by id returns 404 when category does not exist`() {
        val churchId = UUID.fromString(createChurch())
        val unknownId = UUID.randomUUID()

        mockMvc
            .perform(get("/api/v1/churches/{churchId}/transaction-categories/{id}", churchId, unknownId))
            .andExpect(status().isNotFound)
    }

    @Test
    fun `GET by id returns 404 when category belongs to a different church`() {
        val churchOne = UUID.fromString(createChurch("Church One"))
        val churchTwo = UUID.fromString(createChurch("Church Two"))
        val id = extractId(createCategoryAndReturnBody(churchOne, "Tithes", "INCOME"))

        mockMvc
            .perform(get("/api/v1/churches/{churchId}/transaction-categories/{id}", churchTwo, id))
            .andExpect(status().isNotFound)
    }

    @Test
    fun `GET list returns only ACTIVE categories by default`() {
        val churchId = UUID.fromString(createChurch())
        val activeId = extractId(createCategoryAndReturnBody(churchId, "Tithes", "INCOME"))
        val toDeactivateId = extractId(createCategoryAndReturnBody(churchId, "Old", "INCOME"))

        mockMvc
            .perform(
                patch("/api/v1/churches/{churchId}/transaction-categories/{id}/deactivate", churchId, toDeactivateId)
                    .param("version", "0"),
            ).andExpect(status().isOk)

        mockMvc
            .perform(get("/api/v1/churches/{churchId}/transaction-categories", churchId))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].id").value(activeId))
    }

    @Test
    fun `GET list returns empty list when church has no categories`() {
        val churchId = UUID.fromString(createChurch())

        mockMvc
            .perform(get("/api/v1/churches/{churchId}/transaction-categories", churchId))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(0))
    }

    @Test
    fun `GET list returns 404 when church does not exist`() {
        val unknownChurchId = UUID.randomUUID()

        mockMvc
            .perform(get("/api/v1/churches/{churchId}/transaction-categories", unknownChurchId))
            .andExpect(status().isNotFound)
    }

    @Test
    fun `GET list with type=INCOME filters correctly`() {
        val churchId = UUID.fromString(createChurch())
        createCategoryAndReturnBody(churchId, "Tithes", "INCOME")
        createCategoryAndReturnBody(churchId, "Bills", "EXPENDITURE")

        mockMvc
            .perform(
                get("/api/v1/churches/{churchId}/transaction-categories", churchId)
                    .param("type", "INCOME"),
            ).andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].name").value("Tithes"))
    }

    @Test
    fun `PUT updates name and returns 200`() {
        val churchId = UUID.fromString(createChurch())
        val id = extractId(createCategoryAndReturnBody(churchId, "Old", "INCOME"))

        mockMvc
            .perform(
                put("/api/v1/churches/{churchId}/transaction-categories/{id}", churchId, id)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"name": "New", "version": 0}"""),
            ).andExpect(status().isOk)
            .andExpect(jsonPath("$.name").value("New"))
    }

    @Test
    fun `PUT returns 400 on blank name`() {
        val churchId = UUID.fromString(createChurch())
        val id = extractId(createCategoryAndReturnBody(churchId, "Old", "INCOME"))

        mockMvc
            .perform(
                put("/api/v1/churches/{churchId}/transaction-categories/{id}", churchId, id)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"name": "", "version": 0}"""),
            ).andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.message").exists())
    }

    @Test
    fun `PUT returns 404 on unknown id`() {
        val churchId = UUID.fromString(createChurch())
        val unknownId = UUID.randomUUID()

        mockMvc
            .perform(
                put("/api/v1/churches/{churchId}/transaction-categories/{id}", churchId, unknownId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"name": "Anything", "version": 0}"""),
            ).andExpect(status().isNotFound)
    }

    @Test
    fun `PUT returns 404 on tenant mismatch`() {
        val churchOne = UUID.fromString(createChurch("Church One"))
        val churchTwo = UUID.fromString(createChurch("Church Two"))
        val id = extractId(createCategoryAndReturnBody(churchOne, "Tithes", "INCOME"))

        mockMvc
            .perform(
                put("/api/v1/churches/{churchId}/transaction-categories/{id}", churchTwo, id)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"name": "Renamed", "version": 0}"""),
            ).andExpect(status().isNotFound)
    }

    @Test
    fun `PUT returns 409 when stale version`() {
        val churchId = UUID.fromString(createChurch())
        val id = extractId(createCategoryAndReturnBody(churchId, "Old", "INCOME"))

        mockMvc
            .perform(
                put("/api/v1/churches/{churchId}/transaction-categories/{id}", churchId, id)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"name": "First Update", "version": 0}"""),
            ).andExpect(status().isOk)

        mockMvc
            .perform(
                put("/api/v1/churches/{churchId}/transaction-categories/{id}", churchId, id)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"name": "Stale Update", "version": 0}"""),
            ).andExpect(status().isConflict)
            .andExpect(jsonPath("$.message").value("Someone else has made a change. Please refresh and try again."))
    }

    @Test
    fun `PUT returns 409 when new name collides with another category of the same type`() {
        val churchId = UUID.fromString(createChurch())
        createCategoryAndReturnBody(churchId, "Taken", "INCOME")
        val id = extractId(createCategoryAndReturnBody(churchId, "Other", "INCOME"))

        mockMvc
            .perform(
                put("/api/v1/churches/{churchId}/transaction-categories/{id}", churchId, id)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"name": "Taken", "version": 0}"""),
            ).andExpect(status().isConflict)
            .andExpect(jsonPath("$.message").exists())
    }

    @Test
    fun `GET list sorts by name ascending`() {
        val churchId = UUID.fromString(createChurch())
        createCategoryAndReturnBody(churchId, "Zebra", "INCOME")
        createCategoryAndReturnBody(churchId, "Apple", "INCOME")
        createCategoryAndReturnBody(churchId, "Mango", "INCOME")

        mockMvc
            .perform(get("/api/v1/churches/{churchId}/transaction-categories", churchId))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$[0].name").value("Apple"))
            .andExpect(jsonPath("$[1].name").value("Mango"))
            .andExpect(jsonPath("$[2].name").value("Zebra"))
    }

    @Test
    fun `PATCH deactivate sets status to INACTIVE`() {
        val churchId = UUID.fromString(createChurch())
        val id = extractId(createCategoryAndReturnBody(churchId, "X", "INCOME"))

        mockMvc
            .perform(
                patch("/api/v1/churches/{churchId}/transaction-categories/{id}/deactivate", churchId, id)
                    .param("version", "0"),
            ).andExpect(status().isOk)
            .andExpect(jsonPath("$.status").value("INACTIVE"))
    }

    @Test
    fun `PATCH activate sets status to ACTIVE`() {
        val churchId = UUID.fromString(createChurch())
        val id = extractId(createCategoryAndReturnBody(churchId, "X", "INCOME"))

        val deactivateBody =
            mockMvc
                .perform(
                    patch("/api/v1/churches/{churchId}/transaction-categories/{id}/deactivate", churchId, id)
                        .param("version", "0"),
                ).andExpect(status().isOk)
                .andReturn()
                .response.contentAsString

        val newVersion = objectMapper.readTree(deactivateBody).get("version").asString()

        mockMvc
            .perform(
                patch("/api/v1/churches/{churchId}/transaction-categories/{id}/activate", churchId, id)
                    .param("version", newVersion),
            ).andExpect(status().isOk)
            .andExpect(jsonPath("$.status").value("ACTIVE"))
    }

    @Test
    fun `PATCH deactivate returns 404 on unknown id`() {
        val churchId = UUID.fromString(createChurch())
        val unknownId = UUID.randomUUID()

        mockMvc
            .perform(
                patch("/api/v1/churches/{churchId}/transaction-categories/{id}/deactivate", churchId, unknownId)
                    .param("version", "0"),
            ).andExpect(status().isNotFound)
    }

    @Test
    fun `PATCH deactivate returns 404 on tenant mismatch`() {
        val churchOne = UUID.fromString(createChurch("Church One"))
        val churchTwo = UUID.fromString(createChurch("Church Two"))
        val id = extractId(createCategoryAndReturnBody(churchOne, "X", "INCOME"))

        mockMvc
            .perform(
                patch("/api/v1/churches/{churchId}/transaction-categories/{id}/deactivate", churchTwo, id)
                    .param("version", "0"),
            ).andExpect(status().isNotFound)
    }

    @Test
    fun `PATCH activate returns 404 on unknown id`() {
        val churchId = UUID.fromString(createChurch())
        val unknownId = UUID.randomUUID()

        mockMvc
            .perform(
                patch("/api/v1/churches/{churchId}/transaction-categories/{id}/activate", churchId, unknownId)
                    .param("version", "0"),
            ).andExpect(status().isNotFound)
    }

    @Test
    fun `PATCH activate returns 404 on tenant mismatch`() {
        val churchOne = UUID.fromString(createChurch("Church One"))
        val churchTwo = UUID.fromString(createChurch("Church Two"))
        val id = extractId(createCategoryAndReturnBody(churchOne, "X", "INCOME"))

        mockMvc
            .perform(
                patch("/api/v1/churches/{churchId}/transaction-categories/{id}/activate", churchTwo, id)
                    .param("version", "0"),
            ).andExpect(status().isNotFound)
    }

    @Test
    fun `Deactivated category is excluded from default list but included with status=ALL`() {
        val churchId = UUID.fromString(createChurch())
        val activeId = extractId(createCategoryAndReturnBody(churchId, "Active", "INCOME"))
        val toDeactivateId = extractId(createCategoryAndReturnBody(churchId, "Hidden", "INCOME"))

        mockMvc
            .perform(
                patch("/api/v1/churches/{churchId}/transaction-categories/{id}/deactivate", churchId, toDeactivateId)
                    .param("version", "0"),
            ).andExpect(status().isOk)

        mockMvc
            .perform(get("/api/v1/churches/{churchId}/transaction-categories", churchId))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].id").value(activeId))

        mockMvc
            .perform(
                get("/api/v1/churches/{churchId}/transaction-categories", churchId)
                    .param("status", "ALL"),
            ).andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(2))
    }
}
