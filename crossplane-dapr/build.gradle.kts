import java.io.File
import java.time.Instant
import java.util.Locale
import javax.xml.parsers.DocumentBuilderFactory
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
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
                "No JUnit XML reports were found. Run ./gradlew testPyramidMetrics or the test suites first.",
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

val collectTestPyramidMetrics by tasks.registering(CollectTestPyramidMetricsTask::class) {
    group = LifecycleBasePlugin.VERIFICATION_GROUP
    description = "Aggregates JUnit XML into test pyramid metrics for Grafana/Prometheus"
    outputDir.set(layout.buildDirectory.dir("reports/test-pyramid"))
    suiteKinds.set(pyramidLevels.toMap())
    junitReports.from(
        subprojects.map { subproject ->
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

val cleanTestPyramidSuiteResults by tasks.registering(Delete::class) {
    group = LifecycleBasePlugin.VERIFICATION_GROUP
    description = "Deletes test suite XML used for test pyramid aggregation"
    subprojects.forEach { subproject ->
        pyramidLevels.forEach { (suiteTaskName, _) ->
            delete(subproject.layout.buildDirectory.dir("test-results/$suiteTaskName"))
        }
    }
}

unitTest.configure {
    mustRunAfter(cleanTestPyramidSuiteResults)
}

integrationTest.configure {
    mustRunAfter(cleanTestPyramidSuiteResults)
}

contractTest.configure {
    mustRunAfter(cleanTestPyramidSuiteResults)
}

e2eTest.configure {
    mustRunAfter(cleanTestPyramidSuiteResults)
}

collectTestPyramidMetrics.configure {
    dependsOn(unitTest, integrationTest, contractTest)
}

val testPyramidMetrics by tasks.registering {
    group = LifecycleBasePlugin.VERIFICATION_GROUP
    description = "Runs shift-left suites (excluding e2e) and exports test pyramid metrics"
    dependsOn(cleanTestPyramidSuiteResults)
    dependsOn(collectTestPyramidMetrics)
}
