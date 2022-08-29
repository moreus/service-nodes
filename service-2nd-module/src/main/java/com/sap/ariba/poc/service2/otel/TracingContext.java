package com.sap.ariba.poc.service2.otel;

import io.opentelemetry.context.Context;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.util.Collections;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
@RequestScope
public class TracingContext implements Tracing {
    @Autowired
    private HttpServletRequest request;
    private Context context;
    private Boolean extracted = false;

    /**
     * Extract context from headers and set extracted status to true.
     * @param headers
     */
    public void extract(HttpHeaders headers) {
        context = Tracing.extractToContext(headers);
        extracted = true;
    }

    /**
     * Get context after extract
     * @return
     */
    public Context getContext(){
        return context;
    }

    /**
     * Inject key/val to assign headers from http request when did not extract from context, else inject context to headers.
     * @param headers
     * @return
     */
    public HttpHeaders inject(HttpHeaders headers) {
        if (!extracted) {
            // if we can not get the current http request by spring beans, use it
            // getCurrentHttpRequest().ifPresent(currentRequest -> request = currentRequest);
            if (request == null) {
                return headers;
            }
            // get headers from request. according to
            // https://stackoverflow.com/questions/25247218/servlet-filter-how-to-get-all-the-headers-from-servletrequest
            extract(Collections.list(request.getHeaderNames())
                    .stream()
                    .collect(Collectors.toMap(
                            Function.identity(),
                            h -> Collections.list(request.getHeaders(h)),
                            (oldValue, newValue) -> newValue,
                            HttpHeaders::new
                    )));
        }
        return Tracing.injectHeaders(context, headers);
    }

    /**
     * Get current http request.
     *
     * @return Optional<HttpServletRequest>
     */
    // get the current http request by a static way. according to
    // https://stackoverflow.com/questions/592123/is-there-a-static-way-to-get-the-current-httpservletrequest-in-spring
    public static Optional<HttpServletRequest> getCurrentHttpRequest() {
        return Optional.ofNullable(RequestContextHolder.getRequestAttributes())
                .filter(ServletRequestAttributes.class::isInstance)
                .map(ServletRequestAttributes.class::cast)
                .map(ServletRequestAttributes::getRequest);
    }
}

