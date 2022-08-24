package com.sap.ariba.poc.service1.config;

import com.sap.ariba.poc.service1.otel.TextMapGetterImpl;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.propagation.ContextPropagators;
import io.opentelemetry.context.propagation.TextMapPropagator;
import io.opentelemetry.exporter.jaeger.JaegerGrpcSpanExporter;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.SpanProcessor;
import io.opentelemetry.sdk.trace.export.BatchSpanProcessor;
import io.opentelemetry.semconv.resource.attributes.ResourceAttributes;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Configuration
public class AppConfig {

    @Autowired
    private TraceConfig config;

    private OpenTelemetry openTelemetry;
    /**
     * Initializes the OpenTelemetry SDK with a logging span exporter and the W3C Trace Context
     * propagator.
     *
     * @return A ready-to-use {@link OpenTelemetry} instance.
     */
    @Bean
    @ConditionalOnBean(TraceConfig.class)
    public OpenTelemetry initOpenTelemetry() {
        // Use Jaeger Exporter
        SpanProcessor spanProcessor = getJaegerGrpcSpanProcessor();

        Resource serviceNameResource = Resource.create(Attributes.of(ResourceAttributes.SERVICE_NAME, "service-nodes"));

        SdkTracerProvider tracerProvider =
                SdkTracerProvider.builder()
                        .addSpanProcessor(spanProcessor)
                        .setResource(Resource.getDefault().merge(serviceNameResource))
                        .build();

        OpenTelemetrySdk openTelemetrySdk =
                OpenTelemetrySdk.builder().setTracerProvider(tracerProvider)
                        .setPropagators(ContextPropagators.create(W3CTraceContextPropagator.getInstance()))
                        .buildAndRegisterGlobal();

        // add a shutdown hook to shut down the SDK
        Runtime.getRuntime().addShutdownHook(new Thread(tracerProvider::close));

        this.openTelemetry = openTelemetrySdk;
        // return the configured instance so it can be used for instrumentation.
        return openTelemetrySdk;
    }

    private SpanProcessor getJaegerGrpcSpanProcessor(){
        String httpUrl = String.format("http://%s:%s", config.getHost(), config.getPort());
        System.out.println(httpUrl);
        JaegerGrpcSpanExporter exporter =
                JaegerGrpcSpanExporter.builder()
                        .setEndpoint(httpUrl)
                        .build();
        return BatchSpanProcessor.builder(exporter)
                .setScheduleDelay(100, TimeUnit.MILLISECONDS)
                .build();
    }

    @Bean
    public RestTemplate getRestTemplate(){
        return new RestTemplate();
    }


    public OpenTelemetry getOpenTelemetry(){
        return openTelemetry;
    }

    public TextMapPropagator getTextMapPropagator(){
        return openTelemetry.getPropagators().getTextMapPropagator();
    }


    private void wrapHeaders(Map<String, Object> headers) {
        getTextMapPropagator()
                .inject(
                        Context.current(),
                        headers,
                        (carrier, key, value) -> carrier.put(key, value)
                );
    }

    public Context extractContextFromHttpRequest(HttpServletRequest request) {
        return openTelemetry.getPropagators()
                .getTextMapPropagator()
                .extract(Context.current(), request, new TextMapGetterImpl());
    }
}
