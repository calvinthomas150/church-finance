package com.calvintech.churchfinance.administration.service

import com.calvintech.churchfinance.administration.api.ChurchResponse
import com.calvintech.churchfinance.administration.api.CreateChurchRequest
import com.calvintech.churchfinance.administration.api.UpdateChurchRequest
import com.calvintech.churchfinance.administration.domain.Church
import com.calvintech.churchfinance.administration.domain.ChurchNotFoundException
import com.calvintech.churchfinance.administration.domain.ChurchStatus
import com.calvintech.churchfinance.administration.persistence.ChurchMapper
import com.calvintech.churchfinance.administration.persistence.ChurchRepository
import com.calvintech.churchfinance.shared.service.CurrentUserProvider
import com.github.f4b6a3.ulid.UlidCreator
import org.springframework.stereotype.Service
import java.time.Instant
import java.util.UUID

@Service
class ChurchService(
    val churchMapper: ChurchMapper,
    val churchRepository: ChurchRepository,
    val userProvider: CurrentUserProvider,
) {
    fun create(createChurchRequest: CreateChurchRequest): ChurchResponse {
        val church =
            Church(
                id = UlidCreator.getUlid(),
                createdAt = Instant.now(),
                addedBy = userProvider.getCurrentUserId(),
                name = createChurchRequest.name,
                status = ChurchStatus.ACTIVE,
                version = 0L,
            )

        return saveAndRespond(church)
    }

    private fun toResponse(church: Church): ChurchResponse =
        ChurchResponse(
            id = church.id.toUuid(),
            name = church.name,
            status = church.status,
            version = church.version,
        )

    fun get(id: UUID): ChurchResponse {
        val church = findChurch(id)
        return toResponse(church)
    }

    fun update(
        id: UUID,
        updateChurchRequest: UpdateChurchRequest,
    ): ChurchResponse {
        val church = findChurch(id)
        val updated = church.copy(name = updateChurchRequest.name, version = updateChurchRequest.version)
        return saveAndRespond(updated)
    }

    fun deactivate(id: UUID): ChurchResponse {
        val church = findChurch(id)
        val updated = church.copy(status = ChurchStatus.INACTIVE)
        return saveAndRespond(updated)
    }

    fun activate(id: UUID): ChurchResponse {
        val church = findChurch(id)
        val updated = church.copy(status = ChurchStatus.ACTIVE)
        return saveAndRespond(updated)
    }

    private fun findChurch(id: UUID): Church =
        churchRepository
            .findById(id)
            .orElseThrow { ChurchNotFoundException(id) }
            .let { churchMapper.toDomain(it) }

    private fun saveAndRespond(church: Church): ChurchResponse {
        val persisted = churchRepository.save(churchMapper.toJpaEntity(church))
        return toResponse(churchMapper.toDomain(persisted))
    }
}
