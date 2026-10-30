package com.erp.billingservice.interfaces.rest.controller;

import com.erp.billingservice.application.usecase.EmitInvoiceUseCase;
import com.erp.billingservice.application.usecase.GetInvoicePdfUseCase;
import com.erp.billingservice.application.usecase.GetInvoiceUseCase;
import com.erp.billingservice.application.usecase.VoidInvoiceUseCase;
import com.erp.billingservice.domain.exception.InvoiceNotFoundException;
import com.erp.billingservice.domain.model.Invoice;
import com.erp.billingservice.interfaces.rest.dto.EmitInvoiceRequest;
import com.erp.billingservice.interfaces.rest.dto.InvoiceResponse;
import com.erp.billingservice.interfaces.rest.dto.VoidInvoiceRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/billing/invoices")
@Validated
public class InvoiceController {

    private final EmitInvoiceUseCase emitInvoiceUseCase;
    private final GetInvoiceUseCase getInvoiceUseCase;
    private final VoidInvoiceUseCase voidInvoiceUseCase;
    private final GetInvoicePdfUseCase getInvoicePdfUseCase;

    public InvoiceController(
            EmitInvoiceUseCase emitInvoiceUseCase,
            GetInvoiceUseCase getInvoiceUseCase,
            VoidInvoiceUseCase voidInvoiceUseCase,
            GetInvoicePdfUseCase getInvoicePdfUseCase
    ) {
        this.emitInvoiceUseCase = emitInvoiceUseCase;
        this.getInvoiceUseCase = getInvoiceUseCase;
        this.voidInvoiceUseCase = voidInvoiceUseCase;
        this.getInvoicePdfUseCase = getInvoicePdfUseCase;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('SUPERADMIN', 'ADMIN_TALLER', 'ASESOR')")
    public ResponseEntity<InvoiceResponse> emitInvoice(@Valid @RequestBody EmitInvoiceRequest request) {
        List<EmitInvoiceUseCase.InvoiceItemCommand> itemCommands = request.items().stream()
                .map(item -> new EmitInvoiceUseCase.InvoiceItemCommand(
                        item.code(),
                        item.description(),
                        item.quantity(),
                        item.unitPrice(),
                        item.affectationType()
                ))
                .toList();

        EmitInvoiceUseCase.EmitInvoiceCommand command = new EmitInvoiceUseCase.EmitInvoiceCommand(
                request.workOrderId(),
                request.voucherType(),
                request.series(),
                request.customerDocument(),
                request.customerName(),
                request.currency(),
                itemCommands
        );

        Invoice invoice = emitInvoiceUseCase.emitInvoice(command);
        return ResponseEntity.status(HttpStatus.CREATED).body(InvoiceResponse.fromDomain(invoice));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('SUPERADMIN', 'ADMIN_TALLER', 'ASESOR', 'MECANICO')")
    public ResponseEntity<List<InvoiceResponse>> listInvoices(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        List<Invoice> invoices = getInvoiceUseCase.listInvoices(page, size);
        List<InvoiceResponse> responses = invoices.stream()
                .map(InvoiceResponse::fromDomain)
                .toList();
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPERADMIN', 'ADMIN_TALLER', 'ASESOR', 'MECANICO')")
    public ResponseEntity<InvoiceResponse> getInvoiceById(@PathVariable UUID id) {
        Invoice invoice = getInvoiceUseCase.getById(id)
                .orElseThrow(() -> new InvoiceNotFoundException(id));
        return ResponseEntity.ok(InvoiceResponse.fromDomain(invoice));
    }

    @GetMapping("/{id}/pdf")
    @PreAuthorize("hasAnyRole('SUPERADMIN', 'ADMIN_TALLER', 'ASESOR', 'MECANICO')")
    public ResponseEntity<byte[]> downloadPdf(@PathVariable UUID id) {
        byte[] pdfBytes = getInvoicePdfUseCase.getPdf(id);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.set(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"invoice-" + id + ".pdf\"");
        return new ResponseEntity<>(pdfBytes, headers, HttpStatus.OK);
    }

    @PostMapping("/{id}/void")
    @PreAuthorize("hasAnyRole('SUPERADMIN', 'ADMIN_TALLER')")
    public ResponseEntity<InvoiceResponse> voidInvoice(
            @PathVariable UUID id,
            @Valid @RequestBody VoidInvoiceRequest request
    ) {
        Invoice voided = voidInvoiceUseCase.voidInvoice(id, request.reason());
        return ResponseEntity.ok(InvoiceResponse.fromDomain(voided));
    }
}
