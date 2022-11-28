package org.synthesis.infrastructure

import io.ktor.client.*
import io.ktor.client.engine.cio.CIO
import io.ktor.util.*
import io.vertx.pgclient.PgConnectOptions
import io.vertx.pgclient.PgPool
import io.vertx.sqlclient.PoolOptions
import io.vertx.sqlclient.SqlClient
import kotlinx.coroutines.ObsoleteCoroutinesApi
import org.koin.core.context.startKoin
import org.koin.dsl.module
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.synthesis.account.accountModule
import org.synthesis.attachment.attachmentsModule
import org.synthesis.auth.authModule
import org.synthesis.calls.callModule
import org.synthesis.comment.commentsModule
import org.synthesis.contact.contactModule
import org.synthesis.country.currencyModule
import org.synthesis.environment.environmentModule
import org.synthesis.infrastructure.filesystem.Filesystem
import org.synthesis.infrastructure.filesystem.S3Filesystem
import org.synthesis.infrastructure.mailer.*
import org.synthesis.infrastructure.persistence.*
import org.synthesis.institution.institutionsModule
import org.synthesis.keycloak.keycloakModule
import org.synthesis.reporting.reportingModule
import org.synthesis.search.searchModule
import org.synthesis.settings.settingsModule
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.S3AsyncClient

@KtorExperimentalAPI
val elvisModule = module {

    single {
        StorageConfiguration(
            host = getProperty("DATABASE_HOST", "localhost"),
            port = getPropertyOrNull("DATABASE_PORT")?.toInt() ?: 5432,
            database = getProperty("DATABASE_NAME"),
            username = getProperty("DATABASE_USERNAME"),
            password = getProperty("DATABASE_PASSWORD")
        )
    }

    single<Migrator> {
        FlyWayMigrator(
            configuration = get()
        )
    }

    single {
        val configuration = get<StorageConfiguration>()

        val connectionOptions = PgConnectOptions().apply {
            port = configuration.port
            host = configuration.host
            database = configuration.database
            user = configuration.username
            password = configuration.password
        }

        val poolOptions = PoolOptions().apply {
            maxSize = getPropertyOrNull("DATABASE_POOL_SIZE")?.toInt() ?: 5
        }

        PgPool.pool(connectionOptions, poolOptions)
    }

    single<SqlClient> {
        get<PgPool>()
    }

    single<Filesystem> {
        val client = S3AsyncClient.builder()
            .apply {
                credentialsProvider(
                    StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(
                            getProperty("S3_ACCESS_KEY"),
                            getProperty("S3_SECRET_KEY")
                        )
                    )
                )
                region(Region.of(getProperty("S3_REGION")))
            }
            .build()

        S3Filesystem(client, getProperty("S3_BUCKET"))
    }

    single {
        HttpClient(CIO) {
            expectSuccess = false
        }
    }

    single {
        when (getProperty("MAILER")) {
            "vertx" -> get<VertexMailer>()
            else -> get<ConsoleMailer>()
        }
    }

    single {
        ConsoleMailer(
            templateOptions = get(),
            logger = get()
        )
    }

    single {
        VertexMailer(
            config = MailerConfig.Smtp(
                host = getPropertyOrNull("MAILER_SMTP_HOSTNAME"),
                port = getPropertyOrNull("MAILER_SMTP_PORT")?.toInt(),
                ssl = getPropertyOrNull("MAILER_SMTP_SSL")?.toBoolean() ?: false,
                username = getPropertyOrNull("MAILER_SMTP_USERNAME"),
                password = getPropertyOrNull("MAILER_SMTP_PASSWORD"),
                starttls = getPropertyOrNull("MAILER_SMTP_TLS")?.let { true } ?: false
            ),
            templateOptions = get()
        )
    }

    single {
        EmailTemplateOptions(
            frontendUrl = getProperty("FRONTEND_URL"),
            supportEmail = getProperty("SUPPORT_EMAIL"),
            fromEmail = getProperty("MAILER_FROM_EMAIL"),
            fromName = getProperty("MAILER_FROM_NAME"),
            templatePath = "/notification/email_template.html"
        )
    }

    single<Logger> {
        LoggerFactory.getLogger("elvis")
    }
}

@KtorExperimentalAPI
@ObsoleteCoroutinesApi
val koinModules = listOf(
    elvisModule,
    authModule,
    callModule,
    institutionsModule,
    commentsModule,
    attachmentsModule,
    contactModule,
    searchModule,
    settingsModule,
    environmentModule,
    accountModule,
    keycloakModule,
    currencyModule,
    reportingModule
)

@KtorExperimentalAPI
@ObsoleteCoroutinesApi
fun loadDependencies() {
    startKoin {
        modules(koinModules)
        fileProperties()
        environmentProperties()
    }
}
