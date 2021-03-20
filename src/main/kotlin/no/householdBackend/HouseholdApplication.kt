package no.householdBackend

import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import io.dropwizard.Application
import io.dropwizard.Configuration
import io.dropwizard.db.DataSourceFactory
import io.dropwizard.jdbi3.JdbiFactory
import io.dropwizard.setup.Bootstrap
import io.dropwizard.setup.Environment
import no.householdBackend.household.CreateHousehold
import no.householdBackend.household.GetHousehold
import no.householdBackend.household.PatchHousehold
import no.householdBackend.user.CreateUser
import no.householdBackend.user.GetUser
import org.flywaydb.core.Flyway

fun main(args: Array<String>){
    HouseholdApplication().run(*args)
}

class HouseholdApplication : Application<HouseholdConfig>() {

    val databaseName = "household"

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
    }

    override fun initialize(bootstrap: Bootstrap<HouseholdConfig>) {
        bootstrap.objectMapper.registerKotlinModule()
    }

}

data class HouseholdConfig(
    val database: DataSourceFactory
) :Configuration()