package com.erp.billingservice.domain.model;

import com.erp.billingservice.domain.exception.DomainValidationException;

import java.util.Objects;
import java.util.regex.Pattern;

public final class Dni {
    private static final Pattern DNI_PATTERN = Pattern.compile("^\\d{8}$");
    private final String value;

    public Dni(String value) {
        if (value == null || !DNI_PATTERN.matcher(value.trim()).matches()) {
            throw new DomainValidationException("Invalid DNI: must have exactly 8 numeric digits");
        }
        this.value = value.trim();
    }

    public String getValue() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Dni dni)) return false;
        return Objects.equals(value, dni.value);
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
