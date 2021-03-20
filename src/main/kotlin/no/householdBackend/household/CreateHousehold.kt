package no.householdBackend.household

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
import javax.ws.rs.core.UriInfo

@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Path("/household")
class CreateHousehold(private val jdbi: Jdbi) {
    private val logger = LoggerFactory.getLogger(javaClass)

    @POST
    fun createHousehold(createHouseholdRequest: CreateHouseholdRequest, @Context uriInfo: UriInfo): Response =
        runCatching {
            val refrigeratorId = UUID.randomUUID()
            val householdId = UUID.randomUUID()

            jdbi.inTransaction<Unit, Exception> {
                it.createUpdate(
                    """
                    |INSERT INTO refrigerators
                    |VALUES (:refrigeratorId);
                    |
                    |INSERT INTO households
                    |VALUES(:householdId, :householdName, :refrigeratorId);
                    """.trimMargin()
                )
                    .bind("householdId", householdId)
                    .bind("householdName", createHouseholdRequest.name)
                    .bind("refrigeratorId", refrigeratorId)
                    .execute()
            }
            householdId
        }.mapBoth(
            success = {
                Response.created(
                    uriInfo.absolutePathBuilder.path(it.toString()).build())
                    .build()
            },
            failure = {
                logger.error("Db error:", it)
                Response.serverError().build()
            }
        )
}

data class CreateHouseholdRequest(
    val name: String
)