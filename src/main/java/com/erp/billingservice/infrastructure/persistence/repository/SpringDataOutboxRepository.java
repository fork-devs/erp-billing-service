package com.erp.billingservice.infrastructure.persistence.repository;

import com.erp.billingservice.infrastructure.persistence.entity.BillingOutboxEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface SpringDataOutboxRepository extends JpaRepository<BillingOutboxEntity, UUID> {

    @Query("SELECT o FROM BillingOutboxEntity o WHERE o.status = 'PENDING' AND o.nextAttemptAt <= :now ORDER BY o.createdAt ASC")
    List<BillingOutboxEntity> findPendingEvents(@Param("now") Instant now, Pageable pageable);
}
