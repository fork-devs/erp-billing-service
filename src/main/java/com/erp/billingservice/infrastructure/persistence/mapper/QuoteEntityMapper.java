package com.erp.billingservice.infrastructure.persistence.mapper;

import com.erp.billingservice.domain.model.Quote;
import com.erp.billingservice.domain.model.QuoteItem;
import com.erp.billingservice.domain.model.QuoteItemType;
import com.erp.billingservice.domain.model.QuoteStatus;
import com.erp.billingservice.infrastructure.persistence.entity.QuoteEntity;
import com.erp.billingservice.infrastructure.persistence.entity.QuoteItemEntity;

import java.util.List;

public final class QuoteEntityMapper {

    private QuoteEntityMapper() {
    }

    public static Quote toDomain(QuoteEntity entity) {
        if (entity == null) {
            return null;
        }

        List<QuoteItem> items = entity.getItems() != null
                ? entity.getItems().stream().map(QuoteEntityMapper::toItemDomain).toList()
                : List.of();

        return new Quote(
                entity.getId(),
                entity.getTenantId(),
                entity.getWorkOrderId(),
                entity.getBranchId(),
                entity.getDiscountPercentage(),
                entity.getNotes(),
                items,
                QuoteStatus.valueOf(entity.getStatus()),
                entity.getCreatedAt(),
                entity.getApprovedAt(),
                entity.getCanceledAt()
        );
    }

    public static QuoteItem toItemDomain(QuoteItemEntity entity) {
        if (entity == null) {
            return null;
        }
        return new QuoteItem(
                entity.getId(),
                entity.getTenantId(),
                entity.getQuote() != null ? entity.getQuote().getId() : null,
                QuoteItemType.valueOf(entity.getType()),
                entity.getName(),
                entity.getQuantity(),
                entity.getUnitPrice(),
                entity.getTotal()
        );
    }

    public static QuoteEntity toEntity(Quote domain) {
        if (domain == null) {
            return null;
        }

        QuoteEntity entity = new QuoteEntity();
        entity.setId(domain.getId());
        entity.setTenantId(domain.getTenantId());
        entity.setWorkOrderId(domain.getWorkOrderId());
        entity.setBranchId(domain.getBranchId());
        entity.setSubtotalAmount(domain.getSubtotalAmount());
        entity.setDiscountPercentage(domain.getDiscountPercentage());
        entity.setTotalAmount(domain.getTotalAmount());
        entity.setStatus(domain.getStatus().name());
        entity.setNotes(domain.getNotes());
        entity.setCreatedAt(domain.getCreatedAt());
        entity.setApprovedAt(domain.getApprovedAt());
        entity.setCanceledAt(domain.getCanceledAt());

        if (domain.getItems() != null) {
            for (QuoteItem item : domain.getItems()) {
                QuoteItemEntity itemEntity = new QuoteItemEntity(
                        item.getId(),
                        item.getTenantId() != null ? item.getTenantId() : domain.getTenantId(),
                        entity,
                        item.getType().name(),
                        item.getName(),
                        item.getQuantity(),
                        item.getUnitPrice(),
                        item.getTotal()
                );
                entity.addItem(itemEntity);
            }
        }

        return entity;
    }
}
