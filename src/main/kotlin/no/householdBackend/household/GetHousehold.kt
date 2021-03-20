package no.householdBackend.household

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.andThen
import com.github.michaelbull.result.mapBoth
import com.github.michaelbull.result.mapError
import com.github.michaelbull.result.runCatching
import org.jdbi.v3.core.Jdbi
import java.lang.Exception
import java.util.*
import java.util.stream.Collectors
import javax.ws.rs.GET
import javax.ws.rs.Path
import javax.ws.rs.PathParam
import javax.ws.rs.Produces
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response
import javax.ws.rs.core.Response.Status.NOT_FOUND
import org.slf4j.LoggerFactory

@Produces(MediaType.APPLICATION_JSON)
@Path("/household/{id}")
class GetHousehold(private val jdbi: Jdbi) {
    private val logger = LoggerFactory.getLogger(javaClass)

    @GET
    fun getHousehold(@PathParam("id") id: UUID): Response =
        runCatching {
            jdbi.withHandle<Household?, Exception> {
                it.createQuery(
                    """
                    |SELECT *
                    |FROM households
                    |WHERE id = :id;
                """.trimMargin()
                )
                    .bind("id", id)
                    .map { rs, _, _ ->
                        Household(
                            id = UUID.fromString(rs.getString("id")),
                            name = rs.getString("name")
                        )
                    }
                    .collect(Collectors.toList())
                    .firstOrNull()
            }
        }.mapError {
            DBError(it)
        }.andThen {
            if (it != null) {
                Ok(it)
            } else {
                Err(NotFound)
            }
        }.mapBoth(
            success = {
                Response.ok(it).build()
            },
            failure = {
                when (it) {
                    is DBError -> {
                        logger.error("Database error:", it.error)
                        Response.serverError().build()
                    }
                    NotFound -> Response.status(NOT_FOUND).build()
                }
            }
        )
}

private sealed class GetHouseholdError

private class DBError(val error: Throwable) : GetHouseholdError()
private object NotFound : GetHouseholdError()
