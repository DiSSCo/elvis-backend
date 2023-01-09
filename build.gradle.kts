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
    fun ktor(module: String) = "io.ktor:ktor-$module:$ktorVersion"
    fun vertx(module: String) = "io.vertx:$module:$vertxVersion"
    fun kotlinx(module: String) = "org.jetbrains.kotlinx:kotlinx-${module}:$kotlinCoroutinesVersion"

    implementation(kotlinx("coroutines-core"))
    implementation(kotlinx("coroutines-jdk8"))
    implementation(kotlinx("coroutines-reactor"))
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.4.1")

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
    implementation("io.insert-koin:koin-ktor:3.3.0")
    implementation(ktor("server"))
    implementation(ktor("server-core"))
    implementation(ktor("server-netty"))
    implementation(ktor("serialization-jackson"))
    implementation(ktor("server-auth"))
    implementation(ktor("server-auth-jwt"))
    implementation(ktor("server-locations"))
    implementation(ktor("client-cio"))

    // Vertx
    implementation(vertx("vertx-mail-client"))
    implementation(vertx("vertx-pg-client"))
    implementation(vertx("vertx-lang-kotlin"))
    implementation(vertx("vertx-lang-kotlin-coroutines"))
    implementation(vertx("vertx-sql-client"))

    implementation("jakarta.validation:jakarta.validation-api:3.0.2")
    implementation("org.hibernate.validator:hibernate-validator:8.0.0.Final")
    implementation("jakarta.el:jakarta.el-api:5.0.1")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.14.0")
    implementation("software.amazon.awssdk:s3:2.19.8")
    implementation("org.flywaydb:flyway-core:9.10.2")
    implementation("org.xhtmlrenderer:flying-saucer-pdf:9.1.22")
    implementation("org.glassfish:jakarta.el:5.0.0-M1")
}

tasks.withType<KotlinCompile> {
    kotlinOptions.languageVersion = "1.7"
    kotlinOptions.jvmTarget = "17"
}

kotlin.sourceSets["main"].kotlin.srcDirs("src")
kotlin.sourceSets["test"].kotlin.srcDirs("test")

sourceSets["main"].resources.srcDirs("resources")
sourceSets["test"].resources.srcDirs("testresources")

sonarqube {
    properties {
        property("sonar.projectKey", "DiSSCo_elvis-backend")
        property("sonar.organization", "dissco")
        property("sonar.host.url", "https://sonarcloud.io")
    }
}
