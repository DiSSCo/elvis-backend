package org.synthesis.search

import org.koin.dsl.module

val searchModule = module {

    single {
        SearchProviderLocator(
            koin = this.getKoin()
        )
    }
}
