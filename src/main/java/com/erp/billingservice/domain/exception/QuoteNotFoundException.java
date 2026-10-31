package com.erp.billingservice.domain.exception;

import java.util.UUID;

public class QuoteNotFoundException extends RuntimeException {
    public QuoteNotFoundException(UUID id) {
        super("Quote not found with ID: " + id);
    }

    public QuoteNotFoundException(String message) {
        super(message);
    }
}
