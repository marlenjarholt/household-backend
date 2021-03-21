package no.householdBackend.grocery

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.andThen
import com.github.michaelbull.result.mapBoth
import com.github.michaelbull.result.mapError
import com.github.michaelbull.result.runCatching
import no.householdBackend.household.Grocery
import no.householdBackend.household.GroceryUnit
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
        parseGrocery(groceryPatch)
            .andThen { grocery ->
                runCatching {
                    jdbi.withHandle<Int, Exception> {
                        it.createUpdate(
                            """
                      |UPDATE groceries
                      |SET name = :name, amount = :amount, unit = :unit, expiration_date = :expirationDate 
                      |WHERE id = :id;
                    """.trimMargin()
                        )
                            .bind("id", grocery.id)
                            .bind("name", grocery.name)
                            .bind("amount", grocery.amount)
                            .bind("unit", grocery.unit.value)
                            .bind("expirationDate", grocery.expirationDate)
                            .execute()
                    }
                }.mapError {
                    DBErrorPatchGroceryError(it)
                }
            }
            .andThen { rowsAffected ->
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
                        is ParseUserInputError -> {
                            logger.error("User error: gorcery unit invalid")
                            Response.status(BAD_REQUEST.statusCode, "Invalid grocery amount").build()
                        }
                    }
                }
            )

    private fun parseGrocery(groceryPatch: GroceryPatch): Result<Grocery, PatchGroceryError> {
        val groceryUnit = GroceryUnit.fromString(groceryPatch.unit)
        return if (groceryUnit != null) {
            Ok(
                Grocery(
                    groceryPatch.id,
                    groceryPatch.name,
                    groceryPatch.amount,
                    groceryUnit,
                    groceryPatch.expirationDate
                )
            )
        } else {
            Err(
                ParseUserInputError
            )
        }
    }
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
private object ParseUserInputError : PatchGroceryError()
