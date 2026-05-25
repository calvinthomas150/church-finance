package com.calvintech.churchfinance.administration.api

import com.calvintech.churchfinance.administration.service.TransactionCategoryService
import com.calvintech.churchfinance.shared.domain.FinancialTransactionType
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
@RequestMapping("/api/v1/churches/{churchId}/transaction-categories")
@Tag(
    name = "Transaction Categories",
    description =
        "Manage transaction categories used to classify financial transactions " +
            "(e.g. tithes, offerings, utilities). Categories are scoped to a church.",
)
class TransactionCategoryController(
    private val transactionCategoryService: TransactionCategoryService,
) {
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
        summary = "Create a transaction category",
        description = "Creates a new transaction category for the specified church.",
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "201", description = "Transaction category created"),
            ApiResponse(responseCode = "400", description = "Validation failure", content = []),
            ApiResponse(responseCode = "404", description = "Church not found", content = []),
        ],
    )
    fun create(
        @Parameter(description = "Identifier of the church the category belongs to", required = true)
        @PathVariable churchId: UUID,
        @Valid @RequestBody request: CreateTransactionCategoryRequest,
    ): TransactionCategoryResponse = transactionCategoryService.create(churchId, request)

    @GetMapping
    @Operation(
        summary = "List transaction categories for a church",
        description =
            "Returns categories for the supplied church. Defaults to ACTIVE only; use `status=ALL` or " +
                "`status=INACTIVE` to broaden, and `type=INCOME` or `type=EXPENDITURE` to narrow.",
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Categories returned"),
            ApiResponse(responseCode = "404", description = "Church not found", content = []),
        ],
    )
    fun list(
        @Parameter(description = "Identifier of the church", required = true)
        @PathVariable churchId: UUID,
        @Parameter(description = "Status filter (default ACTIVE)")
        @RequestParam(defaultValue = "ACTIVE") status: TransactionCategoryStatusFilter,
        @Parameter(description = "Optional transaction type filter")
        @RequestParam(required = false) type: FinancialTransactionType?,
    ): List<TransactionCategoryResponse> = transactionCategoryService.list(churchId, status, type)

    @GetMapping("/{id}")
    @Operation(
        summary = "Get a transaction category by id",
        description = "Returns the transaction category identified by its UUID, scoped to the supplied church.",
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Transaction category found"),
            ApiResponse(responseCode = "404", description = "Transaction category not found", content = []),
        ],
    )
    fun get(
        @Parameter(description = "Identifier of the church the category belongs to", required = true)
        @PathVariable churchId: UUID,
        @Parameter(description = "Identifier of the transaction category", required = true)
        @PathVariable id: UUID,
    ): TransactionCategoryResponse = transactionCategoryService.get(churchId, id)

    @PutMapping("/{id}")
    @Operation(
        summary = "Update a transaction category",
        description = "Updates the category's name. Type is immutable. Requires the current `version` for optimistic locking.",
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Transaction category updated"),
            ApiResponse(responseCode = "400", description = "Validation failure", content = []),
            ApiResponse(responseCode = "404", description = "Transaction category not found", content = []),
            ApiResponse(responseCode = "409", description = "Name conflict or optimistic locking conflict", content = []),
        ],
    )
    fun update(
        @Parameter(description = "Identifier of the church", required = true)
        @PathVariable churchId: UUID,
        @Parameter(description = "Identifier of the transaction category", required = true)
        @PathVariable id: UUID,
        @Valid @RequestBody request: UpdateTransactionCategoryRequest,
    ): TransactionCategoryResponse = transactionCategoryService.update(churchId, id, request)

    @PatchMapping("/{id}/deactivate")
    @Operation(
        summary = "Deactivate a transaction category",
        description = "Marks the category as INACTIVE. Requires the current `version` for optimistic locking.",
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Category deactivated"),
            ApiResponse(responseCode = "400", description = "Missing required `version` parameter", content = []),
            ApiResponse(responseCode = "404", description = "Transaction category not found", content = []),
            ApiResponse(responseCode = "409", description = "Optimistic locking conflict", content = []),
        ],
    )
    fun deactivate(
        @Parameter(description = "Identifier of the church", required = true)
        @PathVariable churchId: UUID,
        @Parameter(description = "Identifier of the transaction category", required = true)
        @PathVariable id: UUID,
        @Parameter(description = "Current version for optimistic locking", required = true)
        @RequestParam version: Long,
    ): TransactionCategoryResponse = transactionCategoryService.deactivate(churchId, id, version)

    @PatchMapping("/{id}/activate")
    @Operation(
        summary = "Activate a transaction category",
        description = "Marks the category as ACTIVE. Requires the current `version` for optimistic locking.",
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Category activated"),
            ApiResponse(responseCode = "400", description = "Missing required `version` parameter", content = []),
            ApiResponse(responseCode = "404", description = "Transaction category not found", content = []),
            ApiResponse(responseCode = "409", description = "Optimistic locking conflict", content = []),
        ],
    )
    fun activate(
        @Parameter(description = "Identifier of the church", required = true)
        @PathVariable churchId: UUID,
        @Parameter(description = "Identifier of the transaction category", required = true)
        @PathVariable id: UUID,
        @Parameter(description = "Current version for optimistic locking", required = true)
        @RequestParam version: Long,
    ): TransactionCategoryResponse = transactionCategoryService.activate(churchId, id, version)
}
