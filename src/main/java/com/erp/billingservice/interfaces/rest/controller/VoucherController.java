package com.erp.billingservice.interfaces.rest.controller;

import com.erp.billingservice.application.usecase.ExecuteCheckoutUseCase;
import com.erp.billingservice.application.usecase.GetInvoiceUseCase;
import com.erp.billingservice.application.usecase.RegisterPaymentUseCase;
import com.erp.billingservice.domain.exception.InvoiceNotFoundException;
import com.erp.billingservice.domain.model.Currency;
import com.erp.billingservice.domain.model.Invoice;
import com.erp.billingservice.domain.model.Payment;
import com.erp.billingservice.domain.port.InvoiceRepositoryPort;
import com.erp.billingservice.domain.port.PaymentRepositoryPort;
import com.erp.billingservice.interfaces.rest.dto.DirectCheckoutRequest;
import com.erp.billingservice.interfaces.rest.dto.VoucherPaymentRequest;
import com.erp.billingservice.interfaces.rest.dto.VoucherResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/vouchers")
@Validated
public class VoucherController {

    private final InvoiceRepositoryPort invoiceRepository;
    private final GetInvoiceUseCase getInvoiceUseCase;
    private final RegisterPaymentUseCase registerPaymentUseCase;
    private final PaymentRepositoryPort paymentRepository;
    private final ExecuteCheckoutUseCase executeCheckoutUseCase;

    public VoucherController(
            InvoiceRepositoryPort invoiceRepository,
            GetInvoiceUseCase getInvoiceUseCase,
            RegisterPaymentUseCase registerPaymentUseCase,
            PaymentRepositoryPort paymentRepository
    ) {
        this(invoiceRepository, getInvoiceUseCase, registerPaymentUseCase, paymentRepository, null);
    }

    @org.springframework.beans.factory.annotation.Autowired
    public VoucherController(
            InvoiceRepositoryPort invoiceRepository,
            GetInvoiceUseCase getInvoiceUseCase,
            RegisterPaymentUseCase registerPaymentUseCase,
            PaymentRepositoryPort paymentRepository,
            @org.springframework.beans.factory.annotation.Autowired(required = false) ExecuteCheckoutUseCase executeCheckoutUseCase
    ) {
        this.invoiceRepository = invoiceRepository;
        this.getInvoiceUseCase = getInvoiceUseCase;
        this.registerPaymentUseCase = registerPaymentUseCase;
        this.paymentRepository = paymentRepository;
        this.executeCheckoutUseCase = executeCheckoutUseCase;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('SUPERADMIN', 'WORKSHOP_ADMIN', 'ADMIN_TALLER', 'SERVICE_ADVISOR', 'ASESOR')")
    public ResponseEntity<VoucherResponse> emitVoucher(@Valid @RequestBody DirectCheckoutRequest request) {
        if (executeCheckoutUseCase != null) {
            ExecuteCheckoutUseCase.DirectCheckoutCommand command = new ExecuteCheckoutUseCase.DirectCheckoutCommand(
                    request.quoteId(),
                    request.resolvedVoucherType(),
                    request.customerDocumentType(),
                    request.customerDocumentNumber(),
                    request.customerName(),
                    request.method() != null ? request.method() : "CASH"
            );
            ExecuteCheckoutUseCase.CheckoutResult result = executeCheckoutUseCase.executeDirectCheckout(command);
            return ResponseEntity.status(HttpStatus.CREATED).body(VoucherResponse.from(result.invoice(), List.of(result.payment())));
        }
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }


    @GetMapping("/{voucherId}")
    @PreAuthorize("hasAnyRole('SUPERADMIN', 'WORKSHOP_ADMIN', 'ADMIN_TALLER', 'SERVICE_ADVISOR', 'ASESOR', 'TECHNICIAN', 'MECANICO')")
    public ResponseEntity<VoucherResponse> getVoucherById(@PathVariable UUID voucherId) {
        Invoice invoice = invoiceRepository.findById(voucherId)
                .orElseThrow(() -> new InvoiceNotFoundException(voucherId));
        List<Payment> payments = paymentRepository.findByInvoiceId(voucherId);
        return ResponseEntity.ok(VoucherResponse.from(invoice, payments));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('SUPERADMIN', 'WORKSHOP_ADMIN', 'ADMIN_TALLER', 'SERVICE_ADVISOR', 'ASESOR', 'TECHNICIAN', 'MECANICO')")
    public ResponseEntity<List<VoucherResponse>> listVouchers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        List<Invoice> invoices = getInvoiceUseCase.listInvoices(page, size);
        List<VoucherResponse> responses = invoices.stream()
                .map(inv -> {
                    List<Payment> payments = paymentRepository.findByInvoiceId(inv.getId());
                    return VoucherResponse.from(inv, payments);
                })
                .toList();
        return ResponseEntity.ok(responses);
    }

    @PostMapping("/{voucherId}/payments")
    @PreAuthorize("hasAnyRole('SUPERADMIN', 'WORKSHOP_ADMIN', 'ADMIN_TALLER', 'SERVICE_ADVISOR', 'ASESOR')")
    public ResponseEntity<VoucherResponse> registerVoucherPayment(
            @PathVariable UUID voucherId,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKeyHeader,
            @Valid @RequestBody VoucherPaymentRequest request
    ) {
        Invoice invoice = invoiceRepository.findById(voucherId)
                .orElseThrow(() -> new InvoiceNotFoundException(voucherId));

        String idempotencyKey = (idempotencyKeyHeader != null && !idempotencyKeyHeader.trim().isEmpty())
                ? idempotencyKeyHeader.trim()
                : UUID.randomUUID().toString();

        RegisterPaymentUseCase.RegisterPaymentCommand command = new RegisterPaymentUseCase.RegisterPaymentCommand(
                voucherId,
                request.method(),
                null,
                idempotencyKey,
                request.amount(),
                Currency.PEN
        );

        registerPaymentUseCase.registerPayment(command);

        List<Payment> payments = paymentRepository.findByInvoiceId(voucherId);
        return ResponseEntity.status(HttpStatus.CREATED).body(VoucherResponse.from(invoice, payments));
    }

    @DeleteMapping("/{voucherId}/payments/{paymentId}")
    @PreAuthorize("hasAnyRole('SUPERADMIN', 'WORKSHOP_ADMIN', 'ADMIN_TALLER', 'SERVICE_ADVISOR', 'ASESOR')")
    public ResponseEntity<Void> deleteVoucherPayment(
            @PathVariable UUID voucherId,
            @PathVariable UUID paymentId
    ) {
        invoiceRepository.findById(voucherId)
                .orElseThrow(() -> new InvoiceNotFoundException(voucherId));

        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new IllegalArgumentException("Payment not found with id: " + paymentId));

        if (!payment.getInvoiceId().equals(voucherId)) {
            throw new IllegalArgumentException("Payment does not belong to voucher: " + voucherId);
        }

        paymentRepository.deleteById(paymentId);
        return ResponseEntity.noContent().build();
    }
}
