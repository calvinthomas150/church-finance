package com.calvintech.churchfinance.shared.api

import com.calvintech.churchfinance.administration.domain.ChurchNotFoundException
import org.springframework.http.HttpStatus
import org.springframework.orm.ObjectOptimisticLockingFailureException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice
class GlobalExceptionHandler {
    @ExceptionHandler(ChurchNotFoundException::class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    fun handleChurchNotFound(ex: ChurchNotFoundException): ErrorResponse = ErrorResponse(ex.message ?: "Church not found")

    @ExceptionHandler(MethodArgumentNotValidException::class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    fun handleValidation(ex: MethodArgumentNotValidException): ErrorResponse {
        val message =
            ex.bindingResult.fieldErrors
                .joinToString("; ") { "${it.field}: ${it.defaultMessage}" }
                .ifBlank { "Validation failed" }
        return ErrorResponse(message)
    }

    @ExceptionHandler(ObjectOptimisticLockingFailureException::class)
    @ResponseStatus(HttpStatus.CONFLICT)
    fun handleOptimisticLock(): ErrorResponse = ErrorResponse("Someone else has made a change. Please refresh and try again.")
}
