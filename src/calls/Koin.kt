package org.synthesis.calls

import io.vertx.sqlclient.Row
import java.time.LocalDateTime
import java.util.*
import kotlinx.coroutines.runBlocking
import org.koin.core.qualifier.named
import org.koin.dsl.module
import org.synthesis.account.*
import org.synthesis.attachment.AttachmentCollection
import org.synthesis.calls.contract.CallResponse
import org.synthesis.calls.request.comment.*
import org.synthesis.calls.request.flow.CallRequestFlowProxy
import org.synthesis.calls.request.ta.export.TaCallRequestExporter
import org.synthesis.calls.request.ta.export.task.TaCallRequestCommentsOverview
import org.synthesis.calls.request.ta.export.task.TaCallRequestOverview
import org.synthesis.calls.request.ta.flow.TaCallRequestFlow
import org.synthesis.calls.request.ta.presenter.TaCallRequestPresenter
import org.synthesis.calls.request.ta.scoring.PostgresScorerAllocator
import org.synthesis.calls.request.ta.scoring.ScoreFinder
import org.synthesis.calls.request.ta.scoring.ScorePresenter
import org.synthesis.calls.request.ta.scoring.ScorerAllocator
import org.synthesis.calls.request.ta.store.TaCallRequestFinder
import org.synthesis.calls.request.ta.store.TaCallRequestStore
import org.synthesis.calls.request.va.attachmnent.*
import org.synthesis.calls.request.va.export.VaCallRequestExporter
import org.synthesis.calls.request.va.export.task.VaCallRequestCommentsOverview
import org.synthesis.calls.request.va.export.task.VaCallRequestOverview
import org.synthesis.calls.request.va.flow.VaCallRequestFlow
import org.synthesis.calls.request.va.presenter.VaCallRequestPresenter
import org.synthesis.calls.request.va.store.VaCallRequestFinder
import org.synthesis.calls.request.va.store.VaCallRequestStore
import org.synthesis.calls.store.*
import org.synthesis.infrastructure.asHtmlToPdfWriter
import org.synthesis.institution.InstitutionId
import org.synthesis.search.PostgreSqlSearchAdapter
import org.xhtmlrenderer.pdf.ITextRenderer

