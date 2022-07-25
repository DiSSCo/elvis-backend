package org.synthesis.reporting

import org.koin.dsl.module

val reportingModule = module {

    single {
        ReportingPresenter(
            reportingReceiver = get()
        )
    }

    single<ReportingReceiver> {
        PgReportingReceiver(
            sqlClient = get()
        )
    }
}
