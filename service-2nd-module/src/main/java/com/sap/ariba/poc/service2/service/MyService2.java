package com.sap.ariba.poc.service2.service;


import com.sap.ariba.poc.common.Constants;
import com.sap.ariba.poc.service2.config.AppConfig;
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
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class MyService2 {
    private static final Logger logger = LoggerFactory.getLogger(MyService2.class);

    @Autowired
    private AppConfig appConfig;

    @Autowired
    private RestTemplate restTemplate;

    public String myService2() {
        String correlationId = MDC.get("correlationId");
        logger.info("correlationId in myService2: {}",correlationId);
        Tracer tracer = appConfig.getOpenTelemetry().getTracer("MyService2");
        Span parentSpan = Span.current();
        Span span = tracer.spanBuilder("myService2").setParent(Context.current().with(Span.wrap(parentSpan.getSpanContext()))).startSpan();
        SpanContext newSpanContext = span.getSpanContext();
        span.setAttribute("correlationId", correlationId);
        try (Scope scope = span.makeCurrent()){
            HttpHeaders headers = new HttpHeaders(){{
                set("traceId", newSpanContext.getTraceId());
                set("spanId", newSpanContext.getSpanId());
                set("traceFlags",newSpanContext.getTraceFlags().asHex());
                newSpanContext.getTraceState().asMap().entrySet().stream().forEach(e -> set(e.getKey(), e.getValue()));
                set("correlationId", correlationId);
            }};
            HttpEntity<String> httpEntity = new HttpEntity<>(headers);
            logger.info("Headers: {}" ,headers);
            span.addEvent("call srv3_api");
            ResponseEntity<String> response = restTemplate.exchange(Constants.DOCKER_INTERNAL_HOST + "/srv3/api", HttpMethod.GET, httpEntity, String.class);
            logger.info("After invoke service 3, the result is {}", response.getBody());
        } finally {
            span.end();
        }
        return "invoke my service 2";
    }
}
