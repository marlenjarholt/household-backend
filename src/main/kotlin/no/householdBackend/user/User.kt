package no.householdBackend.user

import java.util.*

data class User(
    val id: UUID,
    val mail: String,
    val name: String
)
