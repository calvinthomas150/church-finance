package com.calvintech.churchfinance.administration.api

import com.calvintech.churchfinance.TestcontainersConfiguration
import com.calvintech.churchfinance.shared.domain.FinancialTransactionType
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.testcontainers.context.ImportTestcontainers
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
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

    private fun createChurch(name: String = "Grace Church"): String {
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
    fun `POST should create a transaction category and return 201`() {
        val churchId = UUID.fromString(createChurch())
        mockMvc
            .perform(
                post("/api/v1/transaction-categories/{id}", churchId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"name": "Offerings", "type": "INCOME"}"""),
            ).andExpect(status().isCreated)
            .andExpect(jsonPath("$.name").value("Offerings"))
            .andExpect(jsonPath("$.type").value(FinancialTransactionType.INCOME.name))
            .andExpect(jsonPath("$.id").exists())
            .andExpect(jsonPath("$.version").value(0))
    }
}
