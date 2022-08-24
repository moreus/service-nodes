package com.sap.ariba.poc.service1.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "trace.exporter")
public class TraceConfig {
    private String host;
    private String port;
    private String uiPort;

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public String getPort() {
        return port;
    }

    public void setPort(String port) {
        this.port = port;
    }

    public String getUiPort() {
        return uiPort;
    }

    public void setUiPort(String uiPort) {
        this.uiPort = uiPort;
    }
}
