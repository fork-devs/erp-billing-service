package com.erp.billingservice.infrastructure.persistence.repository;

import com.erp.billingservice.infrastructure.persistence.entity.InvoiceEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface SpringDataInvoiceRepository extends JpaRepository<InvoiceEntity, UUID> {

    Optional<InvoiceEntity> findByWorkOrderId(UUID workOrderId);

    Optional<InvoiceEntity> findByQuoteId(UUID quoteId);

    Optional<InvoiceEntity> findByTenantIdAndSeriesAndCorrelativo(UUID tenantId, String series, Integer correlativo);

    @Query("SELECT COALESCE(MAX(i.correlativo), 0) FROM InvoiceEntity i WHERE i.tenantId = :tenantId AND i.series = :series")
    int findMaxCorrelativoByTenantIdAndSeries(@Param("tenantId") UUID tenantId, @Param("series") String series);

    Page<InvoiceEntity> findAllByTenantIdOrderByIssuedAtDesc(UUID tenantId, Pageable pageable);
}
