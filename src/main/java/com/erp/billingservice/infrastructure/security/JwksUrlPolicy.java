package com.erp.billingservice.infrastructure.security;

import java.net.URI;
import java.util.List;

public final class JwksUrlPolicy {

    private static final List<String> LOCAL_DEV_PROFILES = List.of("dev", "local", "test");

    private JwksUrlPolicy() {
    }

    public static void verify(String jwksUrl, List<String> activeProfiles) {
        if (jwksUrl == null || jwksUrl.isBlank()) {
            throw new IllegalStateException(
                    "No JWKS URL configured. Define JWKS_URL pointing to the auth service's /.well-known/jwks.json");
        }

        String scheme;
        try {
            scheme = URI.create(jwksUrl.trim()).getScheme();
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException("JWKS URL is malformed: " + jwksUrl, e);
        }

        if ("https".equalsIgnoreCase(scheme)) {
            return;
        }

        boolean localDev = activeProfiles != null
                && activeProfiles.stream().anyMatch(LOCAL_DEV_PROFILES::contains);

        if (localDev) {
            return;
        }

        throw new IllegalStateException(
                "JWKS URL must use an encrypted scheme (https), but uses '"
                + scheme + "': " + jwksUrl + ". Only local development profiles permit plaintext HTTP.");
    }
}
