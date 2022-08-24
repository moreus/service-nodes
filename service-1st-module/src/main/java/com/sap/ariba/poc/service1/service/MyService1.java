package com.sap.ariba.poc.service1.service;


import com.sap.ariba.poc.common.Constants;
import com.sap.ariba.poc.service1.config.AppConfig;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class MyService1 {
    private static final Logger logger = LoggerFactory.getLogger(MyService1.class);

    @Autowired
    private AppConfig appConfig;

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    public String handleBusinessLogic() {
        String correlationId = MDC.get("correlationId");
        MDC.remove("correlationId");
        Tracer tracer = appConfig.getOpenTelemetry().getTracer("MyService1");
        Span parentSpan = Span.current();
        Span span = tracer.spanBuilder("myService1").setParent(Context.current().with(Span.wrap(parentSpan.getSpanContext()))).startSpan();
        span.setAttribute("correlationId", correlationId);
        SpanContext newSpanContext = span.getSpanContext();
        try(Scope scope = span.makeCurrent()) {
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
            span.addEvent("call srv2_api");

            ResponseEntity<String> responseFromSrv2 = restTemplate.exchange(Constants.DOCKER_INTERNAL_HOST + "/srv2/api", HttpMethod.GET, httpEntity, String.class);
            logger.info("After invoke service 2, the result is {}", responseFromSrv2.getBody());
            span.addEvent("call srv5_api");

            ResponseEntity<String> responseFromSrv5 = restTemplate.exchange(Constants.DOCKER_INTERNAL_HOST + "/srv5/api", HttpMethod.GET, httpEntity, String.class);
            logger.info("After invoke service 5, the result is {}", responseFromSrv5.getBody());


            logger.info("{}, {}, Now handle business logic.", traceId, spanId);

        } finally {
            span.end();
        }
        return "invoke my service 1";
    }

    public String doRollback(){
        String correlationId = MDC.get("correlationId");
        MDC.remove("correlationId");
        Tracer tracer = appConfig.getOpenTelemetry().getTracer("MyService1");
        Span parentSpan = Span.current();
        Span span = tracer.spanBuilder("myService 1 rollback").setParent(Context.current().with(Span.wrap(parentSpan.getSpanContext()))).startSpan();
        span.setAttribute("correlationId", correlationId);
        SpanContext newSpanContext = span.getSpanContext();
        try(Scope scope = span.makeCurrent()) {
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
            span.addEvent("call srv2_api rollback");

            ResponseEntity<String> responseFromSrv2 = restTemplate.exchange(Constants.DOCKER_INTERNAL_HOST + "/srv2/rollback", HttpMethod.GET, httpEntity, String.class);
            logger.info("After invoke service 2 rollback, the result is {}", responseFromSrv2.getBody());
            span.addEvent("call srv2_api");

            ResponseEntity<String> responseFromSrv5 = restTemplate.exchange(Constants.DOCKER_INTERNAL_HOST + "/srv5/rollback", HttpMethod.GET, httpEntity, String.class);
            logger.info("After invoke service 5 rollback, the result is {}", responseFromSrv5.getBody());


            logger.info("{}, {}, Now handle rollback business logic.", traceId, spanId);

        } finally {
            span.end();
        }
        return "call service 1 rollback";
    }
}
