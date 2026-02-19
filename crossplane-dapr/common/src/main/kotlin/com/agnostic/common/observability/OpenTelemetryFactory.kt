package com.agnostic.common.observability

import io.opentelemetry.api.OpenTelemetry
import io.opentelemetry.api.common.AttributeKey
import io.opentelemetry.api.common.Attributes
import io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator
import io.opentelemetry.context.propagation.ContextPropagators
import io.opentelemetry.exporter.otlp.http.trace.OtlpHttpSpanExporter
import io.opentelemetry.exporter.otlp.trace.OtlpGrpcSpanExporter
import io.opentelemetry.sdk.OpenTelemetrySdk
import io.opentelemetry.sdk.resources.Resource
import io.opentelemetry.sdk.trace.SdkTracerProvider
import io.opentelemetry.sdk.trace.export.BatchSpanProcessor
import java.time.Duration

object OpenTelemetryFactory {
    private const val DEFAULT_OTLP_ENDPOINT = "http://localhost:4317"
    private const val DEFAULT_OTLP_PROTOCOL = "grpc"
    private const val EXPORTER_TIMEOUT_SECONDS = 5L
    private const val OTEL_RESOURCE_ATTRIBUTES = "OTEL_RESOURCE_ATTRIBUTES"
    private val logger = System.getLogger(OpenTelemetryFactory::class.java.name)

    fun create(serviceName: String): OpenTelemetry {
        val otlpEndpoint = System.getenv("OTEL_EXPORTER_OTLP_ENDPOINT") ?: DEFAULT_OTLP_ENDPOINT
        val otlpProtocol = System.getenv("OTEL_EXPORTER_OTLP_PROTOCOL") ?: DEFAULT_OTLP_PROTOCOL
        val normalizedProtocol = otlpProtocol.lowercase()

        if (normalizedProtocol != "grpc" && normalizedProtocol != "http/protobuf") {
            logger.log(
                System.Logger.Level.WARNING,
                "Unsupported OTLP protocol '$otlpProtocol', falling back to grpc exporter",
            )
        }

        val resource = createResource(serviceName)

        val spanExporter =
            if (normalizedProtocol == "http/protobuf") {
                OtlpHttpSpanExporter
                    .builder()
                    .setEndpoint(otlpEndpoint)
                    .setTimeout(Duration.ofSeconds(EXPORTER_TIMEOUT_SECONDS))
                    .build()
            } else {
                OtlpGrpcSpanExporter
                    .builder()
                    .setEndpoint(otlpEndpoint)
                    .setTimeout(Duration.ofSeconds(EXPORTER_TIMEOUT_SECONDS))
                    .build()
            }

        val tracerProvider =
            SdkTracerProvider
                .builder()
                .setResource(resource)
                .addSpanProcessor(BatchSpanProcessor.builder(spanExporter).build())
                .build()

        return OpenTelemetrySdk
            .builder()
            .setTracerProvider(tracerProvider)
            .setPropagators(ContextPropagators.create(W3CTraceContextPropagator.getInstance()))
            .build()
    }

    private fun createResource(serviceName: String): Resource {
        val attributesBuilder = Attributes.builder()
        attributesBuilder.put(AttributeKey.stringKey("service.name"), serviceName)

        System.getenv(OTEL_RESOURCE_ATTRIBUTES)
            ?.split(",")
            ?.mapNotNull { token ->
                val parts = token.split("=", limit = 2)
                if (parts.size == 2 && parts[0].isNotBlank() && parts[1].isNotBlank()) {
                    parts[0].trim() to parts[1].trim()
                } else {
                    logger.log(
                        System.Logger.Level.WARNING,
                        "Ignoring malformed OTEL_RESOURCE_ATTRIBUTES entry '$token'",
                    )
                    null
                }
            }?.forEach { (key, value) ->
                if (key != "service.name") {
                    attributesBuilder.put(AttributeKey.stringKey(key), value)
                }
            }

        return Resource.getDefault().merge(Resource.create(attributesBuilder.build()))
    }
}
