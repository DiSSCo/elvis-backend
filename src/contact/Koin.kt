package org.synthesis.contact

import org.koin.dsl.module

val contactModule = module {

    single {
        ContactHandler(
            mailer = get()
        )
    }
}
