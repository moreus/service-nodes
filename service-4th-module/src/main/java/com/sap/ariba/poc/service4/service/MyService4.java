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
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
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

    public String doRollback(){
        String correlationId = MDC.get("correlationId");
        logger.info("correlationId in myService 4 rollback: {}",correlationId);
        Tracer tracer = appConfig.getOpenTelemetry().getTracer("MyService 4 Rollback");
        Span parentSpan = Span.current();
        Span span = tracer.spanBuilder("myService 4 rollback").setParent(Context.current().with(Span.wrap(parentSpan.getSpanContext()))).startSpan();
        SpanContext newSpanContext = span.getSpanContext();
        span.setAttribute("correlationId", correlationId);
        try (Scope scope = span.makeCurrent()){
            String traceId = newSpanContext.getTraceId();
            String spanId = newSpanContext.getSpanId();
            HttpHeaders headers = new HttpHeaders(){{
                set("traceId", traceId);
                set("spanId", spanId);
                set("traceFlags",newSpanContext.getTraceFlags().asHex());
                newSpanContext.getTraceState().asMap().entrySet().stream().forEach(e -> set(e.getKey(), e.getValue()));
                set("correlationId", correlationId);
            }};
            HttpEntity<String> httpEntity = new HttpEntity<>(headers);
            logger.info("Headers: {}" ,headers);
            span.addEvent("call service 4 rollback");
            logger.info("{}, {}, Now handle rollback business logic.", traceId, spanId);
        } finally {
            span.end();
        }
        return "invoke my service 4 rollback";
    }
}
