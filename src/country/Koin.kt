package org.synthesis.country

import org.koin.dsl.module

val currencyModule = module {

    single<CountryFinder> {
        DefaultCountryFinder(
            sqlClient = get()
        )
    }

    single {
        CountryManager(
            userAccountFinder = get(),
            userAccountProvider = get()
        )
    }
}
