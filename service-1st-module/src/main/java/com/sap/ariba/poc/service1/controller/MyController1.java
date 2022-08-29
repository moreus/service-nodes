package com.sap.ariba.poc.service1.controller;

import com.sap.ariba.poc.service1.config.AppConfig;
import com.sap.ariba.poc.service1.exception.BadRequestException;
import com.sap.ariba.poc.service1.exception.OrderIdNotExistException;
import com.sap.ariba.poc.service1.otel.AppContext;
import com.sap.ariba.poc.service1.service.MyService1;
import io.opentelemetry.api.trace.*;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
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
    public ResponseEntity<String> api_1(@RequestParam("orderId") String orderId) {
        Tracer tracer = appConfig.getOpenTelemetry().getTracer("MyService1");

        if (redisTemplate.hasKey(orderId)) {
            handleBadRequest(tracer);
            return new ResponseEntity("Bad Request, Order Document is created.", HttpStatus.BAD_REQUEST);
        }

        String correlationId = UUID.randomUUID().toString();
        MDC.put("correlationId", correlationId);
        logger.info("correlationId: {}", correlationId);

        Span span = tracer.spanBuilder("api_1").startSpan();
        span.setAttribute("correlationId", correlationId);
        String result = "";
        try (Scope scope = span.makeCurrent()){
            span.addEvent("invoke service1");
            result  = service.handleBusinessLogic();
        } finally {
            SpanContext spanContext = span.getSpanContext();
            AppContext context = new AppContext(spanContext.getTraceId(), spanContext.getSpanId(), correlationId);
            logger.info("App Context: {}，{}, {}", spanContext.getTraceId(), spanContext.getSpanId(), correlationId);
            redisTemplate.opsForValue().set(orderId, context);
            span.end();
        }
        return new ResponseEntity(result, HttpStatus.OK);
    }

    private static void handleBadRequest(Tracer tracer) {
        Span exceptionSpan = tracer.spanBuilder("order_document_created_exception").startSpan();
        BadRequestException e = new BadRequestException("order_document_created_exception");
        exceptionSpan.recordException(e);
        exceptionSpan.end();
    }

    @GetMapping("/rollback")
    public ResponseEntity<String> rollback(@RequestParam("orderId") String orderId) {
        Tracer tracer = appConfig.getOpenTelemetry().getTracer("MyService1_Rollback");
        if (!redisTemplate.hasKey(orderId)) {
            Span exceptionSpan = tracer.spanBuilder("rollback_exception").startSpan();
            OrderIdNotExistException e = new OrderIdNotExistException("Order Id is not exist");
            exceptionSpan.recordException(e);
            exceptionSpan.end();
            return new ResponseEntity("Order Id is not exist", HttpStatus.NOT_FOUND);
        }

        AppContext context = (AppContext) redisTemplate.opsForValue().get(orderId);
        String correlationId = UUID.randomUUID().toString();
        MDC.put("correlationId", correlationId);
        logger.info("App Context: {}", context);

        SpanContext businessContext = SpanContext.createFromRemoteParent(
                context.getTraceId(),
                context.getSpanId(),
                TraceFlags.getSampled(),
                TraceState.getDefault());

        Span span = tracer.spanBuilder("rollback").setParent(Context.current().with(Span.wrap(businessContext))).startSpan();
        span.setAttribute("correlationId", correlationId);
        String result = "";
        try (Scope scope = span.makeCurrent()) {
            span.addEvent("invoke service1 rollback");
            result = service.doRollback();
            //redisTemplate.delete(orderId);
        } catch (Exception e) {
            span.recordException(e);
        } finally {
            span.end();
        }

        return new ResponseEntity("invoke service1 rollback", HttpStatus.OK);
    }

}
