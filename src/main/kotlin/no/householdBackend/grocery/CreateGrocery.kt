package no.householdBackend.grocery

import com.github.michaelbull.result.*
import no.householdBackend.household.Grocery
import no.householdBackend.household.GroceryUnit
import org.jdbi.v3.core.Jdbi
import org.slf4j.LoggerFactory
import java.time.LocalDate
import java.util.UUID
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
    fun createGrocery(createGroceryRequest: CreateGroceryRequest, @Context uriInfo: UriInfo): Response =
        parseGrocery(createGroceryRequest)
            .andThen { grocery ->
                runCatching {
                    jdbi.inTransaction<Unit, Exception> {
                        it.createUpdate(
                            """
                            |INSERT INTO groceries
                            |VALUES(:id, :name, :amount, :unit, :expirationDate);
                            |
                            |INSERT INTO refrigerator_grocery_relation
                            |VALUES(:refrigeratorId, :id);
                            """.trimMargin()
                        )
                            .bind("id", grocery.id)
                            .bind("name", grocery.name)
                            .bind("amount", grocery.amount)
                            .bind("unit", grocery.unit.value)
                            .bind("refrigeratorId", createGroceryRequest.refrigeratorId)
                            .bind("expirationDate", grocery.expirationDate)
                            .execute()
                    }
                    grocery.id
                }.mapError {
                    if (it.message?.contains("is not present in table \"refrigerators\"") == true) {
                        RefrigeratorNotFound
                    } else {
                        DBErrorCreateGrocery(it)
                    }
                }
            }.mapBoth(
                success = {
                    val uri = uriInfo.absolutePathBuilder.path(it.toString()).build()
                    Response.created(uri).build()
                },
                failure = {
                    when (it) {
                        is DBErrorCreateGrocery -> {
                            logger.error("Database error: ", it.error)
                            Response.serverError().build()
                        }
                        RefrigeratorNotFound -> {
                            logger.error("User error: refrigerator not found")
                            Response.status(NOT_FOUND.statusCode, "Refrigerator not found").build()
                        }
                        ParseError -> {
                            logger.error("Parse error.")
                            Response.status(BAD_REQUEST.statusCode, "Unable to parse amount unit").build()
                        }
                    }
                }
            )

    private fun parseGrocery(createGroceryRequest: CreateGroceryRequest): Result<Grocery, CreateGroceryError> {
        val groceryUnit = GroceryUnit.fromString(createGroceryRequest.unit)
        return if (groceryUnit != null) {
            Ok(
                Grocery(
                    UUID.randomUUID(),
                    createGroceryRequest.name,
                    createGroceryRequest.amount,
                    groceryUnit,
                    createGroceryRequest.expirationDate
                )
            )
        } else {
            Err(ParseError)
        }
    }
}

data class CreateGroceryRequest(
    val refrigeratorId: UUID,
    val name: String,
    val amount: Number,
    val unit: String,
    val expirationDate: LocalDate?
)

private sealed class CreateGroceryError
private class DBErrorCreateGrocery(val error: Throwable) : CreateGroceryError()
private object RefrigeratorNotFound : CreateGroceryError()
private object ParseError : CreateGroceryError()
