package org.synthesis.settings

import io.vertx.sqlclient.Row
import org.koin.core.qualifier.named
import org.koin.dsl.module
import org.synthesis.search.PostgreSqlSearchAdapter

val settingsModule = module {
    single<SettingsStore> {
        PgSettingsStore(
            sqlClient = get()
        )
    }

    single {
        SettingsPresenter(
            settingsStore = get()
        )
    }

    single(named("SettingsSearchAdapter")) {
        PostgreSqlSearchAdapter(
            sqlClient = get(),
            table = "settings",
            transformer = fun(row: Row): SettingsParameter {
                val optionType = row.getString("option_type")

                return SettingsParameter(
                    key = row.getString("option_key"),
                    type = row.getString("option_type"),
                    value = when (optionType) {
                        "string" -> row.getString("option_value")
                        "integer" -> row.getInteger("option_value")
                        else -> throw SettingException.UnsupportedType(optionType)
                    }
                )
            }
        )
    }
}
