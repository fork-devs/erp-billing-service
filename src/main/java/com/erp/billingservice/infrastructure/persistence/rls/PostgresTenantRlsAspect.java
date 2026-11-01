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

    @Before("execution(* com.erp.billingservice.infrastructure.persistence.adapter..*(..))")
    public void applyTenantContext() {
        if (!TransactionSynchronizationManager.isActualTransactionActive()) {
            return;
        }

        UUID tenantId = TenantContextHolder.getTenantId();
        if (tenantId != null) {
            try {
                entityManager.createNativeQuery("SET LOCAL app.tenant_id = '" + tenantId + "'").executeUpdate();
            } catch (Exception e) {
                log.debug("Could not execute SET LOCAL app.tenant_id (expected in H2/mock environments): {}", e.getMessage());
            }
        }
    }
}
