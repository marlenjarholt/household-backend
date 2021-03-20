package no.householdBackend.user

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.andThen
import com.github.michaelbull.result.mapBoth
import com.github.michaelbull.result.mapError
import com.github.michaelbull.result.runCatching
import org.jdbi.v3.core.Jdbi
import org.slf4j.LoggerFactory
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

@Produces(MediaType.APPLICATION_JSON)
@Path("/user/{id}")
class GetUser(val jdbi: Jdbi) {

    private val logger = LoggerFactory.getLogger(javaClass)

    @GET
    fun getUser(@PathParam("id") id: UUID): Response =
        runCatching {
            jdbi.withHandle<User?, Exception>{
                it.createQuery(
                    """
                    |SELECT *
                    |FROM users
                    |WHERE id = :id
                """.trimMargin()
                )
                    .bind("id", id)
                    .map{rs, _, _ ->
                        User(
                            id = UUID.fromString(rs.getString("id")),
                            mail = rs.getString("mail"),
                            name = rs.getString("name")
                        )

                    }
                    .collect(Collectors.toList()).firstOrNull()
            }
        }.mapError {
            DBErrorGetUser(it)
        }.andThen {
            if(it != null){
                Ok(it)
            }else{
                Err(NotFound)
            }
        }.mapBoth(
            success = {
                Response.ok(it).build()
            },
            failure = {
                when(it) {
                    is DBErrorGetUser -> {
                        logger.error("Database error: ", it.error)
                        Response.serverError().build()
                    }
                    NotFound -> Response.status(NOT_FOUND).build()
                }
            }
        )

}

internal sealed class GetUserError
private class DBErrorGetUser(val error: Throwable) : GetUserError()
private object NotFound: GetUserError()