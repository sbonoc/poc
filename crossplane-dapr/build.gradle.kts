import java.io.File
import java.time.Instant
import java.util.Locale
import javax.xml.parsers.DocumentBuilderFactory
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.tasks.Delete
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.work.DisableCachingByDefault
import org.w3c.dom.Element

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

@DisableCachingByDefault(because = "Build report aggregation task")
abstract class CollectTestPyramidMetricsTask : DefaultTask() {
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val junitReports: ConfigurableFileCollection

    @get:Input
    abstract val suiteKinds: MapProperty<String, String>

    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty

    @TaskAction
    fun collect() {
        fun parseInt(value: String?): Int = value?.toIntOrNull() ?: 0
        fun parseDouble(value: String?): Double = value?.toDoubleOrNull() ?: 0.0
        fun percentage(part: Double, total: Double): Double = if (total <= 0.0) 0.0 else (part / total) * 100.0
        fun formatDouble(value: Double): String = String.format(Locale.US, "%.4f", value)

        val suiteToKind = suiteKinds.get()
        val kinds = suiteToKind.values.distinct()
        val testsByKind = linkedMapOf<String, Int>().apply { kinds.forEach { put(it, 0) } }
        val failuresByKind = linkedMapOf<String, Int>().apply { kinds.forEach { put(it, 0) } }
        val skippedByKind = linkedMapOf<String, Int>().apply { kinds.forEach { put(it, 0) } }
        val durationByKind = linkedMapOf<String, Double>().apply { kinds.forEach { put(it, 0.0) } }

        val xmlFactory = DocumentBuilderFactory.newInstance().apply {
            isNamespaceAware = false
            setFeature("http://apache.org/xml/features/disallow-doctype-decl", true)
            setFeature("http://xml.org/sax/features/external-general-entities", false)
            setFeature("http://xml.org/sax/features/external-parameter-entities", false)
        }

        fun addIntMetric(map: MutableMap<String, Int>, kind: String, value: Int) {
            map[kind] = map.getValue(kind) + value
        }

        fun addDoubleMetric(map: MutableMap<String, Double>, kind: String, value: Double) {
            map[kind] = map.getValue(kind) + value
        }

        fun extractSuiteName(file: File): String? {
            val marker = "/build/test-results/"
            val path = file.invariantSeparatorsPath
            val markerIdx = path.indexOf(marker)
            if (markerIdx < 0) return null

            val suiteStart = markerIdx + marker.length
            val suiteEnd = path.indexOf('/', suiteStart)
            if (suiteEnd < 0) return null

            return path.substring(suiteStart, suiteEnd)
        }

        fun consumeTestSuite(suite: Element, kind: String) {
            addIntMetric(testsByKind, kind, parseInt(suite.getAttribute("tests")))
            addIntMetric(failuresByKind, kind, parseInt(suite.getAttribute("failures")) + parseInt(suite.getAttribute("errors")))
            addIntMetric(skippedByKind, kind, parseInt(suite.getAttribute("skipped")))
            addDoubleMetric(durationByKind, kind, parseDouble(suite.getAttribute("time")))
        }

        junitReports.files
            .filter { it.isFile && it.name.startsWith("TEST-") && it.extension == "xml" }
            .sortedBy { it.invariantSeparatorsPath }
            .forEach { xmlReport ->
                val suiteTaskName = extractSuiteName(xmlReport) ?: return@forEach
                val kind = suiteToKind[suiteTaskName] ?: return@forEach
                val document = xmlFactory.newDocumentBuilder().parse(xmlReport)
                val root = document.documentElement ?: return@forEach

                if (root.tagName == "testsuite") {
                    consumeTestSuite(root, kind)
                    return@forEach
                }

                if (root.tagName == "testsuites") {
                    val suites = root.getElementsByTagName("testsuite")
                    repeat(suites.length) { index ->
                        val suiteNode = suites.item(index)
                        if (suiteNode is Element) {
                            consumeTestSuite(suiteNode, kind)
                        }
                    }
                }
            }

        val totalTests = testsByKind.values.sum()
        if (totalTests == 0) {
            throw GradleException(
                "No JUnit XML reports were found. Run the stack task (e.g. ./gradlew testPyramidMetricsKtor) or test suites first.",
            )
        }

        val totalDurationSeconds = durationByKind.values.sum()
        val generatedAt = Instant.now()
        val generatedAtEpochSeconds = generatedAt.epochSecond
        val outputDirectory = outputDir.get().asFile

        outputDirectory.mkdirs()

        val summaryJson = buildString {
            appendLine("{")
            appendLine("  \"generatedAt\": \"${generatedAt}\",")
            appendLine("  \"totals\": {")
            appendLine("    \"tests\": $totalTests,")
            appendLine("    \"durationSeconds\": ${formatDouble(totalDurationSeconds)}")
            appendLine("  },")
            appendLine("  \"suites\": [")

            val rows = kinds.mapIndexed { index, kind ->
                val tests = testsByKind.getValue(kind)
                val failures = failuresByKind.getValue(kind)
                val skipped = skippedByKind.getValue(kind)
                val durationSeconds = durationByKind.getValue(kind)
                val testsPct = percentage(tests.toDouble(), totalTests.toDouble())
                val durationPct = percentage(durationSeconds, totalDurationSeconds)
                val suffix = if (index == kinds.size - 1) "" else ","
                """
    {
      "kind": "$kind",
      "tests": $tests,
      "testsPercentage": ${formatDouble(testsPct)},
      "failures": $failures,
      "skipped": $skipped,
      "durationSeconds": ${formatDouble(durationSeconds)},
      "durationPercentage": ${formatDouble(durationPct)}
    }$suffix
                """.trimIndent()
            }

            appendLine(rows.joinToString("\n"))
            appendLine("  ]")
            appendLine("}")
        }

        val summaryFile = outputDirectory.resolve("summary.json")
        summaryFile.writeText(summaryJson)

        val prometheusMetrics = buildString {
            appendLine("# HELP test_pyramid_tests_count Executed tests grouped by test pyramid kind.")
            appendLine("# TYPE test_pyramid_tests_count gauge")
            kinds.forEach { kind ->
                appendLine("test_pyramid_tests_count{kind=\"$kind\"} ${testsByKind.getValue(kind)}")
            }
            appendLine()
            appendLine("# HELP test_pyramid_tests_percentage Percentage of tests grouped by test pyramid kind.")
            appendLine("# TYPE test_pyramid_tests_percentage gauge")
            kinds.forEach { kind ->
                val testsPct = percentage(testsByKind.getValue(kind).toDouble(), totalTests.toDouble())
                appendLine("test_pyramid_tests_percentage{kind=\"$kind\"} ${formatDouble(testsPct)}")
            }
            appendLine()
            appendLine("# HELP test_pyramid_duration_seconds Total test execution time grouped by test pyramid kind.")
            appendLine("# TYPE test_pyramid_duration_seconds gauge")
            kinds.forEach { kind ->
                appendLine("test_pyramid_duration_seconds{kind=\"$kind\"} ${formatDouble(durationByKind.getValue(kind))}")
            }
            appendLine()
            appendLine("# HELP test_pyramid_duration_percentage Percentage of execution time grouped by test pyramid kind.")
            appendLine("# TYPE test_pyramid_duration_percentage gauge")
            kinds.forEach { kind ->
                val durationPct = percentage(durationByKind.getValue(kind), totalDurationSeconds)
                appendLine("test_pyramid_duration_percentage{kind=\"$kind\"} ${formatDouble(durationPct)}")
            }
            appendLine()
            appendLine("# HELP test_pyramid_failures_count Failed tests grouped by test pyramid kind.")
            appendLine("# TYPE test_pyramid_failures_count gauge")
            kinds.forEach { kind ->
                appendLine("test_pyramid_failures_count{kind=\"$kind\"} ${failuresByKind.getValue(kind)}")
            }
            appendLine()
            appendLine("# HELP test_pyramid_skipped_count Skipped tests grouped by test pyramid kind.")
            appendLine("# TYPE test_pyramid_skipped_count gauge")
            kinds.forEach { kind ->
                appendLine("test_pyramid_skipped_count{kind=\"$kind\"} ${skippedByKind.getValue(kind)}")
            }
            appendLine()
            appendLine("# HELP test_pyramid_total_tests Total number of executed tests.")
            appendLine("# TYPE test_pyramid_total_tests gauge")
            appendLine("test_pyramid_total_tests $totalTests")
            appendLine()
            appendLine("# HELP test_pyramid_total_duration_seconds Total execution time across all tests.")
            appendLine("# TYPE test_pyramid_total_duration_seconds gauge")
            appendLine("test_pyramid_total_duration_seconds ${formatDouble(totalDurationSeconds)}")
            appendLine()
            appendLine("# HELP test_pyramid_last_update_unix_seconds Unix epoch time for latest metrics generation.")
            appendLine("# TYPE test_pyramid_last_update_unix_seconds gauge")
            appendLine("test_pyramid_last_update_unix_seconds $generatedAtEpochSeconds")
        }

        val metricsFile = outputDirectory.resolve("test-pyramid.prom")
        metricsFile.writeText(prometheusMetrics)

        logger.lifecycle("Wrote test pyramid metrics to {}", metricsFile)
        logger.lifecycle("Wrote test pyramid summary to {}", summaryFile)
    }
}

