package com.sap.ariba.poc.service1.exception;

public class OrderIdNotExistException extends RuntimeException {

    public OrderIdNotExistException(String message) {
        super(message);
    }

    public OrderIdNotExistException(String message, Throwable throwable) {
        super(message, throwable);
    }
}
