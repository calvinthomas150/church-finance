package com.calvintech.churchfinance.administration.api

import com.calvintech.churchfinance.administration.service.ChurchService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/v1/churches")
class ChurchController(
    val churchService: ChurchService,
) {
    @GetMapping("/{id}")
    fun get(
        @PathVariable id: UUID,
    ): ChurchResponse = churchService.get(id)

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun create(
        @Valid @RequestBody request: CreateChurchRequest,
    ): ChurchResponse = churchService.create(request)

    @PutMapping("/{id}")
    fun update(
        @PathVariable id: UUID,
        @Valid @RequestBody request: UpdateChurchRequest,
    ): ChurchResponse = churchService.update(id, request)

    @PatchMapping("/{id}/deactivate")
    fun deactivate(
        @PathVariable id: UUID,
    ): ChurchResponse = churchService.deactivate(id)

    @PatchMapping("/{id}/activate")
    fun activate(
        @PathVariable id: UUID,
    ): ChurchResponse = churchService.activate(id)
}
