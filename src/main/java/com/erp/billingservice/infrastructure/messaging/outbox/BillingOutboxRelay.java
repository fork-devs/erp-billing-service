package com.erp.billingservice.infrastructure.messaging.outbox;

import com.erp.billingservice.infrastructure.persistence.entity.BillingOutboxEntity;
import com.erp.billingservice.infrastructure.persistence.repository.SpringDataOutboxRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

@Component
@EnableScheduling
@ConditionalOnProperty(name = "erp.outbox.relay-enabled", havingValue = "true", matchIfMissing = true)
public class BillingOutboxRelay {

    private static final Logger log = LoggerFactory.getLogger(BillingOutboxRelay.class);

    private final SpringDataOutboxRepository repository;
    private final int batchSize;
    private final int maxAttempts;

    public BillingOutboxRelay(
            SpringDataOutboxRepository repository,
            @Value("${erp.outbox.batch-size:20}") int batchSize,
            @Value("${erp.outbox.max-attempts:7}") int maxAttempts
    ) {
        this.repository = repository;
        this.batchSize = batchSize;
        this.maxAttempts = maxAttempts;
    }

    @Scheduled(fixedDelayString = "${erp.outbox.poll-interval-ms:5000}")
    public void sweepAndRelay() {
        List<BillingOutboxEntity> pending = repository.findPendingEvents(Instant.now(), PageRequest.of(0, batchSize));
        if (pending.isEmpty()) {
            return;
        }

        log.debug("Found {} pending billing outbox events to relay", pending.size());
        for (BillingOutboxEntity event : pending) {
            relaySingleEvent(event);
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void relaySingleEvent(BillingOutboxEntity event) {
        try {
            log.info("Relaying billing event: id={}, type={}, aggregateId={}",
                    event.getEventId(), event.getEventType(), event.getAggregateId());

            // Simulation of publishing to Kafka bus
            publishToBus(event.getEventType(), event.getPayload());

            event.setStatus("PUBLISHED");
            event.setPublishedAt(Instant.now());
            repository.save(event);
        } catch (Exception e) {
            int currentAttempts = event.getAttempts() + 1;
            event.setAttempts(currentAttempts);
            event.setLastError(e.getMessage());

            if (currentAttempts >= maxAttempts) {
                log.error("Billing event {} reached max attempts ({}). Moving to DEAD_LETTER",
                        event.getEventId(), maxAttempts, e);
                event.setStatus("DEAD_LETTER");
            } else {
                long backoffSeconds = (long) Math.min(Math.pow(2, currentAttempts), 3600);
                event.setNextAttemptAt(Instant.now().plus(Duration.ofSeconds(backoffSeconds)));
                log.warn("Billing event {} failed attempt {}. Next retry scheduled at {}",
                        event.getEventId(), currentAttempts, event.getNextAttemptAt());
            }
            repository.save(event);
        }
    }

    private void publishToBus(String eventType, String payloadJson) {
        // Here KafkaTemplate would send the event if active, otherwise logging serves as the outbox channel
        log.debug("Event successfully dispatched to event bus: [{}]: {}", eventType, payloadJson);
    }
}
