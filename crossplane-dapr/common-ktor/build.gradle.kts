plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.plugin.serialization)
    alias(libs.plugins.detekt)
    alias(libs.plugins.ktlint)
}

kotlin {
    jvmToolchain(21)
}

val integrationTestSourceSet = sourceSets.create("integrationTest")
val contractTestSourceSet = sourceSets.create("contractTest")
val e2eTestSourceSet = sourceSets.create("e2eTest")

val mainOutput = sourceSets["main"].output
val testRuntimeClasspath = configurations["testRuntimeClasspath"]

listOf(integrationTestSourceSet, contractTestSourceSet, e2eTestSourceSet).forEach { sourceSet ->
    sourceSet.compileClasspath += mainOutput + testRuntimeClasspath
    sourceSet.runtimeClasspath += sourceSet.output + sourceSet.compileClasspath
}

configurations[integrationTestSourceSet.implementationConfigurationName].extendsFrom(
    configurations.testImplementation.get(),
)
configurations[integrationTestSourceSet.runtimeOnlyConfigurationName].extendsFrom(configurations.testRuntimeOnly.get())
configurations[contractTestSourceSet.implementationConfigurationName].extendsFrom(
    configurations.testImplementation.get(),
)
configurations[contractTestSourceSet.runtimeOnlyConfigurationName].extendsFrom(configurations.testRuntimeOnly.get())
configurations[e2eTestSourceSet.implementationConfigurationName].extendsFrom(configurations.testImplementation.get())
configurations[e2eTestSourceSet.runtimeOnlyConfigurationName].extendsFrom(configurations.testRuntimeOnly.get())

dependencies {
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.opentelemetry.api)
    implementation(libs.opentelemetry.sdk.autoconfigure)
    implementation(libs.opentelemetry.exporter.otlp)

    testImplementation(libs.junit.jupiter)
    testImplementation(libs.assertj.core)
    testRuntimeOnly(libs.junit.platform.launcher)
}

fun registerSuiteTask(
    name: String,
    sourceSet: SourceSet,
    mustRunAfterTask: String,
) {
    tasks.register<Test>(name) {
        description = "Runs the $name suite."
        group = LifecycleBasePlugin.VERIFICATION_GROUP
        testClassesDirs = sourceSet.output.classesDirs
        classpath = sourceSet.runtimeClasspath
        useJUnitPlatform()
        mustRunAfter(tasks.named(mustRunAfterTask))
    }
}

registerSuiteTask("integrationTest", integrationTestSourceSet, "test")
registerSuiteTask("contractTest", contractTestSourceSet, "integrationTest")
registerSuiteTask("e2eTest", e2eTestSourceSet, "contractTest")

tasks.named("check") {
    dependsOn("integrationTest", "contractTest")
}

detekt {
    buildUponDefaultConfig = true
    allRules = false
}

ktlint {
    version.set("1.2.1")
}