val pyramidLevels = listOf(
    "test" to "unit",
    "integrationTest" to "integration",
    "contractTest" to "contract",
    "e2eTest" to "e2e",
)

val ktorProjects = listOf(":producer-ktor", ":consumer-ktor").map { project(it) }
val springBootProjects = listOf(":producer-springboot", ":consumer-springboot").map { project(it) }
val ktorServiceProjects = linkedMapOf(
    "producer-ktor" to project(":producer-ktor"),
    "consumer-ktor" to project(":consumer-ktor"),
)
val springBootServiceProjects = linkedMapOf(
    "producer-springboot" to project(":producer-springboot"),
    "consumer-springboot" to project(":consumer-springboot"),
)
val jvmServiceProjects = linkedMapOf<String, Project>().apply {
    putAll(ktorServiceProjects)
    putAll(springBootServiceProjects)
}

fun String.toTaskSuffix(): String =
    split('-', '_').joinToString("") { part ->
        part.replaceFirstChar { ch ->
            if (ch.isLowerCase()) ch.titlecase(Locale.US) else ch.toString()
        }
    }

fun registerStackSuiteTask(
    taskName: String,
    description: String,
    suiteTaskName: String,
    projects: List<Project>,
) = tasks.register(taskName) {
    group = LifecycleBasePlugin.VERIFICATION_GROUP
    this.description = description
    dependsOn(projects.map { "${it.path}:$suiteTaskName" })
}

