package org.synthesis.institution

import io.vertx.sqlclient.Row
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.koin.core.qualifier.named
import org.koin.dsl.module
import org.synthesis.formbuilder.*
import org.synthesis.infrastructure.serializer.JacksonSerializer
import org.synthesis.institution.coordinator.*
import org.synthesis.institution.facility.*
import org.synthesis.institution.members.InstitutionMembersProvider
import org.synthesis.institution.view.InstitutionPresenter
import org.synthesis.search.PostgreSqlSearchAdapter

val institutionsModule = module {

    single<InstitutionStore> {
        PostgresInstitutionStore(
            sqlClient = get(),
            countryFinder = get()
        )
    }

    single {
        InstitutionProvider(
            store = get()
        )
    }

    single<CoordinatorAllocator> {
        PostgresCoordinatorAllocator(
            sqlClient = get()
        )
    }

    single {
        InstitutionPresenter(
            institutionStore = get(),
            coordinatorAllocator = get()
        )
    }

    single {
        InstitutionCountryAllocator(
            institutionStore = get(),
            countryFinder = get()
        )
    }

    single(named("InstitutionSearchAdapter")) {
        PostgreSqlSearchAdapter(
            sqlClient = get(),
            table = "institutions",
            transformer = fun(row: Row): InstitutionResponse {
                val coordinatorAllocator: CoordinatorAllocator = get()
                val id = InstitutionId(GRID(row.getString("id")))
                val name = row.getString("title")
                val cetaf = CETAF(row.getString("cetaf"))
                val formData = JacksonSerializer.unserialize(
                    row.getString("data"), DynamicForm::class.java
                )
                return InstitutionResponse(
                    id = id,
                    name = name,
                    fieldValues = formData.normalize(),
                    cetaf = cetaf,
                    coordinators = runBlocking { coordinatorAllocator.all(id).toList() }
                )
            }
        )
    }

    single<FacilityStore> {
        PgFacilityStore(
            sqlClient = get()
        )
    }
    single {
        FacilityProvider(
            store = get()
        )
    }

    single<FacilityPresenter> {
        DefaultFacilityPresenter(
            sqlClient = get(),
            attachmentProvider = get()
        )
    }

    single {
        CoordinatorManager(
            userAccountFinder = get(),
            userAccountProvider = get(),
            taCallRequestStore = get(),
            vaCallRequestStore = get(),
            coordinatorStore = get()
        )
    }
    single {
        PgCoordinatorStore(
            sqlClient = get()
        )
    }

    single {
        InstitutionMembersProvider(
            userAccountFinder = get(),
            userAccountProvider = get()
        )
    }

    @Suppress("NestedBlockDepth")
    single(named("FacilitySearchAdapter")) {

        val institutionStore: InstitutionStore = get()

        /**
         * @todo: fix me
         */
        PostgreSqlSearchAdapter(
            table = "institution_facilities",
            sqlClient = get(),
            transformer = fun(row: Row): FacilityResponse {
                val institution = runBlocking {
                    institutionStore.findById(InstitutionId.fromString(row.getString("institution_id")))
                        ?: error("Cant find institution")
                }

                val formData = JacksonSerializer.unserialize(
                    row.getString("data"), DynamicForm::class.java
                )

                return FacilityResponse(
                    id = row.getUUID("id"),
                    institutionName = institution.title(),
                    institutionId = institution.id(),
                    fieldValues = formData.normalize(),
                    images = row.getArrayOfStrings("images").toList()
                )
            }
        )
    }
}
