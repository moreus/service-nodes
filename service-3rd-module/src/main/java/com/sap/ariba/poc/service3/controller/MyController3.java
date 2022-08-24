package com.sap.ariba.poc.service3.controller;

import com.sap.ariba.poc.service3.config.AppConfig;
import com.sap.ariba.poc.service3.service.MyService3;
import io.opentelemetry.api.trace.*;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/srv3")
public class MyController3 {
    private static final Logger logger = LoggerFactory.getLogger(MyController3.class);
    @Autowired
    private MyService3 service;
    @Autowired
    private AppConfig appConfig;

    @GetMapping("/api")
    public String api_3(@RequestHeader Map<String, String> headers) {
        logger.info("Request Headers from service1 {}", headers);
        String correlationId = headers.get("correlationid");
        MDC.put("correlationId", correlationId);

        String traceId = headers.get("traceid");
        String spanId = headers.get("spanid");
        SpanContext remoteContext = SpanContext.createFromRemoteParent(
                traceId,
                spanId,
                TraceFlags.getSampled(),
                TraceState.getDefault());
        logger.info("Span Context in api_3: {}.", remoteContext);
        Tracer tracer = appConfig.getOpenTelemetry().getTracer("MyService3");
        Span span = tracer.spanBuilder("api_3").setParent(Context.current().with(Span.wrap(remoteContext))).startSpan();
        span.setAttribute("correlationId", correlationId);
        String result = "";
        try(Scope scope = span.makeCurrent()) {
            span.addEvent("invoke myService3");
            result = service.myService3();
            logger.info("New Span Context in api_3: {}.", span.getSpanContext());
        }  finally {
            span.end();
        }
        return result;
    }

    @GetMapping("/rollback")
    public String rollback(@RequestHeader Map<String, String> headers) {
        logger.info("rollback service 3 {}", headers);
        String correlationId = headers.get("correlationid");
        MDC.put("correlationId", correlationId);

        String traceId = headers.get("traceid");
        String spanId = headers.get("spanid");
        SpanContext remoteContext = SpanContext.createFromRemoteParent(
                traceId,
                spanId,
                TraceFlags.getSampled(),
                TraceState.getDefault());
        logger.info("Span Context in service 3 rollback: {}.", remoteContext);

        Tracer tracer = appConfig.getOpenTelemetry().getTracer("Service 3 rollback");
        Span span = tracer.spanBuilder("rollback").setParent(Context.current().with(Span.wrap(remoteContext))).startSpan();
        span.setAttribute("correlationId", correlationId);
        String result = "";
        try(Scope scope = span.makeCurrent()) {
            span.addEvent("invoke myService 3 rollback");
            result = service.doRollback();
            logger.info("New Span Context in rollback: {}.", span.getSpanContext());
        } finally {
            span.end();
        }
        return result;
    }
}