val unitTestKtor = registerStackSuiteTask(
    taskName = "unitTestKtor",
    description = "Runs unit tests for Ktor services",
    suiteTaskName = "test",
    projects = ktorProjects,
)
val integrationTestKtor = registerStackSuiteTask(
    taskName = "integrationTestKtor",
    description = "Runs integration tests for Ktor services",
    suiteTaskName = "integrationTest",
    projects = ktorProjects,
)
val contractTestKtor = registerStackSuiteTask(
    taskName = "contractTestKtor",
    description = "Runs contract tests for Ktor services",
    suiteTaskName = "contractTest",
    projects = ktorProjects,
)
val e2eTestKtor = registerStackSuiteTask(
    taskName = "e2eTestKtor",
    description = "Runs e2e tests for Ktor services",
    suiteTaskName = "e2eTest",
    projects = ktorProjects,
)

val unitTestSpringBoot = registerStackSuiteTask(
    taskName = "unitTestSpringBoot",
    description = "Runs unit tests for Spring Boot services",
    suiteTaskName = "test",
    projects = springBootProjects,
)
val integrationTestSpringBoot = registerStackSuiteTask(
    taskName = "integrationTestSpringBoot",
    description = "Runs integration tests for Spring Boot services",
    suiteTaskName = "integrationTest",
    projects = springBootProjects,
)
val contractTestSpringBoot = registerStackSuiteTask(
    taskName = "contractTestSpringBoot",
    description = "Runs contract tests for Spring Boot services",
    suiteTaskName = "contractTest",
    projects = springBootProjects,
)
val e2eTestSpringBoot = registerStackSuiteTask(
    taskName = "e2eTestSpringBoot",
    description = "Runs e2e tests for Spring Boot services",
    suiteTaskName = "e2eTest",
    projects = springBootProjects,
)

