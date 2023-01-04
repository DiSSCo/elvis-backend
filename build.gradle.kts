import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

val vertxVersion: String by project
val logbackVersion: String by project
val ktorVersion: String by project
val kotlinVersion: String by project
val kotlinCoroutinesVersion: String by project
val koinVersion: String by project
val detektVersion: String by project
val junitVersion: String by project

group = "org.synthesis"
version = "0.0.1-SNAPSHOT"

buildscript {
    val kotlinVersion = "1.7.21"

    repositories {
        mavenCentral()
        maven {
            url = uri("https://plugins.gradle.org/m2/")
        }
    }

    dependencies {
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:$kotlinVersion")
        classpath("org.sonarsource.scanner.gradle:sonarqube-gradle-plugin:3.4.0.2513")
    }
}

plugins {
    application

    kotlin("jvm") version "1.7.21"
    kotlin("plugin.serialization") version "1.4.30"

    id("io.gitlab.arturbosch.detekt") version "1.9.1"
    id("com.github.honourednihilist.gradle-postgresql-embedded") version "0.4.0"
    id("org.sonarqube") version "3.4.0.2513"
}

application {
    mainClass.set("org.synthesis.MainKt")
}

repositories {
    mavenLocal()
    maven {
        url = uri("https://kotlin.bintray.com/ktor")
    }
    mavenCentral()
}
tasks.withType<Test> {
    useJUnitPlatform()
    testLogging {
        events("passed", "skipped", "failed")
    }
}
dependencies {
    fun ktor(module: String) = "io.ktor:ktor-$module:2.2.1"
    fun vertx(module: String) = "io.vertx:$module:$vertxVersion"
    fun kotlinx(module: String) = "org.jetbrains.kotlinx:kotlinx-${module}:$kotlinCoroutinesVersion"

    implementation(kotlinx("coroutines-core"))
    implementation(kotlinx("coroutines-jdk8"))
    implementation(kotlinx("coroutines-reactor"))
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.4.1")

    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:1.5.1")

    detektPlugins("io.gitlab.arturbosch.detekt:detekt-formatting:$detektVersion")

    implementation("org.postgresql:postgresql:42.5.1")
    implementation("ch.qos.logback:logback-classic:$logbackVersion")
    implementation("org.mindrot:jbcrypt:0.4")
    implementation("org.jsoup:jsoup:1.15.3")

    runtimeOnly("org.bouncycastle:bcprov-jdk15on:1.70")

    /** Keycloak */
    implementation("org.jboss.resteasy:resteasy-jaxrs:3.14.0.Final")
    implementation("org.keycloak:keycloak-admin-client:19.0.3")
    implementation("org.keycloak:keycloak-authz-client:19.0.3")

    // Ktor
    implementation("io.insert-koin:koin-ktor:3.2.2")
    implementation("io.ktor:ktor-server:2.2.1")
    implementation("io.ktor:ktor-server-core:2.2.1")
    implementation("io.ktor:ktor-server-netty:2.2.1")
    implementation("io.ktor:ktor-serialization-jackson:2.2.1")
    implementation("io.ktor:ktor-server-auth-jwt:2.2.1")
    implementation("io.ktor:ktor-server-locations:2.2.1")
    implementation("io.ktor:ktor-client-cio:2.2.1")

    // Vertx
    implementation(vertx("vertx-mail-client"))
    implementation(vertx("vertx-pg-client"))
    implementation(vertx("vertx-lang-kotlin"))
    implementation(vertx("vertx-lang-kotlin-coroutines"))
    implementation(vertx("vertx-sql-client"))

    implementation("org.hibernate:hibernate-validator:8.0.0.Final")
    implementation("javax.el:javax.el-api:3.0.0")
    implementation("org.glassfish:javax.el:3.0.0")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.14.0")
    implementation("software.amazon.awssdk:s3:2.19.4")
    implementation("org.flywaydb:flyway-core:9.10.2")
    implementation("org.xhtmlrenderer:flying-saucer-pdf:9.1.22")

    // Test
    testImplementation("io.ktor:ktor-server-test-host:$ktorVersion")
    testImplementation("org.koin:koin-test:3.3.2")
    testImplementation("org.junit.jupiter:junit-jupiter:$junitVersion")
    testImplementation("io.mockk:mockk:1.13.2")
    testImplementation(vertx("vertx-pg-client"))
    testImplementation(vertx("vertx-lang-kotlin"))
    testImplementation(vertx("vertx-lang-kotlin-coroutines"))
    testImplementation(vertx("vertx-sql-client"))
    testImplementation(ktor("server-tests"))
    testImplementation(ktor("server-test-host"))
    testImplementation("ru.yandex.qatools.embed:postgresql-embedded:2.10")
    testImplementation("de.flapdoodle.embed:de.flapdoodle.embed.process:4.3.4")
    testImplementation("org.testcontainers:testcontainers:1.17.6")
    testImplementation("org.testcontainers:junit-jupiter:1.17.6")
    testImplementation("org.testcontainers:junit-jupiter:1.17.6")
}

tasks.withType<KotlinCompile> {
    kotlinOptions.languageVersion = "1.7"
    kotlinOptions.jvmTarget = "17"
    kotlinOptions.freeCompilerArgs += listOf(
        "-XXLanguage:+InlineClasses"
    )
}

kotlin.sourceSets["main"].kotlin.srcDirs("src")
kotlin.sourceSets["test"].kotlin.srcDirs("test")

sourceSets["main"].resources.srcDirs("resources")
sourceSets["test"].resources.srcDirs("testresources")

//detekt {
//    config = files("detekt.yml")
//    input = files(
//        "src/"
//    )
//    autoCorrect = true
//}

sonarqube {
    properties {
        property("sonar.projectKey", "DiSSCo_elvis-backend")
        property("sonar.organization", "dissco")
        property("sonar.host.url", "https://sonarcloud.io")
    }
}
