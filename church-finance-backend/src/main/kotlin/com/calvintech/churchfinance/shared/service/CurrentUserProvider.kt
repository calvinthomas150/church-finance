package com.calvintech.churchfinance.shared.service

import com.github.f4b6a3.ulid.Ulid

interface CurrentUserProvider {
    fun getCurrentUserId(): Ulid
}
