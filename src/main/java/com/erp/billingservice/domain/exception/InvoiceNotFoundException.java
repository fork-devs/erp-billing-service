package com.erp.billingservice.domain.exception;

import java.util.UUID;

public class InvoiceNotFoundException extends RuntimeException {
    public InvoiceNotFoundException(UUID id) {
        super("Invoice not found with ID: " + id);
    }

    public InvoiceNotFoundException(String message) {
        super(message);
    }
}
