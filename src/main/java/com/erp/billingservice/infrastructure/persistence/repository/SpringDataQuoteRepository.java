package com.erp.billingservice.infrastructure.persistence.repository;

import com.erp.billingservice.infrastructure.persistence.entity.QuoteEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SpringDataQuoteRepository extends JpaRepository<QuoteEntity, UUID> {
    Optional<QuoteEntity> findByWorkOrderId(UUID workOrderId);

    List<QuoteEntity> findByBranchId(UUID branchId);

    List<QuoteEntity> findByBranchIdAndStatus(UUID branchId, String status);
}
