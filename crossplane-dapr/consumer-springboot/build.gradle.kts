plugins {
    java
    id("org.springframework.boot") version "3.4.3"
}

group = "com.agnostic"
version = "1.0.0"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
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
    implementation("org.springframework.boot:spring-boot-starter-web:3.4.3")
    implementation("org.springframework.boot:spring-boot-starter-actuator:3.4.3")
    implementation("io.micrometer:micrometer-registry-prometheus:1.14.5")

    testImplementation("org.springframework.boot:spring-boot-starter-test:3.4.3")
    add("contractTestImplementation", "au.com.dius.pact.consumer:junit5:4.6.17")
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

val pactOutputDir = layout.projectDirectory.dir("pacts")

val cleanGeneratedPacts by tasks.registering(Delete::class) {
    delete(pactOutputDir.asFileTree.matching { include("*.json") })
}

tasks.named<Test>("contractTest") {
    dependsOn(cleanGeneratedPacts)
    outputs.dir(pactOutputDir)
    systemProperty("pact.rootDir", pactOutputDir.asFile.absolutePath)
}

tasks.named("check") {
    dependsOn("integrationTest", "contractTest")
}

tasks.named<Jar>("jar") {
    enabled = false
}

tasks.named<org.springframework.boot.gradle.tasks.bundling.BootJar>("bootJar") {
    archiveFileName.set("app.jar")
}
