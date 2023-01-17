package org.synthesis

import org.koin.core.context.stopKoin
import org.synthesis.environment.EnvironmentSetup
import org.synthesis.infrastructure.loadDependencies
import kotlin.system.exitProcess

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
