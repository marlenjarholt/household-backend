package no.householdBackend.grocery

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.andThen
import com.github.michaelbull.result.mapBoth
import com.github.michaelbull.result.mapError
import com.github.michaelbull.result.runCatching
import org.jdbi.v3.core.Jdbi
import org.slf4j.LoggerFactory
import java.lang.Exception
import java.time.LocalDate
import java.util.UUID
import javax.ws.rs.PATCH
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response
import javax.ws.rs.core.Response.Status.BAD_REQUEST

@Produces(MediaType.APPLICATION_JSON)
@Path("/grocery")
class PatchGrocery(private val jdbi: Jdbi) {
    private val logger = LoggerFactory.getLogger(javaClass)

    @PATCH
    fun patchGrocery(groceryPatch: GroceryPatch): Response =
        runCatching {
            jdbi.withHandle<Int, Exception> {
                it.createUpdate(
                    """
                      |UPDATE groceries
                      |SET name = :name, amount = :amount, unit = :unit, expiration_date = :expirationDate 
                      |WHERE id = :id;
                    """.trimMargin()
                )
                    .bind("id", groceryPatch.id)
                    .bind("name", groceryPatch.name)
                    .bind("amount", groceryPatch.amount)
                    .bind("unit", groceryPatch.unit)
                    .bind("expirationDate", groceryPatch.expirationDate)
                    .execute()
            }
        }.mapError {
            DBErrorPatchGroceryError(it)
        }.andThen { rowsAffected ->
            if (rowsAffected == 0) {
                Err(GroceryNotFound)
            } else {
                Ok(Unit)
            }
        }.mapBoth(
            success = {
                Response.noContent().build()
            },
            failure = {
                when (it) {
                    is DBErrorPatchGroceryError -> {
                        logger.error("Db error: ", it)
                        Response.serverError().build()
                    }
                    GroceryNotFound -> {
                        logger.error("User error: gorcery not found")
                        Response.status(BAD_REQUEST.statusCode, "Invalid Grocery id").build()
                    }
                }
            }
        )
}

data class GroceryPatch(
    val id: UUID,
    val name: String,
    val amount: Number,
    val unit: String,
    val expirationDate: LocalDate?
)

private sealed class PatchGroceryError
private class DBErrorPatchGroceryError(val error: Throwable) : PatchGroceryError()
private object GroceryNotFound : PatchGroceryError()
