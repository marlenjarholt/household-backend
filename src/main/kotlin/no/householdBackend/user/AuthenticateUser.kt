package no.householdBackend.user

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.github.michaelbull.result.*
import no.householdBackend.auth.NoAuthRequired
import no.householdBackend.util.hash
import org.jdbi.v3.core.Jdbi
import org.slf4j.LoggerFactory
import java.util.*
import javax.ws.rs.Consumes
import javax.ws.rs.POST
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response

@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Path("/users/authenticate")
class AuthenticateUser(
    private val jdbi: Jdbi,
    private val secret: String
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    @POST
    @NoAuthRequired
    fun authenticate(loginRequest: LoginRequest): Response =
        findUserByMail(loginRequest.mail)
            .andThen { verifyPassword(it, loginRequest.password) }
            .andThen(::generateToken)
            .mapBoth(
                success = {
                    Response.ok(it).build()
                },
                failure = {
                    when (it) {
                        AuthenticateUserError.NotFound -> {
                            logger.error("NotFound")
                            Response.status(Response.Status.UNAUTHORIZED).build()
                        }
                        AuthenticateUserError.IncorrectPassword -> {
                            logger.error("IncorrectPassword")
                            Response.status(Response.Status.UNAUTHORIZED).build()
                        }
                        is AuthenticateUserError.DbError -> {
                            logger.error("DbError", it.error)
                            Response.serverError().build()
                        }
                        is AuthenticateUserError.TokenFailed -> {
                            logger.error("TokenFailed", it.error)
                            Response.serverError().build()
                        }
                    }
                }
            )

    private fun findUserByMail(mail: String): Result<AuthUser, AuthenticateUserError> =
        runCatching {
            jdbi.withHandle<AuthUser?, Exception> {
                it.createQuery(
                    """
                    |SELECT *
                    |FROM users
                    |WHERE mail = :mail;
                    """.trimMargin()
                )
                    .bind("mail", mail)
                    .map { rs, _, _ ->
                        AuthUser(
                            id = UUID.fromString(rs.getString("id")),
                            mail = rs.getString("mail"),
                            salt = rs.getString("salt"),
                            hash = rs.getString("hash")
                        )
                    }
                    .findFirst()
                    .orElse(null)
            }
        }
            .mapError { AuthenticateUserError.DbError(it) }
            .andThen {
                if (it == null) {
                    Err(AuthenticateUserError.NotFound)
                } else {
                    Ok(it)
                }
            }

    private fun verifyPassword(authUser: AuthUser, password: String): Result<AuthUser, AuthenticateUserError> =
        if (authUser.hash == hash(password + authUser.salt)) {
            Ok(authUser)
        } else {
            Err(AuthenticateUserError.IncorrectPassword)
        }

    private fun generateToken(authUser: AuthUser): Result<String, AuthenticateUserError> =
        runCatching {
            val algorithm = Algorithm.HMAC256(secret)
            JWT.create()
                .withIssuer(issuer)
                .withClaim(claim, authUser.mail)
                .sign(algorithm)
        }.mapError {
            AuthenticateUserError.TokenFailed(it)
        }
}

private sealed class AuthenticateUserError {
    object NotFound : AuthenticateUserError()
    class DbError(val error: Throwable) : AuthenticateUserError()
    object IncorrectPassword : AuthenticateUserError()
    class TokenFailed(val error: Throwable) : AuthenticateUserError()
}

data class LoginRequest(
    val mail: String,
    val password: String
)
