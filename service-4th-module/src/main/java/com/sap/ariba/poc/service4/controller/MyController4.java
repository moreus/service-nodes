package com.sap.ariba.poc.service4.controller;

import com.sap.ariba.poc.service4.config.AppConfig;
import com.sap.ariba.poc.service4.service.MyService4;
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
@RequestMapping("/srv4")
public class MyController4 {
    private static final Logger logger = LoggerFactory.getLogger(MyController4.class);
    @Autowired
    private MyService4 service;

    @Autowired
    private AppConfig appConfig;

    @GetMapping("/api")
    public String api_4(@RequestHeader Map<String, String> headers) {
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
        logger.info("Span Context in api_4: {}.", remoteContext);
        Tracer tracer = appConfig.getOpenTelemetry().getTracer("MyService4");
        Span span = tracer.spanBuilder("api_4").setParent(Context.current().with(Span.wrap(remoteContext))).startSpan();
        span.setAttribute("correlationId", correlationId);
        String result = "";
        try(Scope scope = span.makeCurrent()) {
            span.addEvent("invoke myService4");
            result = service.myService4();
            logger.info("New Span Context in api_4: {}.", span.getSpanContext());
        }  finally {
            span.end();
        }
        return result;
    }

    @GetMapping("/rollback")
    public String rollback(@RequestHeader Map<String, String> headers) {
        logger.info("rollback service 4 {}", headers);
        String correlationId = headers.get("correlationid");
        MDC.put("correlationId", correlationId);

        String traceId = headers.get("traceid");
        String spanId = headers.get("spanid");
        SpanContext remoteContext = SpanContext.createFromRemoteParent(
                traceId,
                spanId,
                TraceFlags.getSampled(),
                TraceState.getDefault());
        logger.info("Span Context in service 4 rollback: {}.", remoteContext);

        Tracer tracer = appConfig.getOpenTelemetry().getTracer("Service 4 rollback");
        Span span = tracer.spanBuilder("rollback").setParent(Context.current().with(Span.wrap(remoteContext))).startSpan();
        span.setAttribute("correlationId", correlationId);
        String result = "";
        try(Scope scope = span.makeCurrent()) {
            span.addEvent("invoke myService 4 rollback");
            result = service.doRollback();
            logger.info("New Span Context in rollback: {}.", span.getSpanContext());
        } finally {
            span.end();
        }
        return result;
    }

}
