package com.erp.billingservice.domain.model;

import com.erp.billingservice.domain.exception.DomainValidationException;

import java.util.Objects;
import java.util.regex.Pattern;

public final class Series {
    private static final Pattern SERIES_PATTERN = Pattern.compile("^[FB][0-9A-Z]{3}$");
    private final String value;

    public Series(String value) {
        if (value == null || !SERIES_PATTERN.matcher(value.trim()).matches()) {
            throw new DomainValidationException("Invalid series: must have 4 alphanumeric characters and start with F or B (e.g. F001, B001)");
        }
        this.value = value.trim().toUpperCase();
    }

    public String getValue() {
        return value;
    }

    public void validateFor(VoucherType voucherType) {
        if (voucherType == VoucherType.FACTURA && !value.startsWith("F")) {
            throw new DomainValidationException("Series for Factura must start with 'F' (e.g. F001)");
        }
        if (voucherType == VoucherType.BOLETA && !value.startsWith("B")) {
            throw new DomainValidationException("Series for Boleta must start with 'B' (e.g. B001)");
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Series series)) return false;
        return Objects.equals(value, series.value);
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
