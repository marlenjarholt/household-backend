package no.householdBackend.grocery

import com.github.michaelbull.result.Result
import com.github.michaelbull.result.mapBoth
import com.github.michaelbull.result.mapError
import com.github.michaelbull.result.runCatching
import org.jdbi.v3.core.Jdbi
import org.slf4j.LoggerFactory
import java.util.*
import javax.ws.rs.Consumes
import javax.ws.rs.POST
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.core.Context
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response
import javax.ws.rs.core.Response.Status.*
import javax.ws.rs.core.UriInfo

@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Path("/grocery")
class CreateGrocery(private val jdbi: Jdbi) {
    private val logger = LoggerFactory.getLogger(javaClass)

    @POST
    fun createGrocery(createGroceryRequest: CreateGroceryRequest, @Context uriInfo:UriInfo): Response =
        runCatching {
           val id = UUID.randomUUID()
           jdbi.inTransaction<Unit, Exception> {
               it.createUpdate(
                   """
                      |INSERT INTO  groceries
                      |VALUES(:id, :name, :amount, :unit);
                      |
                      |INSERT INTO refrigerator_grocery_relation
                      |VALUES(:refrigeratorId, :id);
                   """.trimMargin()
               )
                   .bind("id", id)
                   .bind("name", createGroceryRequest.name)
                   .bind("amount", createGroceryRequest.amount)
                   .bind("unit", createGroceryRequest.unit)
                   .bind("refrigeratorId", createGroceryRequest.refrigeratorId)
                   .execute()
           }
            id
        }.mapError {
            if (it.message?.contains("is not present in table \"refrigerators\"") == true){
                RefrigeratorNotFound
            }else {
                DBErrorCreateGrocery(it)
            }
        }.mapBoth(
            success = {
                val uri = uriInfo.absolutePathBuilder.path(it.toString()).build()
                Response.created(uri).build()
            },
            failure = {
                when(it){
                    is DBErrorCreateGrocery -> {
                        logger.error("Database error: ", it.error)
                        Response.serverError().build()
                    }
                    RefrigeratorNotFound -> {
                        logger.error("User error: refrigerator not found")
                        Response.status(NOT_FOUND.statusCode, "Refrigerator not found").build()
                    }
                }

            }
        )
}

data class CreateGroceryRequest(
    val refrigeratorId: UUID,
    val name: String,
    val amount: Number,
    val unit: String
)

private sealed class CreateGroceryError
private class DBErrorCreateGrocery(val error: Throwable) : CreateGroceryError()
private object  RefrigeratorNotFound: CreateGroceryError()