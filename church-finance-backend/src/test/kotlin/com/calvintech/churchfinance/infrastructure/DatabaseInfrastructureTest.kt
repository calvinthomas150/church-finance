package com.calvintech.churchfinance.infrastructure

import com.calvintech.churchfinance.TestcontainersConfiguration
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.testcontainers.context.ImportTestcontainers
import javax.sql.DataSource

@SpringBootTest
@ImportTestcontainers(TestcontainersConfiguration::class)
class DatabaseInfrastructureTest {
    @Autowired
    lateinit var dataSource: DataSource

    @Test
    fun `should start application context and run migrations`() {
        dataSource.connection.use { conn ->
            conn.metaData.getSchemas(null, "church_finance").use { rs ->
                assertTrue(rs.next(), "church_finance schema should exist")
            }
        }
    }
}
