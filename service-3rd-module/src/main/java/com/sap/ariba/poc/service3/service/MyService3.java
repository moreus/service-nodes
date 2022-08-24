package com.sap.ariba.poc.service3.service;


import com.sap.ariba.poc.common.Constants;
import com.sap.ariba.poc.service3.config.AppConfig;
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
public class MyService3 {
    private static final Logger logger = LoggerFactory.getLogger(MyService3.class);

    @Autowired
    private AppConfig appConfig;

    @Autowired
    private RestTemplate restTemplate;

    public String myService3() {
        String correlationId = MDC.get("correlationId");
        Tracer tracer = appConfig.getOpenTelemetry().getTracer("MyService3");
        Span parentSpan = Span.current();
        Span span = tracer.spanBuilder("myService3").setParent(Context.current().with(Span.wrap(parentSpan.getSpanContext()))).startSpan();
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
            span.addEvent("call srv4_api");
            ResponseEntity<String> response = restTemplate.exchange(Constants.DOCKER_INTERNAL_HOST + "/srv4/api", HttpMethod.GET, httpEntity, String.class);
            logger.info("After invoke service 4, the result is {}", response.getBody());
        } finally {
            span.end();
        }
        return "invoke my service 3";
    }
}
