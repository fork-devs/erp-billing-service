package com.erp.billingservice.domain.port;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface WorkOrderClientPort {
    Optional<WorkOrderDetail> getWorkOrder(UUID workOrderId);

    record WorkOrderDetail(
            UUID orderId,
            String number,
            String status,
            UUID branchId,
            UUID customerId,
            String customerName,
            String customerDocument,
            List<WorkOrderItem> items
    ) {}

    record WorkOrderItem(
            String type,
            String description,
            BigDecimal quantity,
            BigDecimal unitPrice,
            BigDecimal subtotal
    ) {}
}
