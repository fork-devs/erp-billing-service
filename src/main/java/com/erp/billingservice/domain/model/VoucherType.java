package com.erp.billingservice.domain.model;

public enum VoucherType {
    FACTURA("01", "F"),
    BOLETA("03", "B");

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
}
