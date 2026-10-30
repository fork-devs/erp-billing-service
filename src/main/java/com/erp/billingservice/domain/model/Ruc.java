package com.erp.billingservice.domain.model;

import com.erp.billingservice.domain.exception.DomainValidationException;

import java.util.Objects;
import java.util.regex.Pattern;

public final class Ruc {
    private static final Pattern RUC_PATTERN = Pattern.compile("^(10|20|15|16|17)\\d{9}$");
    private final String value;

    public Ruc(String value) {
        if (value == null || !RUC_PATTERN.matcher(value.trim()).matches()) {
            throw new DomainValidationException("Invalid RUC: must have 11 digits and start with 10, 20, 15, 16, or 17");
        }
        this.value = value.trim();
    }

    public String getValue() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Ruc ruc)) return false;
        return Objects.equals(value, ruc.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
