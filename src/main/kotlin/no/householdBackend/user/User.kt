package no.householdBackend.user

import java.security.Principal
import java.util.*

data class User(
    val id: UUID,
    val mail: String,
    val name: String
)

data class AuthUser(
    val id: UUID,
    val mail: String,
    val hash: String,
    val salt: String
) : Principal {
    override fun getName() = "User"
}

const val issuer = "household-backend"
const val claim = "userName"
