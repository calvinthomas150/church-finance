package com.calvintech.churchfinance.administration.domain

import java.util.UUID

class ChurchNotFoundException(
    id: UUID,
) : RuntimeException("Church not found: $id")
