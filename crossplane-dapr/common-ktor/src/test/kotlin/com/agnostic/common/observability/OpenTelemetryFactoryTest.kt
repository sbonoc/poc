package com.agnostic.common.observability

import io.opentelemetry.api.common.AttributeKey
import io.opentelemetry.sdk.OpenTelemetrySdk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class OpenTelemetryFactoryTest {
    @Test
    fun `creates opentelemetry instance with grpc exporter by default`() {
        val otel = OpenTelemetryFactory.create("test-service") { null }

        assertThat(otel).isInstanceOf(OpenTelemetrySdk::class.java)
    }

    @Test
    fun `creates opentelemetry instance with http protobuf exporter`() {
        val otel =
            OpenTelemetryFactory.create("test-service") { key ->
                when (key) {
                    "OTEL_EXPORTER_OTLP_PROTOCOL" -> "http/protobuf"
                    else -> null
                }
            }

        assertThat(otel).isInstanceOf(OpenTelemetrySdk::class.java)
    }

    @Test
    fun `falls back to grpc exporter for unsupported protocol`() {
        val otel =
            OpenTelemetryFactory.create("test-service") { key ->
                when (key) {
                    "OTEL_EXPORTER_OTLP_PROTOCOL" -> "thrift"
                    else -> null
                }
            }

        assertThat(otel).isInstanceOf(OpenTelemetrySdk::class.java)
    }

    @Test
    fun `uses custom endpoint from environment variable`() {
        val otel =
            OpenTelemetryFactory.create("test-service") { key ->
                when (key) {
                    "OTEL_EXPORTER_OTLP_ENDPOINT" -> "http://custom-collector:4317"
                    else -> null
                }
            }

        assertThat(otel).isInstanceOf(OpenTelemetrySdk::class.java)
    }

    @Test
    fun `sets service name as resource attribute`() {
        val resource = OpenTelemetryFactory.createResource("my-service") { null }
        val serviceName: String? = resource.attributes.get(AttributeKey.stringKey("service.name"))

        assertThat(serviceName).isEqualTo("my-service")
    }

    @Test
    fun `parses valid resource attributes from environment variable`() {
        val resource =
            OpenTelemetryFactory.createResource("my-service") { key ->
                when (key) {
                    "OTEL_RESOURCE_ATTRIBUTES" -> "env=production,region=us-east-1"
                    else -> null
                }
            }
        val env: String? = resource.attributes.get(AttributeKey.stringKey("env"))
        val region: String? = resource.attributes.get(AttributeKey.stringKey("region"))

        assertThat(env).isEqualTo("production")
        assertThat(region).isEqualTo("us-east-1")
    }

    @Test
    fun `ignores malformed resource attribute entries`() {
        val resource =
            OpenTelemetryFactory.createResource("my-service") { key ->
                when (key) {
                    "OTEL_RESOURCE_ATTRIBUTES" -> "valid.key=valid-value,malformed,=nokey"
                    else -> null
                }
            }
        val valid: String? = resource.attributes.get(AttributeKey.stringKey("valid.key"))
        val malformed: String? = resource.attributes.get(AttributeKey.stringKey("malformed"))

        assertThat(valid).isEqualTo("valid-value")
        assertThat(malformed).isNull()
    }

    @Test
    fun `service name cannot be overridden via resource attributes`() {
        val resource =
            OpenTelemetryFactory.createResource("my-service") { key ->
                when (key) {
                    "OTEL_RESOURCE_ATTRIBUTES" -> "service.name=other-service"
                    else -> null
                }
            }
        val serviceName: String? = resource.attributes.get(AttributeKey.stringKey("service.name"))

        assertThat(serviceName).isEqualTo("my-service")
    }
}
