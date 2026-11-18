package com.erp.billingservice.domain.model;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum VoucherType {
    INVOICE("01", "F"),
    RECEIPT("03", "B");

    private final String cpeCode;
    private final String seriesPrefix;

    VoucherType(String cpeCode, String seriesPrefix) {
        this.cpeCode = cpeCode;
        this.seriesPrefix = seriesPrefix;
    }

    public String getCpeCode() {
        return cpeCode;
    }

    public String getSeriesPrefix() {
        return seriesPrefix;
    }

    public static VoucherType fromCpeCode(String cpeCode) {
        for (VoucherType type : values()) {
            if (type.cpeCode.equals(cpeCode)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown CPE code: " + cpeCode);
    }

    @JsonCreator
    public static VoucherType fromString(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Voucher type cannot be null or blank");
        }
        return switch (value.trim().toUpperCase()) {
            case "INVOICE", "FACTURA" -> INVOICE;
            case "RECEIPT", "BOLETA" -> RECEIPT;
            default -> throw new IllegalArgumentException("Unknown voucher type: " + value);
        };
    }
}
