package org.synthesis.settings

import io.vertx.sqlclient.Row
import io.vertx.sqlclient.SqlClient
import java.util.*
import org.synthesis.infrastructure.persistence.StorageException
import org.synthesis.infrastructure.persistence.querybuilder.*

interface SettingsStore {
    /**
     * @throws [SettingException.UnsupportedType]
     * @throws [StorageException.InteractingFailed]
     */
    suspend fun find(key: String): Parameter?

    /**
     * @throws [StorageException.InteractingFailed]
     */
    suspend fun save(parameter: Parameter)
}

class PgSettingsStore(
    private val sqlClient: SqlClient
) : SettingsStore {
    override suspend fun find(key: String): Parameter? = sqlClient.fetchOne(
        select("settings") {
            where {
                "option_key" eq key
            }
        }
    )?.hydrate()

    override suspend fun save(parameter: Parameter) {
        val optionValueType = parameter.value.type()
        val optionValue: Any? = when (parameter.value) {
            is ParameterValue.StringValue -> parameter.value.value
            is ParameterValue.IntValue -> parameter.value.value
        }

        sqlClient.execute(
            insert(
                "settings", mapOf(
                    "id" to UUID.randomUUID(),
                    "option_key" to parameter.key,
                    "option_value" to optionValue,
                    "option_type" to optionValueType
                )
            ) {
                onConflict(
                    columns = listOf("option_key"),
                    action = OnConflict.DoUpdate(
                        mapOf(
                            "option_value" to optionValue,
                            "option_type" to optionValueType
                        )
                    )
                )
            }
        )
    }

    private fun Row.hydrate() = Parameter(
        key = getString("option_key"),
        value = when (getString("option_type")) {
            "string" -> ParameterValue.StringValue(getString("option_value"))
            "integer" -> ParameterValue.IntValue(getInteger("option_value"))
            else -> throw SettingException.UnsupportedType(getString("option_type"))
        }
    )

    private fun ParameterValue.type(): String = when (this) {
        is ParameterValue.StringValue -> "string"
        is ParameterValue.IntValue -> "integer"
    }
}
