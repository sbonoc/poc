plugins {
    base
    alias(libs.plugins.kover)
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.plugin.serialization) apply false
    alias(libs.plugins.ktor) apply false
    alias(libs.plugins.detekt) apply false
    alias(libs.plugins.ktlint) apply false
}

subprojects {
    group = "com.agnostic"
    version = "1.0.0"

    tasks.withType<Test>().configureEach {
        useJUnitPlatform()
        testLogging {
            events("passed", "skipped", "failed")
        }
    }
}

kover {
    reports {
        total {
            verify {
                rule {
                    minBound(80)
                }
            }
        }
    }
}

val unitTest by tasks.registering {
    group = LifecycleBasePlugin.VERIFICATION_GROUP
    description = "Runs unit tests for all modules"
    dependsOn(subprojects.map { "${it.path}:test" })
}

val integrationTest by tasks.registering {
    group = LifecycleBasePlugin.VERIFICATION_GROUP
    description = "Runs integration tests for all modules"
    dependsOn(subprojects.map { "${it.path}:integrationTest" })
}

val contractTest by tasks.registering {
    group = LifecycleBasePlugin.VERIFICATION_GROUP
    description = "Runs contract tests for all modules"
    dependsOn(subprojects.map { "${it.path}:contractTest" })
}

val e2eTest by tasks.registering {
    group = LifecycleBasePlugin.VERIFICATION_GROUP
    description = "Runs smoke E2E tests for all modules"
    dependsOn(subprojects.map { "${it.path}:e2eTest" })
}

tasks.named("check") {
    dependsOn(unitTest, integrationTest, contractTest)
}
