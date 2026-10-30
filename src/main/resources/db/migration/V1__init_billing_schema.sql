-- =============================================================================
-- ERP BILLING SERVICE - V1: INVOICES, LINES, PAYMENTS AND OUTBOX
-- =============================================================================

CREATE TABLE IF NOT EXISTS invoices (
    id                 UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id          UUID NOT NULL,
    work_order_id      UUID NOT NULL,
    voucher_type       VARCHAR(20) NOT NULL,
    serie              VARCHAR(10) NOT NULL,
    correlativo        INTEGER NOT NULL,
    customer_document  VARCHAR(20) NOT NULL,
    customer_name      VARCHAR(255) NOT NULL,
    currency           VARCHAR(3) NOT NULL DEFAULT 'PEN',
    total_taxable      NUMERIC(12,2) NOT NULL DEFAULT 0.00,
    total_igv          NUMERIC(12,2) NOT NULL DEFAULT 0.00,
    total_amount       NUMERIC(12,2) NOT NULL DEFAULT 0.00,
    sunat_status       VARCHAR(30) NOT NULL DEFAULT 'BORRADOR',
    pdf_url            VARCHAR(500),
    qr_content         TEXT,
    void_reason        TEXT,
    issued_at          TIMESTAMP NOT NULL DEFAULT now(),
    voided_at          TIMESTAMP,
    CONSTRAINT uq_invoice_tenant_serie_correlativo UNIQUE (tenant_id, serie, correlativo),
    CONSTRAINT ck_invoice_voucher_type CHECK (voucher_type IN ('FACTURA', 'BOLETA')),
    CONSTRAINT ck_invoice_sunat_status CHECK (sunat_status IN ('BORRADOR', 'PENDIENTE_SUNAT', 'ACEPTADO', 'OBSERVADO', 'ANULADO'))
);

CREATE INDEX IF NOT EXISTS idx_invoices_work_order ON invoices (tenant_id, work_order_id);
CREATE INDEX IF NOT EXISTS idx_invoices_tenant_issued_at ON invoices (tenant_id, issued_at DESC);

CREATE TABLE IF NOT EXISTS invoice_lines (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id         UUID NOT NULL,
    invoice_id        UUID NOT NULL REFERENCES invoices(id) ON DELETE CASCADE,
    code              VARCHAR(50) NOT NULL,
    description       VARCHAR(255) NOT NULL,
    quantity          NUMERIC(10,2) NOT NULL,
    unit_price        NUMERIC(12,2) NOT NULL,
    subtotal          NUMERIC(12,2) NOT NULL,
    affectation_type  VARCHAR(30) NOT NULL DEFAULT 'TAXABLE_ONEROUS'
);

CREATE INDEX IF NOT EXISTS idx_invoice_lines_invoice_id ON invoice_lines (tenant_id, invoice_id);

CREATE TABLE IF NOT EXISTS payments (
    id                   UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id            UUID NOT NULL,
    invoice_id           UUID NOT NULL REFERENCES invoices(id) ON DELETE RESTRICT,
    provider             VARCHAR(50) NOT NULL DEFAULT 'EFECTIVO',
    provider_payment_id  VARCHAR(100),
    idempotency_key      VARCHAR(128) NOT NULL,
    amount               NUMERIC(12,2) NOT NULL,
    currency             VARCHAR(3) NOT NULL DEFAULT 'PEN',
    status               VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    created_at           TIMESTAMP NOT NULL DEFAULT now(),
    CONSTRAINT uq_payment_tenant_idempotency UNIQUE (tenant_id, idempotency_key),
    CONSTRAINT ck_payment_status CHECK (status IN ('PENDING', 'CONFIRMED', 'FAILED'))
);

CREATE INDEX IF NOT EXISTS idx_payments_invoice_id ON payments (tenant_id, invoice_id);

CREATE TABLE IF NOT EXISTS billing_outbox (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id       UUID NOT NULL,
    aggregate_id    UUID NOT NULL,
    event_id        UUID NOT NULL UNIQUE,
    event_type      VARCHAR(64) NOT NULL,
    payload         JSONB NOT NULL,
    status          VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    attempts        INT NOT NULL DEFAULT 0,
    next_attempt_at TIMESTAMP NOT NULL DEFAULT now(),
    created_at      TIMESTAMP NOT NULL DEFAULT now(),
    published_at    TIMESTAMP,
    last_error      TEXT,
    CONSTRAINT ck_billing_outbox_status CHECK (status IN ('PENDING', 'PUBLISHED', 'DEAD_LETTER'))
);

CREATE INDEX IF NOT EXISTS idx_billing_outbox_poll ON billing_outbox (status, next_attempt_at)
    WHERE status = 'PENDING';
