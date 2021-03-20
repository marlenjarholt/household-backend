package no.householdBackend.household

import javax.ws.rs.GET
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response

@Produces(MediaType.APPLICATION_JSON)
@Path("/household")
class GetHousehold {

    @GET
    fun getHousehold(): Response{
        return Response.ok("HEI:))").build()
    }

}