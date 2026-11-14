package com.erp.billingservice.domain.valueobject;

import java.util.UUID;

/**
 * Strongly-typed Tenant identifier rejecting null and nil UUIDs.
 */
public record TenantId(UUID value) {

    private static final UUID NIL_UUID = new UUID(0L, 0L);

    public TenantId {
        if (value == null) {
            throw new IllegalArgumentException("TenantId cannot be null");
        }
        if (NIL_UUID.equals(value)) {
            throw new IllegalArgumentException("TenantId cannot be the nil UUID");
        }
    }

    public static TenantId fromString(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new IllegalArgumentException("TenantId cannot be null or blank");
        }
        try {
            return new TenantId(UUID.fromString(raw.trim()));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid TenantId UUID format: " + raw, e);
        }
    }
}
