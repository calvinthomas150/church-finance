package com.calvintech.churchfinance.shared.api

import org.springdoc.core.models.GroupedOpenApi
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * Groups OpenAPI operations by DDD aggregate. Each group surfaces as a separate
 * entry in the Swagger UI "Select a definition" dropdown and is also exposed at
 * `/v3/api-docs/<group>` for per-aggregate client codegen.
 *
 * Add a new bean here whenever a new aggregate exposes its first endpoint —
 * one line per aggregate, matched by package pattern.
 */
@Configuration
class OpenApiConfig {
    @Bean
    fun allApi(): GroupedOpenApi =
        GroupedOpenApi
            .builder()
            .group("all")
            .displayName("All APIs")
            .packagesToScan("com.calvintech.churchfinance")
            .build()

    @Bean
    fun administrationApi(): GroupedOpenApi =
        GroupedOpenApi
            .builder()
            .group("administration")
            .displayName("Administration")
            .packagesToScan("com.calvintech.churchfinance.administration")
            .build()
}