val unitTest by tasks.registering {
    group = LifecycleBasePlugin.VERIFICATION_GROUP
    description = "Runs unit tests for JVM services (Ktor + Spring Boot)"
    dependsOn(unitTestKtor, unitTestSpringBoot)
}

val integrationTest by tasks.registering {
    group = LifecycleBasePlugin.VERIFICATION_GROUP
    description = "Runs integration tests for JVM services (Ktor + Spring Boot)"
    dependsOn(integrationTestKtor, integrationTestSpringBoot)
}

val contractTest by tasks.registering {
    group = LifecycleBasePlugin.VERIFICATION_GROUP
    description = "Runs contract tests for JVM services (Ktor + Spring Boot)"
    dependsOn(contractTestKtor, contractTestSpringBoot)
}

val e2eTest by tasks.registering {
    group = LifecycleBasePlugin.VERIFICATION_GROUP
    description = "Runs e2e tests for JVM services (Ktor + Spring Boot)"
    dependsOn(e2eTestKtor, e2eTestSpringBoot)
}

tasks.named("check") {
    dependsOn(unitTest, integrationTest, contractTest)
}

fun registerCollectTask(
    taskName: String,
    description: String,
    outputSubDir: String,
    projects: List<Project>,
) = tasks.register(taskName, CollectTestPyramidMetricsTask::class) {
    group = LifecycleBasePlugin.VERIFICATION_GROUP
    this.description = description
    outputDir.set(layout.buildDirectory.dir("reports/test-pyramid/$outputSubDir"))
    suiteKinds.set(pyramidLevels.toMap())
    junitReports.from(
        projects.map { subproject ->
            subproject.layout.buildDirectory.dir("test-results").map { reportsRoot ->
                reportsRoot.asFileTree.matching {
                    pyramidLevels.forEach { (suiteTaskName, _) ->
                        include("$suiteTaskName/TEST-*.xml")
                    }
                }
            }
        },
    )
}

val collectTestPyramidMetricsKtor = registerCollectTask(
    taskName = "collectTestPyramidMetricsKtor",
    description = "Aggregates Ktor JUnit XML into test pyramid metrics",
    outputSubDir = "ktor",
    projects = ktorProjects,
)
val collectTestPyramidMetricsSpringBoot = registerCollectTask(
    taskName = "collectTestPyramidMetricsSpringBoot",
    description = "Aggregates Spring Boot JUnit XML into test pyramid metrics",
    outputSubDir = "springboot",
    projects = springBootProjects,
)
val collectTestPyramidMetricsJvmServices =
    jvmServiceProjects.mapValues { (serviceName, serviceProject) ->
        registerCollectTask(
            taskName = "collectTestPyramidMetrics${serviceName.toTaskSuffix()}",
            description = "Aggregates $serviceName JUnit XML into test pyramid metrics",
            outputSubDir = serviceName,
            projects = listOf(serviceProject),
        )
    }

fun registerCleanSuiteResultsTask(
    taskName: String,
    description: String,
    projects: List<Project>,
) = tasks.register(taskName, Delete::class) {
    group = LifecycleBasePlugin.VERIFICATION_GROUP
    this.description = description
    projects.forEach { subproject ->
        pyramidLevels.forEach { (suiteTaskName, _) ->
            delete(subproject.layout.buildDirectory.dir("test-results/$suiteTaskName"))
        }
    }
}

val cleanTestPyramidSuiteResultsKtor = registerCleanSuiteResultsTask(
    taskName = "cleanTestPyramidSuiteResultsKtor",
    description = "Deletes Ktor test suite XML used for pyramid aggregation",
    projects = ktorProjects,
)
val cleanTestPyramidSuiteResultsSpringBoot = registerCleanSuiteResultsTask(
    taskName = "cleanTestPyramidSuiteResultsSpringBoot",
    description = "Deletes Spring Boot test suite XML used for pyramid aggregation",
    projects = springBootProjects,
)

