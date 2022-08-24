package com.sap.ariba.poc.service1.otel;

import io.opentelemetry.context.propagation.TextMapGetter;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;

public class TextMapGetterImpl implements TextMapGetter<HttpServletRequest> {

    private static final int MIN_LENGTH_FOR_HEADER = 1;

    public <E> Iterable<E> iterable(final Enumeration<E> enumeration) {
        if (enumeration == null) {
            throw new NullPointerException();
        }
        return () -> new Iterator<E>() {
            public boolean hasNext() {
                return enumeration.hasMoreElements();
            }
            public E next() {
                return enumeration.nextElement();
            }
            public void remove() {
                throw new UnsupportedOperationException();
            }
        };
    }

    @Override
    public Iterable<String> keys(HttpServletRequest carrier) {
        return iterable(carrier.getHeaderNames());
    }

    @Override
    public String get(HttpServletRequest carrier, String key) {
        Enumeration<String> headers = carrier.getHeaders(key);
        if (headers == null || !headers.hasMoreElements()) {
            return null;
        }
        List<String> values = new ArrayList<>();
        while (headers.hasMoreElements()) {
            String nextElement = headers.nextElement();
            if (!nextElement.trim().chars().allMatch(Character::isWhitespace)) {
                values.add(nextElement);
            }
        }
        if (values.isEmpty()) {
            return null;
        }
        if (values.size() == MIN_LENGTH_FOR_HEADER) {
            return values.get(0);
        }
        StringBuilder builder = new StringBuilder(values.get(0));
        for (int i = 1; i < values.size(); i++) {
            builder.append(',').append(values.get(i));
        }
        return builder.toString();
    }
}