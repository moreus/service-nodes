package com.sap.ariba.poc.service1.otel;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.propagation.ContextPropagators;
import io.opentelemetry.context.propagation.TextMapGetter;
import io.opentelemetry.context.propagation.TextMapPropagator;
import io.opentelemetry.context.propagation.TextMapSetter;
import io.opentelemetry.extension.trace.propagation.B3Propagator;
import org.springframework.http.HttpHeaders;
import org.springframework.lang.Nullable;

import java.net.HttpURLConnection;
import java.net.URLConnection;
import java.util.List;

public interface Tracing {
    OpenTelemetry openTelemetry = OpenTelemetry.propagating(
            ContextPropagators.create(TextMapPropagator.composite(B3Propagator.injectingMultiHeaders())));

    TextMapGetter<HttpHeaders> getter =
            new TextMapGetter<HttpHeaders>() {
                @Override
                public Iterable<String> keys(HttpHeaders carrier) {
                    return carrier.keySet();
                }

                @Nullable
                @Override
                public String get(HttpHeaders carrier, String key) {
                    List<String> values = carrier != null ? carrier.get(key) : null;
                    if (values == null || values.isEmpty()) {
                        return null;
                    }
                    return values.get(0);
                }
            };

    TextMapSetter<HttpURLConnection> httpURLConnectionSetter = URLConnection::setRequestProperty;
    TextMapSetter<HttpHeaders> headersSetter = HttpHeaders::set;

    /**
     * Extract key/val from headers to context and return context
     * @param headers
     * @return Context
     */
    static Context extractToContext(HttpHeaders headers) {
        return openTelemetry.getPropagators().getTextMapPropagator().extract(Context.current(), headers, getter);
    }

    /**
     * Extract key/val from headers to context
     * @param headers
     */
    static void extract(HttpHeaders headers) {
        Context context = extractToContext(headers);
        context.makeCurrent();
    }

    /**
     * Inject HttpURLConnection with assign context and return HttpURLConnection
     * @param context
     * @param httpURLConnection
     * @return HttpURLConnection
     */
    static HttpURLConnection injectHttpURLConnection(Context context, HttpURLConnection httpURLConnection) {
        if (context == null) {
            return httpURLConnection;
        }
        openTelemetry.getPropagators().getTextMapPropagator().inject(context, httpURLConnection, httpURLConnectionSetter);
        return httpURLConnection;
    }

    /**
     * Inject HttpURLConnection with current context and return HttpURLConnection
     * @param httpURLConnection
     * @return HttpURLConnection
     */
    static HttpURLConnection injectHttpURLConnection(HttpURLConnection httpURLConnection) {
        Context context = Context.current();
        return injectHttpURLConnection(context, httpURLConnection);
    }

    /**
     * Inject headers with assign context and return HttpHeaders
     * @param context
     * @param headers
     * @return HttpHeaders
     */
    static HttpHeaders injectHeaders(Context context, HttpHeaders headers) {
        if (context == null) {
            return headers;
        }
        openTelemetry.getPropagators().getTextMapPropagator().inject(context, headers, headersSetter);
        return headers;
    }

    /**
     * Inject headers with current context and return HttpHeaders
     * @param headers
     * @return
     */
    static HttpHeaders injectHeaders(HttpHeaders headers) {
        Context context = Context.current();
        return injectHeaders(context, headers);
    }
}

