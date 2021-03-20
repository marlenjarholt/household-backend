package no.householdBackend.household

import org.jdbi.v3.core.Jdbi
import java.lang.Exception
import java.util.*
import java.util.stream.Collectors
import javax.ws.rs.GET
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response

@Produces(MediaType.APPLICATION_JSON)
@Path("/household")
class GetHousehold(val jdbi: Jdbi) {

    @GET
    fun getHousehold(): Response{
        val householdList = jdbi.withHandle<List<Household>, Exception>{
            it.createQuery(
                """
                    |SELECT *
                    |FROM households;
                """.trimMargin()
            )
                .map{rs, _, _ ->
                    Household(
                        id = UUID.fromString(rs.getString("id")),
                        name = rs.getString("name")
                    )

                }
                .collect(Collectors.toList())
        }
        return Response.ok(householdList).build()
    }

}