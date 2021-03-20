package no.householdBackend.user

import com.github.michaelbull.result.mapBoth
import com.github.michaelbull.result.mapError
import com.github.michaelbull.result.runCatching
import no.householdBackend.util.hash
import no.householdBackend.util.secureRandom
import org.jdbi.v3.core.Jdbi
import org.jdbi.v3.core.statement.UnableToExecuteStatementException
import org.slf4j.LoggerFactory
import java.lang.Exception
import java.util.*
import javax.ws.rs.Consumes
import javax.ws.rs.POST
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.core.Context
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response
import javax.ws.rs.core.UriInfo

@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Path("/user")
class CreateUser(val jdbi: Jdbi){

    private val logger = LoggerFactory.getLogger(javaClass)

    @POST
    fun createUser(user: UserRequest, @Context uriInfo: UriInfo) : Response{
        val salt = secureRandom()
        val hashedPassword = hash(user.password + salt)
        val id = UUID.randomUUID()
        return runCatching {
            jdbi.withHandle<Unit, Exception>{
                it.createUpdate(
                    """
                        |INSERT INTO users
                        |VALUES(:id, :mail, :name, :hash, :salt);
                    """.trimMargin()
                )
                    .bind("id", id)
                    .bind("mail", user.mail)
                    .bind("name", user.name)
                    .bind("hash", hashedPassword)
                    .bind("salt", salt)
                    .execute()
            }
        }.mapError {
            if(it is UnableToExecuteStatementException && it.message?.contains("ERROR: duplicate key value violates unique constraint \"users_mail_key\"") == true){
                MailError
            }else{
                DBErrorCreateUser(it)
            }
        }.mapBoth(
           success = {
               val uri = uriInfo.absolutePathBuilder.path(id.toString()).build()
               Response.created(uri).build()
           },
            failure = {
                when(it){
                    is DBErrorCreateUser -> {
                        logger.error("Database error: ", it.error)
                        Response.serverError().build()
                    }
                    MailError -> {
                        logger.error("User error: email already exists")
                        Response.status(Response.Status.BAD_REQUEST).build()
                    }
                }
            }
        )
    }
}

data class UserRequest(
    val mail: String,
    val name: String,
    val password: String
)

private sealed class CreateUserError
private class DBErrorCreateUser(val error: Throwable) : CreateUserError()
private object MailError: CreateUserError()