package com.sap.ariba.poc.service1.otel;

import java.util.HashMap;
import java.util.Map;

public class ExtractModel {

    private Map<String, String> headers;

    public void addHeader(String key, String value) {
        if (this.headers == null){
            headers = new HashMap<>();
        }
        headers.put(key, value);
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    public void setHeaders(Map<String, String> headers) {
        this.headers = headers;
    }
}