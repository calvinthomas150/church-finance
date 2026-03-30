package com.calvintech.churchfinance.infrastructure

import com.calvintech.churchfinance.TestcontainersConfiguration
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.testcontainers.context.ImportTestcontainers
import javax.sql.DataSource
import kotlin.test.Test
import kotlin.test.assertTrue

@SpringBootTest
@ImportTestcontainers(TestcontainersConfiguration::class)
class DatabaseInfrastructureTest {
    @Autowired
    lateinit var dataSource: DataSource

    @Test
    fun `should create church_finance schema on startup`() {
        dataSource.connection.use { conn ->
            conn.metaData.getSchemas(null, "church_finance").use { rs ->
                assertTrue(rs.next(), "church_finance schema should exist")
            }
        }
    }
}
