import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion

plugins {
    id("com.gradle.plugin-publish") version "1.2.1"
    kotlin("jvm") version "2.2.0-RC"
    kotlin("plugin.serialization") version "2.2.0-RC"
}

val groupStr = "io.github.klahap.pgen"
val gitRepo = "https://github.com/klahap/pgen"

version = System.getenv("GIT_TAG_VERSION") ?: "1.0.0-SNAPSHOT"
group = groupStr

repositories {
    mavenCentral()
}

dependencies {
    implementation("com.squareup:kotlinpoet:2.0.0")
    implementation("org.postgresql:postgresql:42.7.2")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")
    implementation("com.charleskorn.kaml:kaml:0.66.0")
}

kotlin {
    compilerOptions {
        allWarningsAsErrors = true
        freeCompilerArgs.add("-Xjsr305=strict")
        freeCompilerArgs.add("-Xcontext-parameters")
        jvmTarget.set(JvmTarget.JVM_21)
        languageVersion.set(KotlinVersion.KOTLIN_2_2)
    }
}

gradlePlugin {
    website = gitRepo
    vcsUrl = "$gitRepo.git"

    val generateFrappeDsl by plugins.creating {
        id = groupStr
        implementationClass = "$groupStr.Plugin"
        displayName = "Generate Kotlin Exposed tables from a PostgreSQL database schema"
        description =
            "This Gradle plugin simplifies the development process by automatically generating Kotlin Exposed table definitions from a PostgreSQL database schema. It connects to your database, introspects the schema, and creates Kotlin code for Exposed DSL, including table definitions, column mappings, and relationships. Save time and eliminate boilerplate by keeping your Exposed models synchronized with your database schema effortlessly."
        tags =
            listOf("Kotlin Exposed", "PostgreSQL", "Exposed", "Kotlin DSL", "Database Integration", "Code Generation")
    }
}
