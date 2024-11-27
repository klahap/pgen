import com.vanniktech.maven.publish.SonatypeHost
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion

plugins {
    kotlin("jvm") version "2.0.20"
    id("com.vanniktech.maven.publish") version "0.28.0"
}

repositories {
    mavenCentral()
}

val githubUser = "klahap"
val githubProject = "pgen"
val artifactId = "pgenlib"
val groupStr = "io.github.$githubUser"
val versionStr = System.getenv("GIT_TAG_VERSION") ?: "1.0-SNAPSHOT"
group = groupStr
version = versionStr

kotlin {
    explicitApi()
    compilerOptions {
        allWarningsAsErrors = true
        apiVersion.set(KotlinVersion.KOTLIN_2_0)
        languageVersion.set(KotlinVersion.KOTLIN_2_0)
    }
    dependencies {
        implementation("org.postgresql:postgresql:42.7.4")
        implementation("org.jetbrains.exposed:exposed-core:0.56.0")
    }
}

mavenPublishing {
    coordinates(
        groupId = project.group as String,
        artifactId = artifactId,
        version = project.version as String
    )
    pom {
        name = artifactId
        description = "Util code for the Kotlin Exposed generated tables"
        url = "https://github.com/$githubUser/$githubProject"
        licenses {
            license {
                name = "MIT License"
                url = "https://github.com/$githubUser/$githubProject/blob/main/LICENSE"
            }
        }
        developers {
            developer {
                id = githubUser
                name = githubUser
                url = "https://github.com/$githubUser"
            }
        }
        scm {
            url = "https://github.com/${githubUser}/${githubProject}"
            connection = "scm:git:https://github.com/${githubUser}/${githubProject}.git"
            developerConnection = "scm:git:git@github.com:${githubUser}/${githubProject}.git"
        }
    }
    publishToMavenCentral(
        SonatypeHost.CENTRAL_PORTAL,
        automaticRelease = true,
    )
    signAllPublications()
}
