package com.sap.ariba.poc.service1.controller;

import com.sap.ariba.poc.service1.config.AppConfig;
import com.sap.ariba.poc.service1.service.MyService1;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/srv1")
public class MyController1 {
    private static final Logger logger = LoggerFactory.getLogger(MyController1.class);
    @Autowired
    private AppConfig appConfig;

    @Autowired
    private MyService1 service;


    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @GetMapping("/api")
    public String api_1() {
        String correlationId = UUID.randomUUID().toString();
        MDC.put("correlationId", correlationId);
        logger.info("correlationId: {}", correlationId);
        Tracer tracer = appConfig.getOpenTelemetry().getTracer("MyService1");
        Span span = tracer.spanBuilder("api_1").startSpan();
        span.setAttribute("correlationId", correlationId);
        String result = "";
        try (Scope scope = span.makeCurrent()){
            span.addEvent("invoke service1");
            result  = service.myService1();
        } finally {
            span.end();
        }
        return result;
    }

}
