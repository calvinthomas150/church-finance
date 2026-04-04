package com.calvintech.churchfinance.administration.service

import com.calvintech.churchfinance.administration.api.CreateChurchRequest
import com.calvintech.churchfinance.administration.api.UpdateChurchRequest
import com.calvintech.churchfinance.administration.domain.Church
import com.calvintech.churchfinance.administration.domain.ChurchNotFoundException
import com.calvintech.churchfinance.administration.domain.ChurchStatus
import com.calvintech.churchfinance.administration.persistence.ChurchJpaEntity
import com.calvintech.churchfinance.administration.persistence.ChurchMapper
import com.calvintech.churchfinance.administration.persistence.ChurchRepository
import com.calvintech.churchfinance.shared.service.CurrentUserProvider
import com.github.f4b6a3.ulid.UlidCreator
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.slot
import java.time.Instant
import java.util.Optional
import java.util.UUID
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull

class ChurchServiceTest {
    @MockK
    lateinit var churchRepository: ChurchRepository

    @MockK
    lateinit var churchMapper: ChurchMapper

    @MockK
    lateinit var userProvider: CurrentUserProvider

    lateinit var churchService: ChurchService

    @BeforeTest
    fun setUp() {
        MockKAnnotations.init(this)
        churchService = ChurchService(churchMapper, churchRepository, userProvider)
    }

    private fun buildChurch(
        name: String = "Grace Church",
        status: ChurchStatus = ChurchStatus.ACTIVE,
        version: Long = 0L,
    ): Church =
        Church(
            id = UlidCreator.getUlid(),
            createdAt = Instant.now(),
            addedBy = UlidCreator.getUlid(),
            name = name,
            status = status,
            version = version,
        )

    private fun buildChurchJpaEntity(
        id: UUID = UUID.randomUUID(),
        name: String = "Grace Church",
        status: ChurchStatus = ChurchStatus.ACTIVE,
        version: Long = 0L,
    ): ChurchJpaEntity =
        ChurchJpaEntity(
            id = id,
            createdAt = Instant.now(),
            addedBy = UUID.randomUUID(),
            name = name,
            status = status,
            version = version,
        )

    @Test
    fun `create should persist and return a new church`() {
        val currentUserId = UlidCreator.getUlid()
        val churchSlot = slot<Church>()
        val savedEntity = buildChurchJpaEntity(name = "Grace Church")
        val savedChurch = buildChurch(name = "Grace Church")

        every { userProvider.getCurrentUserId() } returns currentUserId
        every { churchMapper.toJpaEntity(capture(churchSlot)) } returns savedEntity
        every { churchRepository.save(savedEntity) } returns savedEntity
        every { churchMapper.toDomain(savedEntity) } returns savedChurch

        val response = churchService.create(CreateChurchRequest(name = "Grace Church"))

        assertEquals("Grace Church", response.name)
        assertEquals(ChurchStatus.ACTIVE, response.status)
        assertNotNull(response.id)
        assertEquals(currentUserId, churchSlot.captured.addedBy)
    }

    @Test
    fun `get should return an existing church`() {
        val church = buildChurch(name = "Grace Church")
        val jpaEntity = buildChurchJpaEntity(id = church.id.toUuid(), name = "Grace Church")

        every { churchRepository.findById(church.id.toUuid()) } returns Optional.of(jpaEntity)
        every { churchMapper.toDomain(jpaEntity) } returns church

        val response = churchService.get(church.id.toUuid())

        assertEquals(church.id.toUuid(), response.id)
        assertEquals("Grace Church", response.name)
        assertEquals(ChurchStatus.ACTIVE, response.status)
    }

    @Test
    fun `get should throw ChurchNotFoundException for unknown id`() {
        val unknownId = UUID.randomUUID()

        every { churchRepository.findById(unknownId) } returns Optional.empty()

        assertFailsWith<ChurchNotFoundException> {
            churchService.get(unknownId)
        }
    }

