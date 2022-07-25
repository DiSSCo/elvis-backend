package org.synthesis

import io.ktor.locations.KtorExperimentalLocationsAPI
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.util.InternalAPI
import io.ktor.util.KtorExperimentalAPI
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.ObsoleteCoroutinesApi

@InternalAPI
@KtorExperimentalAPI
@KtorExperimentalLocationsAPI
@ExperimentalCoroutinesApi
@ObsoleteCoroutinesApi
fun startWebServer(port: Int) = embeddedServer(Netty, port) {
    module()
}.start(true)
