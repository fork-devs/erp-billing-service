package com.erp.billingservice.application.service;

import com.erp.billingservice.domain.port.PaymentSessionStrategy;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Resolves and executes the appropriate {@link PaymentSessionStrategy} by provider name.
 */
@Service
public class PaymentSessionService {

    private final Map<String, PaymentSessionStrategy> strategiesByProvider;

    public PaymentSessionService(List<PaymentSessionStrategy> strategies) {
        this.strategiesByProvider = strategies.stream()
                .collect(Collectors.toUnmodifiableMap(
                        s -> s.provider().trim().toUpperCase(),
                        Function.identity()
                ));
    }

    public PaymentSessionStrategy.PaymentSessionResult createSession(String provider, UUID quoteId, UUID invoiceId) {
        if (provider == null || provider.isBlank()) {
            throw new IllegalArgumentException("Payment provider is required");
        }
        if (quoteId == null && invoiceId == null) {
            throw new IllegalArgumentException("Either quoteId or invoiceId is required");
        }
        String normalized = provider.trim().toUpperCase();
        PaymentSessionStrategy strategy = strategiesByProvider.get(normalized);
        if (strategy == null) {
            throw new IllegalArgumentException("Unsupported payment session provider: " + provider);
        }
        return strategy.createSession(UUID.randomUUID(), quoteId, invoiceId);
    }
}
