plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.plugin.serialization)
    alias(libs.plugins.ktor)
    alias(libs.plugins.detekt)
    alias(libs.plugins.ktlint)
}

kotlin {
    jvmToolchain(21)
}

application {
    mainClass = "io.ktor.server.netty.EngineMain"
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
    implementation(project(":common-ktor"))

    implementation(libs.ktor.server.core)
    implementation(libs.ktor.server.netty)
    implementation(libs.ktor.server.config.yaml)
    implementation(libs.ktor.server.content.negotiation)
    implementation(libs.ktor.serialization.kotlinx.json)
    implementation(libs.ktor.server.status.pages)
    implementation(libs.ktor.server.call.logging)
    implementation(libs.ktor.server.metrics.micrometer)
    implementation(libs.logback.classic)
    implementation(libs.dapr.sdk)
    implementation(libs.kotlinx.coroutines.reactor)
    implementation(libs.micrometer.registry.prometheus)
    implementation(libs.opentelemetry.api)
    implementation(libs.opentelemetry.sdk.autoconfigure)
    implementation(libs.opentelemetry.exporter.otlp)

    testImplementation(libs.ktor.server.test.host)
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.mockk)
    testImplementation(libs.assertj.core)
    testRuntimeOnly(libs.junit.platform.launcher)

    add("integrationTestImplementation", libs.ktor.server.test.host)
    add("integrationTestImplementation", libs.junit.jupiter)
    add("integrationTestImplementation", libs.mockk)
    add("integrationTestImplementation", libs.assertj.core)

    add("contractTestImplementation", project(":common-ktor"))
    add("contractTestImplementation", libs.junit.jupiter)
    add("contractTestImplementation", libs.pact.provider.junit5)

    add("e2eTestImplementation", libs.junit.jupiter)
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

tasks.named("contractTest") {
    dependsOn(":consumer-ktor:contractTest")
}

tasks.named<Test>("contractTest") {
    inputs.files(fileTree("${project.projectDir}/../consumer-ktor/pacts") { include("*.json") })
}

tasks.named("check") {
    dependsOn("integrationTest", "contractTest")
}

// Fixes the gRPC NameResolverProvider overwrite issue in Fat JARs
tasks.withType<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar> {
    mergeServiceFiles()
    duplicatesStrategy = DuplicatesStrategy.INCLUDE
}

detekt {
    buildUponDefaultConfig = true
    allRules = false
}

ktlint {
    version.set("1.2.1")
}
