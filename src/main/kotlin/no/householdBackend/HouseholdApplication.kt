package no.householdBackend

import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import io.dropwizard.Application
import io.dropwizard.Configuration
import io.dropwizard.auth.AuthValueFactoryProvider
import io.dropwizard.auth.oauth.OAuthCredentialAuthFilter
import io.dropwizard.db.DataSourceFactory
import io.dropwizard.jdbi3.JdbiFactory
import io.dropwizard.setup.Bootstrap
import io.dropwizard.setup.Environment
import no.householdBackend.auth.HouseholdAuthenticator
import no.householdBackend.auth.RequireAuthByDefaultDynamicFeature
import no.householdBackend.grocery.CreateGrocery
import no.householdBackend.grocery.PatchGrocery
import no.householdBackend.household.CreateHousehold
import no.householdBackend.household.GetHousehold
import no.householdBackend.household.PatchHousehold
import no.householdBackend.user.AuthenticateUser
import no.householdBackend.user.CreateUser
import no.householdBackend.user.GetUser
import no.householdBackend.user.AuthUser
import org.flywaydb.core.Flyway
import javax.ws.rs.container.ContainerRequestContext
import javax.ws.rs.container.ContainerRequestFilter

fun main(args: Array<String>) {
    HouseholdApplication().run(*args)
}

class HouseholdApplication : Application<HouseholdConfig>() {

    private val databaseName = "household"

    override fun run(configuration: HouseholdConfig, environment: Environment) {
        val dataSource = configuration.database.build(environment.metrics(), databaseName)
        val jdbi = JdbiFactory().build(environment, configuration.database, dataSource, databaseName)

        Flyway.configure()
            .dataSource(dataSource)
            .load()
            .migrate()

        environment.jersey().register(GetHousehold(jdbi))
        environment.jersey().register(GetUser(jdbi))
        environment.jersey().register(CreateUser(jdbi))
        environment.jersey().register(CreateHousehold(jdbi))
        environment.jersey().register(PatchHousehold(jdbi))
        environment.jersey().register(AuthenticateUser(jdbi, configuration.jwtSecret))
        environment.jersey().register(PatchGrocery(jdbi))
        environment.jersey().register(CreateGrocery(jdbi))

        environment.jersey().register(
            RequireAuthByDefaultDynamicFeature(
                AuthFilter(
                    emptyList(),
                    OAuthCredentialAuthFilter.Builder<AuthUser>()
                        .setAuthenticator(HouseholdAuthenticator(jdbi, configuration.jwtSecret))
                        .setPrefix("Bearer")
                        .buildAuthFilter()
                )
            )
        )
        // env.jersey().register(RolesAllowedDynamicFeature::class.java)
        // If you want to use @Auth to inject a custom Principal type into your resource
        environment.jersey().register(AuthValueFactoryProvider.Binder(AuthUser::class.java))
    }

    override fun initialize(bootstrap: Bootstrap<HouseholdConfig>) {
        bootstrap.objectMapper.registerKotlinModule()
    }
}

data class HouseholdConfig(
    val database: DataSourceFactory,
    val jwtSecret: String
) : Configuration()

class AuthFilter(
    private val openPaths: List<String>,
    private val filter: ContainerRequestFilter
) : ContainerRequestFilter {
    override fun filter(requestContext: ContainerRequestContext?) {
        if (requestContext?.uriInfo?.path !in openPaths) {
            filter.filter(requestContext)
        }
    }
}