    @Test
    fun `update should change the church name`() {
        val church = buildChurch(name = "Grace Church", version = 1L)
        val jpaEntity = buildChurchJpaEntity(id = church.id.toUuid(), name = "Grace Church", version = 1L)
        val updatedChurch = church.copy(name = "New Life Church", version = 1L)
        val updatedJpaEntity = buildChurchJpaEntity(id = church.id.toUuid(), name = "New Life Church", version = 1L)

        every { churchRepository.findById(church.id.toUuid()) } returns Optional.of(jpaEntity)
        every { churchMapper.toDomain(jpaEntity) } returns church
        every { churchMapper.toJpaEntity(updatedChurch) } returns updatedJpaEntity
        every { churchRepository.save(updatedJpaEntity) } returns updatedJpaEntity
        every { churchMapper.toDomain(updatedJpaEntity) } returns updatedChurch

        val response =
            churchService.update(
                church.id.toUuid(),
                UpdateChurchRequest(name = "New Life Church", version = 1L),
            )

        assertEquals(church.id.toUuid(), response.id)
        assertEquals("New Life Church", response.name)
    }

    @Test
    fun `update should throw ChurchNotFoundException for unknown id`() {
        val unknownId = UUID.randomUUID()

        every { churchRepository.findById(unknownId) } returns Optional.empty()

        assertFailsWith<ChurchNotFoundException> {
            churchService.update(unknownId, UpdateChurchRequest(name = "New Life Church", version = 0L))
        }
    }

    @Test
    fun `deactivate should set status to INACTIVE`() {
        val church = buildChurch(name = "Grace Church")
        val jpaEntity = buildChurchJpaEntity(id = church.id.toUuid(), name = "Grace Church")
        val deactivatedChurch = church.copy(status = ChurchStatus.INACTIVE)
        val deactivatedJpaEntity = buildChurchJpaEntity(id = church.id.toUuid(), name = "Grace Church", status = ChurchStatus.INACTIVE)

        every { churchRepository.findById(church.id.toUuid()) } returns Optional.of(jpaEntity)
        every { churchMapper.toDomain(jpaEntity) } returns church
        every { churchMapper.toJpaEntity(deactivatedChurch) } returns deactivatedJpaEntity
        every { churchRepository.save(deactivatedJpaEntity) } returns deactivatedJpaEntity
        every { churchMapper.toDomain(deactivatedJpaEntity) } returns deactivatedChurch

        val response = churchService.deactivate(church.id.toUuid())

        assertEquals(church.id.toUuid(), response.id)
        assertEquals(ChurchStatus.INACTIVE, response.status)
    }

    @Test
    fun `deactivate should throw ChurchNotFoundException for unknown id`() {
        val unknownId = UUID.randomUUID()

        every { churchRepository.findById(unknownId) } returns Optional.empty()

        assertFailsWith<ChurchNotFoundException> {
            churchService.deactivate(unknownId)
        }
    }

    @Test
    fun `activate should set status to ACTIVE`() {
        val church = buildChurch(name = "Grace Church", status = ChurchStatus.INACTIVE)
        val jpaEntity = buildChurchJpaEntity(id = church.id.toUuid(), name = "Grace Church", status = ChurchStatus.INACTIVE)
        val activatedChurch = church.copy(status = ChurchStatus.ACTIVE)
        val activatedJpaEntity = buildChurchJpaEntity(id = church.id.toUuid(), name = "Grace Church", status = ChurchStatus.ACTIVE)

        every { churchRepository.findById(church.id.toUuid()) } returns Optional.of(jpaEntity)
        every { churchMapper.toDomain(jpaEntity) } returns church
        every { churchMapper.toJpaEntity(activatedChurch) } returns activatedJpaEntity
        every { churchRepository.save(activatedJpaEntity) } returns activatedJpaEntity
        every { churchMapper.toDomain(activatedJpaEntity) } returns activatedChurch

        val response = churchService.activate(church.id.toUuid())

        assertEquals(church.id.toUuid(), response.id)
        assertEquals(ChurchStatus.ACTIVE, response.status)
    }

    @Test
    fun `activate should throw ChurchNotFoundException for unknown id`() {
        val unknownId = UUID.randomUUID()

        every { churchRepository.findById(unknownId) } returns Optional.empty()

        assertFailsWith<ChurchNotFoundException> {
            churchService.activate(unknownId)
        }
    }
}
