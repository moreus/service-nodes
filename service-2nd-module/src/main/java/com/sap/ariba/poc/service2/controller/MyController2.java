package com.sap.ariba.poc.service2.controller;

import com.sap.ariba.poc.service2.config.AppConfig;
import com.sap.ariba.poc.service2.service.MyService2;
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
@RequestMapping("/srv2")
public class MyController2 {
    private static final Logger logger = LoggerFactory.getLogger(MyController2.class);
    @Autowired
    private MyService2 service;

    @Autowired
    private AppConfig appConfig;

    @GetMapping("/api")
    public String api_2(@RequestHeader Map<String, String> headers) {
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
        logger.info("Span Context in api_2: {}.", remoteContext);

        Tracer tracer = appConfig.getOpenTelemetry().getTracer("MyService2");
        Span span = tracer.spanBuilder("api_2").setParent(Context.current().with(Span.wrap(remoteContext))).startSpan();
        span.setAttribute("correlationId", correlationId);
        String result = "";
        try(Scope scope = span.makeCurrent()) {
            span.addEvent("invoke myService2");
            result = service.myService2();
            logger.info("New Span Context in api_2: {}.", span.getSpanContext());
        } finally {
             span.end();
        }
        return result;
    }

}
