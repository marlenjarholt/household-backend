package no.householdBackend.household

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.andThen
import com.github.michaelbull.result.mapBoth
import com.github.michaelbull.result.mapError
import com.github.michaelbull.result.runCatching
import org.jdbi.v3.core.Jdbi
import org.slf4j.LoggerFactory
import java.lang.Exception
import java.time.LocalDate
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
@Path("/household/{id}")
class GetHousehold(private val jdbi: Jdbi) {
    private val logger = LoggerFactory.getLogger(javaClass)

    @GET
    fun getHousehold(@PathParam("id") id: UUID): Response =
        runCatching {
            jdbi.withHandle<List<DatabaseRow>, Exception> {
                it.createQuery(
                    """
                    |SELECT
                    |   households.id AS householdId,
                    |   households.name AS householdName,
                    |   households.refrigerator_id AS householdRefrigeratorId,
                    |   refrigerators.id AS refrigeratorId,
                    |   refrigerator_grocery_relation.refrigerator_id AS refrigeratorGroceryRelationId,
                    |   groceries.id AS groceryId,
                    |   groceries.name AS groceryName,
                    |   groceries.amount AS groceryAmount,
                    |   groceries.unit AS groceryUnit,
                    |   groceries.expiration_date AS groceryExpirationDate
                    |FROM households
                    |JOIN refrigerators ON refrigerators.id = households.refrigerator_id
                    |JOIN refrigerator_grocery_relation ON refrigerator_grocery_relation.refrigerator_id = refrigerators.id
                    |JOIN groceries ON groceries.id = refrigerator_grocery_relation.grocery_id
                    |WHERE households.id = :id;
                    """.trimMargin()
                )
                    .bind("id", id)
                    .map { rs, _, _ ->
                        DatabaseRow(
                            householdId = UUID.fromString(rs.getString("householdId")),
                            householdName = rs.getString("householdName"),
                            householdRefrigeratorId = UUID.fromString(rs.getString("householdRefrigeratorId")),
                            refrigeratorId = UUID.fromString(rs.getString("refrigeratorId")),
                            refrigeratorGroceryRelationId = UUID.fromString(rs.getString("refrigeratorGroceryRelationId")),
                            groceryId = UUID.fromString(rs.getString("groceryId")),
                            groceryName = rs.getString("groceryName"),
                            groceryAmount = rs.getDouble("groceryAmount"),
                            groceryUnit = rs.getString("groceryUnit"),
                            groceryExpirationDate = rs.getDate("groceryExpirationDate")?.toLocalDate()
                        )
                    }
                    .collect(Collectors.toList())
            }
        }.mapError {
            DBError(it)
        }.andThen {
            if (it.isEmpty()) {
                Err(NotFound)
            } else {
                Ok(it)
            }
        }.andThen(::createHousehold)
            .mapBoth(
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
                        is ParseError -> {
                            logger.error("Parse error:", it.error)
                            Response.serverError().build()
                        }
                    }
                }
            )
}

private fun createHousehold(databaseRows: List<DatabaseRow>): Result<Household, GetHouseholdError> =
    runCatching {
        Household(
            id = databaseRows.first().householdId,
            name = databaseRows.first().householdName,
            refrigerator = Refrigerator(
                id = databaseRows.first().refrigeratorId,
                groceries = databaseRows.map {
                    Grocery(
                        id = it.groceryId,
                        name = it.groceryName,
                        amount = it.groceryAmount,
                        unit = it.groceryUnit,
                        expirationDate = it.groceryExpirationDate
                    )
                }
            )
        )
    }.mapError {
        ParseError(it)
    }

data class DatabaseRow(
    val householdId: UUID,
    val householdName: String,
    val householdRefrigeratorId: UUID,
    val refrigeratorId: UUID,
    val refrigeratorGroceryRelationId: UUID,
    val groceryId: UUID,
    val groceryName: String,
    val groceryAmount: Double,
    val groceryUnit: String,
    val groceryExpirationDate: LocalDate?
)

private sealed class GetHouseholdError

private class DBError(val error: Throwable) : GetHouseholdError()
private object NotFound : GetHouseholdError()
private class ParseError(val error: Throwable) : GetHouseholdError()
