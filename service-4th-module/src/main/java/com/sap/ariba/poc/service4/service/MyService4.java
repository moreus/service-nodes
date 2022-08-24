package com.sap.ariba.poc.service4.service;


import com.sap.ariba.poc.service4.config.AppConfig;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class MyService4 {
    private static final Logger logger = LoggerFactory.getLogger(MyService4.class);

    @Autowired
    private AppConfig appConfig;

    public String myService4() {
        String correlationId = MDC.get("correlationId");
        logger.info("correlation Id: {}", correlationId);
        Tracer tracer = appConfig.getOpenTelemetry().getTracer("MyService4");
        Span parentSpan = Span.current();
        Span span = tracer.spanBuilder("myService4").setParent(Context.current().with(Span.wrap(parentSpan.getSpanContext()))).startSpan();
        span.setAttribute("correlationId", correlationId);
        SpanContext newSpanContext = span.getSpanContext();
        String result = "";
        try (Scope scope = span.makeCurrent()){
            result = "invoke my service 4";
            logger.info("Span Context in myService4: {}.", newSpanContext);
        } finally {
            span.end();
        }
        return result;
    }
}