package no.householdBackend

import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import io.dropwizard.Application
import io.dropwizard.Configuration
import io.dropwizard.setup.Bootstrap
import io.dropwizard.setup.Environment
import no.householdBackend.household.GetHousehold

fun main(args: Array<String>){
    HouseholdApplication().run(*args)
}

class HouseholdApplication : Application<HouseholdConfig>() {
    override fun run(configuration: HouseholdConfig, environment: Environment) {
        print(configuration.s)

        environment.jersey().register(GetHousehold())
    }

    override fun initialize(bootstrap: Bootstrap<HouseholdConfig>) {
        bootstrap.objectMapper.registerKotlinModule()
    }

}

data class HouseholdConfig(
    val s:String
) :Configuration()