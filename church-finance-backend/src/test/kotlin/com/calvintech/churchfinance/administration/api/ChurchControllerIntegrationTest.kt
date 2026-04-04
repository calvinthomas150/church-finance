package com.calvintech.churchfinance.administration.api

import com.calvintech.churchfinance.TestcontainersConfiguration
import com.calvintech.churchfinance.administration.domain.ChurchStatus
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
class ChurchControllerIntegrationTest {
    @Autowired
    lateinit var mockMvc: MockMvc

    @Autowired
    lateinit var objectMapper: ObjectMapper

    private fun extractId(responseBody: String): String = objectMapper.readTree(responseBody).get("id").asString()

    private fun extractVersion(responseBody: String): String = objectMapper.readTree(responseBody).get("version").asString()

    private fun createChurch(name: String = "Grace Church"): String {
        val result =
            mockMvc
                .perform(
                    post("/api/v1/churches")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""{"name": "$name"}"""),
                ).andExpect(status().isCreated)
                .andReturn()

        return result.response.contentAsString
    }

    @Test
    fun `POST should create a church and return 201`() {
        mockMvc
            .perform(
                post("/api/v1/churches")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"name": "Grace Church"}"""),
            ).andExpect(status().isCreated)
            .andExpect(jsonPath("$.name").value("Grace Church"))
            .andExpect(jsonPath("$.status").value(ChurchStatus.ACTIVE.name))
            .andExpect(jsonPath("$.id").exists())
            .andExpect(jsonPath("$.version").value(0))
    }

    @Test
    fun `POST should return 400 when name is blank`() {
        mockMvc
            .perform(
                post("/api/v1/churches")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"name": ""}"""),
            ).andExpect(status().isBadRequest)
    }

    @Test
    fun `POST should return 400 when name is missing`() {
        mockMvc
            .perform(
                post("/api/v1/churches")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{}"""),
            ).andExpect(status().isBadRequest)
    }

    @Test
    fun `GET should return an existing church`() {
        val body = createChurch("Grace Church")
        val id = extractId(body)

        mockMvc
            .perform(get("/api/v1/churches/$id"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.id").value(id))
            .andExpect(jsonPath("$.name").value("Grace Church"))
            .andExpect(jsonPath("$.status").value(ChurchStatus.ACTIVE.name))
    }

    @Test
    fun `GET should return error for unknown id`() {
        val unknownId = UUID.randomUUID()

        mockMvc
            .perform(get("/api/v1/churches/$unknownId"))
            .andExpect(status().isNotFound)
    }

    @Test
    fun `PUT should update the church name`() {
        val body = createChurch("Grace Church")
        val id = extractId(body)

        mockMvc
            .perform(
                put("/api/v1/churches/$id")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"name": "New Life Church", "version": 0}"""),
            ).andExpect(status().isOk)
            .andExpect(jsonPath("$.id").value(id))
            .andExpect(jsonPath("$.name").value("New Life Church"))
    }

    @Test
    fun `PUT should return 400 when name is blank`() {
        val body = createChurch("Grace Church")
        val id = extractId(body)

        mockMvc
            .perform(
                put("/api/v1/churches/$id")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"name": "", "version": 0}"""),
            ).andExpect(status().isBadRequest)
    }

    @Test
    fun `PUT should return error for unknown id`() {
        val unknownId = UUID.randomUUID()

        mockMvc
            .perform(
                put("/api/v1/churches/$unknownId")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"name": "New Life Church", "version": 0}"""),
            ).andExpect(status().isNotFound)
    }

    @Test
    fun `PATCH deactivate should set status to INACTIVE`() {
        val body = createChurch("Grace Church")
        val id = extractId(body)
        val version = extractVersion(body)

        mockMvc
            .perform(patch("/api/v1/churches/$id/deactivate?version=$version"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.id").value(id))
            .andExpect(jsonPath("$.status").value(ChurchStatus.INACTIVE.name))
    }

    @Test
    fun `PATCH deactivate should return error for unknown id`() {
        val unknownId = UUID.randomUUID()
        val version = 0

        mockMvc
            .perform(patch("/api/v1/churches/$unknownId/deactivate?version=$version"))
            .andExpect(status().isNotFound)
    }

    @Test
    fun `PATCH activate should set status to ACTIVE`() {
        val body = createChurch("Grace Church")
        val id = extractId(body)

        val deactivateResult = mockMvc.perform(patch("/api/v1/churches/$id/deactivate?version=0")).andReturn()

        val version = extractVersion(deactivateResult.response.contentAsString)

        mockMvc
            .perform(patch("/api/v1/churches/$id/activate?version=$version"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.id").value(id))
            .andExpect(jsonPath("$.status").value(ChurchStatus.ACTIVE.name))
    }

    @Test
    fun `PATCH activate should return error for unknown id`() {
        val unknownId = UUID.randomUUID()
        val version = 0

        mockMvc
            .perform(patch("/api/v1/churches/$unknownId/activate?version=$version"))
            .andExpect(status().isNotFound)
    }
}
