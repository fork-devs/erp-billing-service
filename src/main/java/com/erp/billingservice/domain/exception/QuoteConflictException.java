package com.erp.billingservice.domain.exception;

public class QuoteConflictException extends RuntimeException {
    public QuoteConflictException(String message) {
        super(message);
    }
}
