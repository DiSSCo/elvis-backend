package org.synthesis.attachment

import org.koin.dsl.module

val attachmentsModule = module {

    single<AttachmentReferenceStore> {
        PgAttachmentReferenceStore(
            sqlClient = get()
        )
    }

    single {
        AttachmentProvider(
            store = get(),
            filesystem = get()
        )
    }
}
