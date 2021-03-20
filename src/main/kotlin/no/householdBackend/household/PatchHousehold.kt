package no.householdBackend.household

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.andThen
import com.github.michaelbull.result.mapBoth
import com.github.michaelbull.result.mapError
import com.github.michaelbull.result.runCatching
import no.householdBackend.user.User
import org.jdbi.v3.core.Jdbi
import org.slf4j.LoggerFactory
import java.util.*
import java.util.stream.Collectors
import javax.ws.rs.Consumes
import javax.ws.rs.PATCH
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.core.Context
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response
import javax.ws.rs.core.Response.Status.NOT_FOUND
import javax.ws.rs.core.UriInfo

@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Path("/household")
class PatchHousehold(val jdbi: Jdbi) {
    private val logger = LoggerFactory.getLogger(javaClass)

    @PATCH
    fun patchHousehold(userHouseholdRelation: UserHouseholdRelation): Response=
        verifyUserHasNoHousehold(userHouseholdRelation.userId)
            .andThen {
                addUserHouseholdRelation(userHouseholdRelation)
            }.mapBoth(
            success = {
                Response.noContent().build()
            },
            failure = {
                when(it){
                   HouseholdNotFound ->{
                       Response.status(NOT_FOUND.statusCode, "Household not found").build()
                   }
                    is DBErrorPatchUserToHousehold -> {
                        Response.serverError().build()
                    }
                    UserNotFound -> {
                        Response.status(NOT_FOUND.statusCode, "User not found").build()
                    }
                    UserAlreadyHasHouseHold -> {
                        Response.status(NOT_FOUND.statusCode, "User already has household").build()
                    }
                }
            }
        )

    private fun addUserHouseholdRelation(userHouseholdRelation: UserHouseholdRelation): Result<Unit, PatchUserToHouseholdError> =
        runCatching {
            jdbi.withHandle<Unit, Exception> {
                it.createUpdate(
                    """
                    |INSERT INTO user_household_relation
                    |VALUES (:userId, :householdId);
                    """.trimMargin()
                )
                    .bind("userId", UUID.fromString(userHouseholdRelation.userId))
                    .bind("householdId", UUID.fromString(userHouseholdRelation.householdId))
                    .execute()
            }
        }.mapError {
            when {
                it.message?.contains("is not present in table \"households\"") == true -> {
                    HouseholdNotFound
                }
                it.message?.contains("is not present in table \"users\"") == true -> {
                    UserNotFound
                }
                else -> {
                    DBErrorPatchUserToHousehold(it)
                }
            }
        }.andThen {
            Ok(Unit)
        }


    private fun verifyUserHasNoHousehold(userId: String): Result<Unit, PatchUserToHouseholdError> =
        runCatching {
            jdbi.withHandle<List<UUID>,Exception>{
                it.createQuery(
                    """
                        |SELECT user_id
                        |FROM user_household_relation
                        |WHERE user_id = :userId;
                    """.trimMargin()
                )
                    .bind("userId", UUID.fromString(userId))
                    .map { rs,_,_ ->
                        UUID.fromString(rs.getString("user_id"))
                    }
                    .collect(Collectors.toList())
            }
        }.mapError {
            if(it.message?.contains("is not present in table \"users\"") == true){
                UserNotFound
            }else{
                DBErrorPatchUserToHousehold(it)
            }
        }.andThen {
            if(it.isEmpty()){
                Ok(Unit)
            }else{
                Err(UserAlreadyHasHouseHold)
            }
        }
}

data class UserHouseholdRelation(
    val userId: String,
    val householdId: String
)

private sealed class PatchUserToHouseholdError
private class DBErrorPatchUserToHousehold(val error: Throwable) : PatchUserToHouseholdError()
private object UserNotFound: PatchUserToHouseholdError()
private object HouseholdNotFound: PatchUserToHouseholdError()
private object UserAlreadyHasHouseHold: PatchUserToHouseholdError()