val callModule = module {

    single<CallFinder> {
        PostgresCallFinder(
            sqlClient = get()
        )
    }

    single<CallStore> {
        PostgresCallStore(
            sqlClient = get()
        )
    }
    single<ScorerAllocator> {
        PostgresScorerAllocator(
            sqlClient = get()
        )
    }
    single {
        CallProvider(
            store = get(),
            finder = get()
        )
    }

    single {
        VaCallRequestFinder(
            sqlClient = get()
        )
    }

    single {
        TaCallRequestFinder(
            sqlClient = get(),
            countryFinder = get()
        )
    }

    single {
        VaCallRequestStore(
            sqlClient = get()
        )
    }

    single {
        TaCallRequestStore(
            sqlClient = get()
        )
    }
    single {
        PostgresScorerAllocator(
            sqlClient = get()
        )
    }

    single {
        VaCallRequestFlow(
            vaCallRequestFinder = get(),
            vaCallRequestStore = get(),
            institutionStore = get(),
            coordinatorAllocator = get(),
            mailer = get(),
            keycloakClient = get(),
            logger = get()
        )
    }

    single {
        TaCallRequestFlow(
            taCallRequestFinder = get(),
            taCallRequestStore = get(),
            coordinatorAllocator = get(),
            mailer = get(),
            keycloakClient = get(),
            logger = get(),
            institutionStore = get(),
            scorerAllocator = get(),
            institutionCountryAllocator = get()
        )
    }

    single {
        CallRequestFlowProxy(
            callFinder = get(),
            vaCallRequestFlow = get(),
            taCallRequestFlow = get(),
            vaCallRequestPresenter = get(),
            taCallRequestPresenter = get(),
            taCallRequestExporter = get(),
            vaCallRequestExporter = get()
        )
    }

    single {
        VaCallRequestPresenter(
            vaCallRequestFinder = get(),
            institutionStore = get(),
            userAccountFinder = get()
        )
    }
    single {
        TaCallRequestPresenter(
            taCallRequestFinder = get(),
            userAccountFinder = get(),
            institutionStore = get()
        )
    }

    single<VaCallRequestAttachmentFinder> {
        DefaultVaCallRequestAttachmentFinder(
            sqlClient = get()
        )
    }

    single {
        AttachmentCollection(
            getProperty("S3_REQUESTS_BUCKET")
        )
    }

    single<VaCallRequestAttachmentStore> {
        DefaultVaCallRequestAttachmentStore(
            sqlClient = get(),
            attachmentProvider = get(),
            collection = get()
        )
    }

    single {
        VaCallRequestAttachmentPresenter(
            finder = get(),
            attachmentProvider = get(),
            collection = get(),
            userAccountFinder = get()
        )
    }

    single<CallRequestCommentsFinder> {
        ProxyCallRequestCommentsFinder(
            finder = get()
        )
    }

    single<CallRequestCommentsHandler> {
        ProxyCallRequestCommentsHandler(
            commentProvider = get()
        )
    }

    single {
        CallRequestCommentsPresenter(
            callRequestCommentsFinder = get(),
            userAccountFinder = get()
        )
    }

    single {
        RequestTypeAllocator(
            sqlClient = get()
        )
    }

    single {
        VaCallRequestExporter(
            vaCallRequestFinder = get(),
            taskCollection = mapOf(
                "overview" to VaCallRequestOverview(
                    institutionStore = get()
                ),
                "comments" to VaCallRequestCommentsOverview(
                    commentFinder = get(),
                    userAccountFinder = get()
                )
            ),
            htmlToPdfWriter = ITextRenderer().asHtmlToPdfWriter(),
        )
    }
    single {
        TaCallRequestExporter(
            taCallRequestFinder = get(),
            taskCollection = mapOf(
                "overview" to TaCallRequestOverview(
                    institutionStore = get(),
                    facilityStore = get()
                ),
                "comments" to TaCallRequestCommentsOverview(
                    commentFinder = get(),
                    userAccountFinder = get()
                )
            ),
            htmlToPdfWriter = ITextRenderer().asHtmlToPdfWriter(),
        )
    }
    single {
        ScoreFinder(
            sqlClient = get()
        )
    }

    single {
        ScorePresenter(
            scoreFinder = get()
        )
    }

    single(named("CallSearchAdapter")) {
        PostgreSqlSearchAdapter(
            sqlClient = get(),
            table = "calls",
            transformer = fun(row: Row): CallResponse {
                val fromDate = row.getLocalDateTime("start_date")
                val endDate = row.getLocalDateTime("end_date")
                val deletedAt = row.getLocalDateTime("deleted_at")

                return CallResponse(
                    id = row.getUUID("id"),
                    name = row.getString("title"),
                    description = row.getString("description"),
                    additionalInfo = row.getString("additional_info"),
                    type = row.getString("call_type").toLowerCase(),
                    startDate = fromDate,
                    endDate = endDate,
                    status = if (endDate != null && endDate >= LocalDateTime.now() && deletedAt == null) {
                        "available"
                    } else {
                        "ended"
                    },
                    scoring = ScoringData(
                        endDate = row.getLocalDateTime("scoring_end_date"),
                        weight = ScoringWeight(
                            methodology = row.getString("methodology"),
                            researchExcellence = row.getString("research_excellence"),
                            supportStatement = row.getString("support_statement"),
                            justification = row.getString("justification"),
                            gainsOutputs = row.getString("gains_outputs"),
                            societalChallenge = row.getString("societal_challenge"),
                            merit = row.getString("merit"),
                        )
                    )
                )
            }
        )
    }

    single(named("CallRequestsSearchAdapter")) {
        data class RequesterData(
            val id: UUID,
            val email: String,
            val firstName: String,
            val lastName: String,
            val orcId: OrcId?,
            val status: String = "Enabled"
        )

        data class CallListApiResponse(
            val id: UUID,
            val callId: UUID,
            val title: String?,
            val requestType: String,
            val requestDate: LocalDateTime,
            val status: String,
            val creator: RequesterData?
        )

        fun UserAccount.asRequesterData() = RequesterData(
            id = id.uuid,
            email = email,
            firstName = fullName.firstName,
            lastName = fullName.lastName,
            orcId = attributes.orcId,
            status = when (status) {
                is UserAccountStatus.Banned -> "Disabled"
                is UserAccountStatus.Inactive -> "Disabled"
                is UserAccountStatus.Active -> "Enabled"
            }
        )

        PostgreSqlSearchAdapter(
            table = "requests",
            sqlClient = get(),
            transformer = fun(row: Row): CallListApiResponse {
                val userAccountFinder: UserAccountFinder = get()

                return CallListApiResponse(
                    id = row.getUUID("id"),
                    callId = row.getUUID("call_id"),
                    title = row.getString("title"),
                    requestType = row.getString("type"),
                    requestDate = row.getLocalDateTime("created_at"),
                    status = row.getString("status"),
                    /** @todo: move to separated data transformer */
                    creator = runBlocking {
                        userAccountFinder.find(UserAccountId(row.getUUID("requester_id")))?.asRequesterData()
                    }
                )
            }
        )
    }

    single(named("InstitutionCallRequestsSearchAdapter")) {

        data class InstitutionFormApiResponse(
            val id: UUID,
            val requestId: UUID,
            val institutionId: InstitutionId,
            val coordinatorId: UUID,
            val status: String,
            val type: String,
            val isDeleted: Boolean
        )

        PostgreSqlSearchAdapter(
            table = "requests_institution_forms",
            sqlClient = get(),
            transformer = fun(row: Row): InstitutionFormApiResponse {
                val requestTypeAllocator: RequestTypeAllocator = get()

                val deletedAt = row.getLocalDateTime("deleted_at")

                return InstitutionFormApiResponse(
                    id = row.getUUID("id"),
                    requestId = row.getUUID("request_id"),
                    institutionId = InstitutionId.fromString(row.getString("institution_id")),
                    coordinatorId = row.getUUID("coordinator_id"),
                    type = runBlocking {
                        requestTypeAllocator.allocate(row.getUUID("id"))?.name?.toLowerCase() ?: ""
                    },
                    status = row.getString("status"),
                    isDeleted = deletedAt != null
                )
            }
        )
    }
}
