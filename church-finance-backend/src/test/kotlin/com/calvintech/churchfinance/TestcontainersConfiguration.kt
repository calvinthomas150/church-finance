package com.calvintech.churchfinance

import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.postgresql.PostgreSQLContainer
import org.testcontainers.utility.DockerImageName

class TestcontainersConfiguration {
    companion object {
        @Container
        @ServiceConnection
        val postgresContainer = PostgreSQLContainer(DockerImageName.parse("postgres:18-alpine"))
    }
}
