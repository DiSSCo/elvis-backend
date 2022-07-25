package org.synthesis.comment

import org.koin.dsl.module

val commentsModule = module {

    single<CommentStore> {
        PgCommentStore(
            sqlClient = get()
        )
    }

    single {
        CommentProvider(
            store = get(),
            finder = get()
        )
    }

    single<CommentFinder> {
        PgCommentFinder(
            sqlClient = get()
        )
    }
}
