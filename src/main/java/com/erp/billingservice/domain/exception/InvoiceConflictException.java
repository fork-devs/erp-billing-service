package com.erp.billingservice.domain.exception;

public class InvoiceConflictException extends RuntimeException {
    public InvoiceConflictException(String message) {
        super(message);
    }
}
