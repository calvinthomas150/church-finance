package com.calvintech.churchfinance.shared.api

import com.calvintech.churchfinance.administration.domain.ChurchNotFoundException
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice
class GlobalExceptionHandler {
    @ExceptionHandler(ChurchNotFoundException::class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    fun handleChurchNotFound(ex: ChurchNotFoundException): ErrorResponse = ErrorResponse(ex.message ?: "Church not found")
}
