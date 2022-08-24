package com.sap.ariba.poc.service5.service;


import com.sap.ariba.poc.service5.config.AppConfig;
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Service
public class MyService5 {
    private static final Logger logger = LoggerFactory.getLogger(MyService5.class);

    @Autowired
    private AppConfig appConfig;

    public String myService5() {
        String correlationId = MDC.get("correlationId");
        logger.info("correlation Id: {}", correlationId);
        Tracer tracer = appConfig.getOpenTelemetry().getTracer("MyService5");
        Span parentSpan = Span.current();
        Span span = tracer.spanBuilder("myService5").setParent(Context.current().with(Span.wrap(parentSpan.getSpanContext()))).startSpan();
        span.setAttribute("correlationId", correlationId);
        SpanContext newSpanContext = span.getSpanContext();
        String result = "";
        String[] appModules = {"authorization", "authentication", "route", "api", "gateway"};
        List<String> modules = Arrays.asList(appModules);
        try (Scope scope = span.makeCurrent()) {
            result = "invoke my service 5";
            logger.info("Span Context in myService4: {}.", newSpanContext);
            logger.info("module {}", modules.get(5));
        } catch (IndexOutOfBoundsException e){
            span.recordException(e);
        } finally {
            span.end();
        }
        return result;
    }

    public String doRollback(){
        String correlationId = MDC.get("correlationId");
        logger.info("correlationId in myService 5 rollback: {}",correlationId);
        Tracer tracer = appConfig.getOpenTelemetry().getTracer("MyService 5 Rollback");
        Span parentSpan = Span.current();
        Span span = tracer.spanBuilder("myService 5 rollback").setParent(Context.current().with(Span.wrap(parentSpan.getSpanContext()))).startSpan();
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
            span.addEvent("call service 5 rollback");
            logger.info("{}, {}, Now handle rollback business logic.", traceId, spanId);
        } finally {
            span.end();
        }
        return "invoke my service 5 rollback";
    }
}
