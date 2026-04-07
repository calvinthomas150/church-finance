package com.calvintech.churchfinance.administration.api

import com.calvintech.churchfinance.administration.service.TransactionCategoryService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
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
}
