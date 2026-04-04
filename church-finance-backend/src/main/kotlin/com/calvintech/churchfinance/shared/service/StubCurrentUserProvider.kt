package com.calvintech.churchfinance.shared.service

import com.github.f4b6a3.ulid.Ulid
import com.github.f4b6a3.ulid.UlidCreator
import org.springframework.stereotype.Component

@Component
class StubCurrentUserProvider : CurrentUserProvider {
    override fun getCurrentUserId(): Ulid = UlidCreator.getUlid()
}
