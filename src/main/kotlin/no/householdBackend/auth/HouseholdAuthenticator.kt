package no.householdBackend.auth

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.interfaces.DecodedJWT
import com.github.michaelbull.result.*
import io.dropwizard.auth.Authenticator
import no.householdBackend.user.AuthUser
import no.householdBackend.user.claim
import no.householdBackend.user.issuer
import org.jdbi.v3.core.Jdbi
import org.slf4j.LoggerFactory
import java.util.*
import java.util.stream.Collectors

class HouseholdAuthenticator(
    private val jdbi: Jdbi,
    private val secret: String
) : Authenticator<String, AuthUser> {
    private val logger = LoggerFactory.getLogger(javaClass)

    override fun authenticate(token: String): Optional<AuthUser> =
        verifyToken(token)
            .andThen(::getUsernameFromToken)
            .andThen(::findUserByUsername)
            .mapBoth(
                success = {
                    Optional.of(it)
                },
                failure = {
                    when (it) {
                        is HouseholdAuthenticatorError.InvalidTokenError -> {
                            logger.error("Token was invalid:", it.error)
                        }
                        is HouseholdAuthenticatorError.NoUsernameInToken -> {
                            logger.error("Didn't find username in token:", it.error)
                        }
                        is HouseholdAuthenticatorError.DBError -> {
                            logger.error("Database error:", it.error)
                        }
                        HouseholdAuthenticatorError.NotFound -> {
                            logger.error("User not found")
                        }
                    }
                    Optional.empty()
                }
            )

    private fun verifyToken(token: String): Result<DecodedJWT, HouseholdAuthenticatorError> =
        runCatching {
            val algorithm = Algorithm.HMAC256(secret)
            JWT.require(algorithm)
                .withIssuer(issuer)
                .build()
                .verify(token)
        }.mapError {
            HouseholdAuthenticatorError.InvalidTokenError(it)
        }

    private fun getUsernameFromToken(decodedJWT: DecodedJWT): Result<String, HouseholdAuthenticatorError> =
        runCatching {
            decodedJWT.getClaim(claim).asString()
        }.mapError {
            HouseholdAuthenticatorError.NoUsernameInToken(it)
        }

    private fun findUserByUsername(username: String): Result<AuthUser, HouseholdAuthenticatorError> =
        runCatching {
            jdbi.withHandle<AuthUser?, Exception> {
                it.createQuery(
                    """
                    |SELECT *
                    |FROM users
                    |WHERE mail = :mail
                    """.trimMargin()
                )
                    .bind("mail", username)
                    .map { rs, _, _ ->
                        AuthUser(
                            id = UUID.fromString(rs.getString("id")),
                            mail = rs.getString("mail"),
                            hash = rs.getString("hash"),
                            salt = rs.getString("salt")
                        )
                    }
                    .collect(Collectors.toList())
                    .firstOrNull()
            }
        }.mapError {
            HouseholdAuthenticatorError.DBError(it)
        }.andThen {
            if (it != null) {
                Ok(it)
            } else {
                Err(HouseholdAuthenticatorError.NotFound)
            }
        }
}

private sealed class HouseholdAuthenticatorError {
    class InvalidTokenError(val error: Throwable) : HouseholdAuthenticatorError()
    class NoUsernameInToken(val error: Throwable) : HouseholdAuthenticatorError()
    class DBError(val error: Throwable) : HouseholdAuthenticatorError()
    object NotFound : HouseholdAuthenticatorError()
}
