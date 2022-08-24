package com.sap.ariba.poc.service1.otel;

import java.io.Serializable;

public class RollbackContext implements Serializable {
    private String traceId;
    private String spanId;

    public RollbackContext(){}
    public RollbackContext(String traceId, String spanId) {
        this.traceId = traceId;
        this.spanId = spanId;
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
}
