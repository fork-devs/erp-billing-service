-- =============================================================================
-- V5: Payment Intents for Mercado Pago + branch_id on invoices
-- =============================================================================

ALTER TABLE invoices
    ADD COLUMN IF NOT EXISTS branch_id UUID;

CREATE TABLE IF NOT EXISTS payment_intents (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL,
    quote_id UUID NOT NULL,
    preference_id VARCHAR(120) NOT NULL,
    external_reference VARCHAR(120) NOT NULL,
    amount NUMERIC(12, 2) NOT NULL,
    currency VARCHAR(10) NOT NULL DEFAULT 'PEN',
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    voucher_type VARCHAR(20),
    customer_document_type VARCHAR(20),
    customer_document_number VARCHAR(30),
    customer_name VARCHAR(255),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_payment_intents_preference UNIQUE (preference_id),
    CONSTRAINT uq_payment_intents_ext_ref UNIQUE (external_reference)
);

ALTER TABLE payment_intents ENABLE ROW LEVEL SECURITY;
ALTER TABLE payment_intents FORCE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation_payment_intents ON payment_intents
    USING (tenant_id = current_setting('app.current_tenant_id', true)::uuid)
    WITH CHECK (tenant_id = current_setting('app.current_tenant_id', true)::uuid);
