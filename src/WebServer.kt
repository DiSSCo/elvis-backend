package org.synthesis

import io.ktor.server.engine.*
import io.ktor.server.netty.*

fun startWebServer(port: Int) = embeddedServer(Netty, port) {
    module()
}.start(true)