unitTestKtor.configure { mustRunAfter(cleanTestPyramidSuiteResultsKtor) }
integrationTestKtor.configure { mustRunAfter(cleanTestPyramidSuiteResultsKtor) }
contractTestKtor.configure { mustRunAfter(cleanTestPyramidSuiteResultsKtor) }
e2eTestKtor.configure { mustRunAfter(cleanTestPyramidSuiteResultsKtor) }

unitTestSpringBoot.configure { mustRunAfter(cleanTestPyramidSuiteResultsSpringBoot) }
integrationTestSpringBoot.configure { mustRunAfter(cleanTestPyramidSuiteResultsSpringBoot) }
contractTestSpringBoot.configure { mustRunAfter(cleanTestPyramidSuiteResultsSpringBoot) }
e2eTestSpringBoot.configure { mustRunAfter(cleanTestPyramidSuiteResultsSpringBoot) }

collectTestPyramidMetricsKtor.configure { dependsOn(unitTestKtor, integrationTestKtor, contractTestKtor) }
collectTestPyramidMetricsSpringBoot.configure { dependsOn(unitTestSpringBoot, integrationTestSpringBoot, contractTestSpringBoot) }
collectTestPyramidMetricsJvmServices.forEach { (serviceName, taskProvider) ->
    val serviceProject = jvmServiceProjects.getValue(serviceName)
    taskProvider.configure {
        dependsOn(
            "${serviceProject.path}:test",
            "${serviceProject.path}:integrationTest",
            "${serviceProject.path}:contractTest",
        )
    }
}

val testPyramidMetricsKtor by tasks.registering {
    group = LifecycleBasePlugin.VERIFICATION_GROUP
    description = "Runs Ktor shift-left suites and exports Ktor pyramid metrics"
    dependsOn(collectTestPyramidMetricsKtor)
}

val testPyramidMetricsSpringBoot by tasks.registering {
    group = LifecycleBasePlugin.VERIFICATION_GROUP
    description = "Runs Spring Boot shift-left suites and exports Spring Boot pyramid metrics"
    dependsOn(collectTestPyramidMetricsSpringBoot)
}
val collectTestPyramidMetricsJvmServicesTask by tasks.registering {
    group = LifecycleBasePlugin.VERIFICATION_GROUP
    description = "Aggregates JVM JUnit XML into per-service test pyramid metrics"
    dependsOn(collectTestPyramidMetricsJvmServices.values)
}
val testPyramidMetricsJvmServices by tasks.registering {
    group = LifecycleBasePlugin.VERIFICATION_GROUP
    description = "Runs JVM shift-left suites and exports per-service pyramid metrics"
    dependsOn(collectTestPyramidMetricsJvmServicesTask)
}
val testPyramidMetricsJvm by tasks.registering {
    group = LifecycleBasePlugin.VERIFICATION_GROUP
    description = "Runs JVM shift-left suites and exports stack + per-service pyramid metrics"
    dependsOn(testPyramidMetricsKtor, testPyramidMetricsSpringBoot, testPyramidMetricsJvmServices)
}

val collectTestPyramidMetrics by tasks.registering {
    group = LifecycleBasePlugin.VERIFICATION_GROUP
    description = "Aggregates JVM test pyramid metrics by stack and service"
    dependsOn(collectTestPyramidMetricsKtor, collectTestPyramidMetricsSpringBoot, collectTestPyramidMetricsJvmServicesTask)
}

val cleanTestPyramidSuiteResults by tasks.registering {
    group = LifecycleBasePlugin.VERIFICATION_GROUP
    description = "Deletes JVM test suite XML used for stack-specific pyramid aggregation"
    dependsOn(cleanTestPyramidSuiteResultsKtor, cleanTestPyramidSuiteResultsSpringBoot)
}

val testPyramidMetrics by tasks.registering {
    group = LifecycleBasePlugin.VERIFICATION_GROUP
    description = "Runs shift-left JVM suites and exports stack + per-service pyramid metrics"
    dependsOn(testPyramidMetricsJvm)
}
