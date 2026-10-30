package com.erp.billingservice.infrastructure.security;

import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.security.interfaces.RSAPublicKey;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class JwksKeyManager {

    private static final Logger log = LoggerFactory.getLogger(JwksKeyManager.class);

    private static final long CACHE_TTL_SECONDS = 300;
    private static final long RETRY_INTERVAL_SECONDS = 10;
    private static final int CONNECT_TIMEOUT_MILLIS = 2000;
    private static final int READ_TIMEOUT_MILLIS = 3000;

    private final String jwksUrl;
    private final ConcurrentHashMap<String, RSAPublicKey> publicKeyMap = new ConcurrentHashMap<>();

    private volatile Instant lastSuccessTime = Instant.EPOCH;
    private volatile Instant lastAttemptTime = Instant.EPOCH;
    private volatile boolean lastAttemptFailed = false;

    public JwksKeyManager(
            @Value("${erp.auth.jwks-url:https://auth.erp-automotriz.com/.well-known/jwks.json}") String jwksUrl,
            Environment env
    ) {
        this.jwksUrl = jwksUrl;
        List<String> activeProfiles = Arrays.asList(env.getActiveProfiles());
        JwksUrlPolicy.verify(jwksUrl, activeProfiles);
    }

    public RSAPublicKey getPublicKey(String keyId) {
        if (keyId == null || keyId.isBlank()) {
            return null;
        }

        Instant now = Instant.now();
        boolean cacheExpired = now.isAfter(lastSuccessTime.plusSeconds(CACHE_TTL_SECONDS));

        if (!cacheExpired && publicKeyMap.containsKey(keyId)) {
            return publicKeyMap.get(keyId);
        }

        refreshKeysIfAppropriate(now);

        return publicKeyMap.get(keyId);
    }

    private synchronized void refreshKeysIfAppropriate(Instant now) {
        if (lastAttemptFailed && now.isBefore(lastAttemptTime.plusSeconds(RETRY_INTERVAL_SECONDS))) {
            return;
        }

        lastAttemptTime = now;
        try {
            URL url = URI.create(jwksUrl).toURL();
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(CONNECT_TIMEOUT_MILLIS);
            conn.setReadTimeout(READ_TIMEOUT_MILLIS);
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Accept", "application/json");

            if (conn.getResponseCode() != 200) {
                lastAttemptFailed = true;
                log.warn("Failed to retrieve JWKS: HTTP {}", conn.getResponseCode());
                return;
            }

            try (InputStream in = conn.getInputStream()) {
                JWKSet jwkSet = JWKSet.load(in);
                for (JWK key : jwkSet.getKeys()) {
                    if (key instanceof RSAKey rsaKey && key.getKeyID() != null) {
                        publicKeyMap.put(key.getKeyID(), rsaKey.toRSAPublicKey());
                    }
                }
            }

            lastSuccessTime = now;
            lastAttemptFailed = false;
        } catch (Exception e) {
            lastAttemptFailed = true;
            log.warn("Error refreshing JWKS from {}: {}", jwksUrl, e.getMessage());
        }
    }
}
