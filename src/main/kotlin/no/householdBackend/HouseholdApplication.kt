package no.householdBackend

import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import io.dropwizard.Application
import io.dropwizard.Configuration
import io.dropwizard.db.DataSourceFactory
import io.dropwizard.setup.Bootstrap
import io.dropwizard.setup.Environment
import no.householdBackend.household.GetHousehold
import org.flywaydb.core.Flyway

fun main(args: Array<String>){
    HouseholdApplication().run(*args)
}

class HouseholdApplication : Application<HouseholdConfig>() {
    override fun run(configuration: HouseholdConfig, environment: Environment) {
        val dataSource = configuration.database.build(environment.metrics(), "household")
        Flyway.configure()
            .dataSource(dataSource)
            .load()
            .migrate()

        environment.jersey().register(GetHousehold())
    }

    override fun initialize(bootstrap: Bootstrap<HouseholdConfig>) {
        bootstrap.objectMapper.registerKotlinModule()
    }

}

data class HouseholdConfig(
    val database: DataSourceFactory
) :Configuration()