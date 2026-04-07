package com.calvintech.churchfinance.administration.api

import com.calvintech.churchfinance.administration.service.ChurchService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/v1/churches")
@Tag(
    name = "Churches",
    description =
        "Manage churches (tenants) within the system. A church is the top-level " +
            "tenant boundary; almost all other resources are scoped to a church.",
)
class ChurchController(
    private val churchService: ChurchService,
) {
    @GetMapping("/{id}")
    @Operation(
        summary = "Get a church by id",
        description = "Returns the church identified by the supplied UUID.",
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Church found"),
            ApiResponse(responseCode = "404", description = "Church not found", content = []),
        ],
    )
    fun get(
        @Parameter(description = "Identifier of the church", required = true)
        @PathVariable id: UUID,
    ): ChurchResponse = churchService.get(id)

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
        summary = "Create a church",
        description = "Registers a new church (tenant). The created church is returned in an `ACTIVE` state.",
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "201", description = "Church created"),
            ApiResponse(responseCode = "400", description = "Validation failure", content = []),
        ],
    )
    fun create(
        @Valid @RequestBody request: CreateChurchRequest,
    ): ChurchResponse = churchService.create(request)

    @PutMapping("/{id}")
    @Operation(
        summary = "Update a church",
        description =
            "Updates a church's mutable attributes. Optimistic locking is enforced via the " +
                "`version` field on the request body — supply the current version returned by the most recent read.",
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Church updated"),
            ApiResponse(responseCode = "400", description = "Validation failure", content = []),
            ApiResponse(responseCode = "404", description = "Church not found", content = []),
            ApiResponse(responseCode = "409", description = "Optimistic locking conflict", content = []),
        ],
    )
    fun update(
        @Parameter(description = "Identifier of the church", required = true)
        @PathVariable id: UUID,
        @Valid @RequestBody request: UpdateChurchRequest,
    ): ChurchResponse = churchService.update(id, request)

    @PatchMapping("/{id}/deactivate")
    @Operation(
        summary = "Deactivate a church",
        description = "Marks the church as inactive. Requires the current `version` for optimistic locking.",
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Church deactivated"),
            ApiResponse(responseCode = "404", description = "Church not found", content = []),
            ApiResponse(responseCode = "409", description = "Optimistic locking conflict", content = []),
        ],
    )
    fun deactivate(
        @Parameter(description = "Identifier of the church", required = true)
        @PathVariable id: UUID,
        @Parameter(description = "Current version of the church for optimistic locking", required = true)
        @RequestParam version: Long,
    ): ChurchResponse = churchService.deactivate(id, version)

    @PatchMapping("/{id}/activate")
    @Operation(
        summary = "Activate a church",
        description = "Reactivates a previously deactivated church. Requires the current `version` for optimistic locking.",
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Church activated"),
            ApiResponse(responseCode = "404", description = "Church not found", content = []),
            ApiResponse(responseCode = "409", description = "Optimistic locking conflict", content = []),
        ],
    )
    fun activate(
        @Parameter(description = "Identifier of the church", required = true)
        @PathVariable id: UUID,
        @Parameter(description = "Current version of the church for optimistic locking", required = true)
        @RequestParam version: Long,
    ): ChurchResponse = churchService.activate(id, version)
}
