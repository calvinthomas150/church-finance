package com.calvintech.churchfinance

import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.testcontainers.context.ImportTestcontainers

@ImportTestcontainers(TestcontainersConfiguration::class)
@SpringBootTest
class ChurchFinanceApplicationTests {
    @Test
    fun contextLoads() {
    }
}
