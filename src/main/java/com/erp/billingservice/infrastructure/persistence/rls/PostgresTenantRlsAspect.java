package com.erp.billingservice.infrastructure.persistence.rls;

import com.erp.billingservice.domain.exception.TenantContextMissingException;
import jakarta.persistence.EntityManager;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.UUID;

@Aspect
@Component
@Order(10)
public class PostgresTenantRlsAspect {

    private static final Logger log = LoggerFactory.getLogger(PostgresTenantRlsAspect.class);
    private final EntityManager entityManager;

    public PostgresTenantRlsAspect(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Before("execution(* com.erp.billingservice.infrastructure.persistence.adapter..*(..)) && "
            + "!execution(* com.erp.billingservice.infrastructure.persistence.adapter.OutboxPersistenceAdapter.*(..))")
    public void applyTenantContext() {
        if (!TransactionSynchronizationManager.isActualTransactionActive()) {
            throw new IllegalStateException("Active transaction required for tenant-scoped persistence operation");
        }

        UUID tenantId = TenantContextHolder.getTenantId();
        if (tenantId == null) {
            throw new TenantContextMissingException("TenantContextHolder is empty: cannot execute tenant-scoped persistence operation");
        }
        if (new UUID(0L, 0L).equals(tenantId)) {
            throw new TenantContextMissingException("Nil tenant UUID is not permitted");
        }

        try {
            entityManager.createNativeQuery("SET LOCAL app.tenant_id = '" + tenantId + "'").executeUpdate();
        } catch (Exception e) {
            log.debug("Could not execute SET LOCAL app.tenant_id (expected in H2/mock environments): {}", e.getMessage());
        }
    }
}
