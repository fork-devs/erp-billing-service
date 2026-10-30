package com.erp.billingservice.infrastructure.persistence.mapper;

import com.erp.billingservice.domain.model.Correlative;
import com.erp.billingservice.domain.model.Currency;
import com.erp.billingservice.domain.model.Invoice;
import com.erp.billingservice.domain.model.InvoiceLine;
import com.erp.billingservice.domain.model.InvoiceStatus;
import com.erp.billingservice.domain.model.Series;
import com.erp.billingservice.domain.model.VoucherType;
import com.erp.billingservice.infrastructure.persistence.entity.InvoiceEntity;
import com.erp.billingservice.infrastructure.persistence.entity.InvoiceLineEntity;

import java.util.List;

public final class InvoiceEntityMapper {

    private InvoiceEntityMapper() {
    }

    public static Invoice toDomain(InvoiceEntity entity) {
        if (entity == null) {
            return null;
        }

        List<InvoiceLine> lines = entity.getLines().stream()
                .map(InvoiceEntityMapper::toLineDomain)
                .toList();

        return new Invoice(
                entity.getId(),
                entity.getTenantId(),
                entity.getWorkOrderId(),
                VoucherType.valueOf(entity.getVoucherType()),
                new Series(entity.getSeries()),
                new Correlative(entity.getCorrelativo()),
                entity.getCustomerDocument(),
                entity.getCustomerName(),
                Currency.valueOf(entity.getCurrency()),
                lines,
                InvoiceStatus.valueOf(entity.getSunatStatus()),
                entity.getPdfUrl(),
                entity.getQrContent(),
                entity.getVoidReason(),
                entity.getIssuedAt(),
                entity.getVoidedAt()
        );
    }

    public static InvoiceLine toLineDomain(InvoiceLineEntity entity) {
        return new InvoiceLine(
                entity.getId(),
                entity.getTenantId(),
                entity.getCode(),
                entity.getDescription(),
                entity.getQuantity(),
                entity.getUnitPrice(),
                entity.getAffectationType()
        );
    }

    public static InvoiceEntity toEntity(Invoice domain) {
        if (domain == null) {
            return null;
        }

        InvoiceEntity entity = new InvoiceEntity();
        entity.setId(domain.getId());
        entity.setTenantId(domain.getTenantId());
        entity.setWorkOrderId(domain.getWorkOrderId());
        entity.setVoucherType(domain.getVoucherType().name());
        entity.setSeries(domain.getSeries().getValue());
        entity.setCorrelativo(domain.getCorrelative().getValue());
        entity.setCustomerDocument(domain.getCustomerDocument());
        entity.setCustomerName(domain.getCustomerName());
        entity.setCurrency(domain.getCurrency().name());
        entity.setTotalTaxable(domain.getTotalTaxable());
        entity.setTotalIgv(domain.getTotalIgv());
        entity.setTotalAmount(domain.getTotalAmount());
        entity.setSunatStatus(domain.getSunatStatus().name());
        entity.setPdfUrl(domain.getPdfUrl());
        entity.setQrContent(domain.getQrContent());
        entity.setVoidReason(domain.getVoidReason());
        entity.setIssuedAt(domain.getIssuedAt());
        entity.setVoidedAt(domain.getVoidedAt());

        for (InvoiceLine line : domain.getLines()) {
            InvoiceLineEntity lineEntity = new InvoiceLineEntity(
                    line.getId(),
                    line.getTenantId() != null ? line.getTenantId() : domain.getTenantId(),
                    entity,
                    line.getCode(),
                    line.getDescription(),
                    line.getQuantity(),
                    line.getUnitPrice(),
                    line.getSubtotal(),
                    line.getAffectationType()
            );
            entity.addLine(lineEntity);
        }

        return entity;
    }
}
