package com.erp.billingservice.domain.model;

import com.erp.billingservice.domain.exception.DomainValidationException;

public final class Correlative {
    private final int value;

    public Correlative(int value) {
        if (value <= 0 || value > 99_999_999) {
            throw new DomainValidationException("Correlative must be a positive integer between 1 and 99999999");
        }
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    public String toFormattedString() {
        return String.format("%08d", value);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Correlative that)) return false;
        return value == that.value;
    }

    @Override
    public int hashCode() {
        return Integer.hashCode(value);
    }

    @Override
    public String toString() {
        return toFormattedString();
    }
}
