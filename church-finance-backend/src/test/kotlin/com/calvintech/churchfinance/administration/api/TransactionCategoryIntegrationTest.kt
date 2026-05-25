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
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
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
        mockMvc.perform(
            post("/api/v1/churches/{churchId}/transaction-categories", churchId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"name": "$name", "type": "$type"}"""),
        ).andExpect(status().isCreated)
            .andReturn().response.contentAsString

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
}
