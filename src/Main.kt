package org.synthesis

import io.ktor.locations.KtorExperimentalLocationsAPI
import io.ktor.util.InternalAPI
import io.ktor.util.KtorExperimentalAPI
import kotlin.system.exitProcess
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.koin.core.context.stopKoin
import org.synthesis.environment.EnvironmentSetup
import org.synthesis.infrastructure.loadDependencies

@InternalAPI
@KtorExperimentalAPI
@KtorExperimentalLocationsAPI
@ExperimentalCoroutinesApi
@kotlinx.coroutines.ObsoleteCoroutinesApi
fun main() {
    try {
        loadDependencies()

        /** Prepare application to run **/
        EnvironmentSetup.prepare()

        startWebServer(8080)
    } catch (cause: Throwable) {
        cause.printStackTrace()
        stopKoin()

        exitProcess(1)
    }
}
