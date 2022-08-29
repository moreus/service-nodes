package com.sap.ariba.poc.service1.otel;

import java.io.Serializable;

public class AppContext implements Serializable {
    private String traceId;
    private String spanId;
    private String correlationId;

    public AppContext(){}

    public AppContext(String traceId, String spanId, String correlationId) {
        this.traceId = traceId;
        this.spanId = spanId;
        this.correlationId = correlationId;
    }

    public String getTraceId() {
        return traceId;
    }

    public void setTraceId(String traceId) {
        this.traceId = traceId;
    }

    public String getSpanId() {
        return spanId;
    }

    public void setSpanId(String spanId) {
        this.spanId = spanId;
    }

    public String getCorrelationId() {
        return correlationId;
    }

    public void setCorrelationId(String correlationId) {
        this.correlationId = correlationId;
    }

    @Override
    public String toString() {
        return "AppContext{" +
                "traceId='" + traceId + '\'' +
                ", spanId='" + spanId + '\'' +
                ", correlationId='" + correlationId + '\'' +
                '}';
    }
}
