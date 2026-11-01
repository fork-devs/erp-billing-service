-- =============================================================================
-- ERP BILLING SERVICE - V3: QUOTES, QUOTE ITEMS, AND INVOICE LINKING
-- =============================================================================

CREATE TABLE IF NOT EXISTS quotes (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id           UUID NOT NULL,
    work_order_id       UUID NOT NULL,
    branch_id           UUID NOT NULL,
    subtotal_amount     NUMERIC(12,2) NOT NULL DEFAULT 0.00,
    discount_percentage NUMERIC(5,2) NOT NULL DEFAULT 0.00,
    total_amount        NUMERIC(12,2) NOT NULL DEFAULT 0.00,
    status              VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    notes               TEXT,
    created_at          TIMESTAMP NOT NULL DEFAULT now(),
    approved_at         TIMESTAMP,
    canceled_at         TIMESTAMP,
    CONSTRAINT uq_quote_tenant_work_order UNIQUE (tenant_id, work_order_id),
    CONSTRAINT ck_quote_status CHECK (status IN ('DRAFT', 'APPROVED', 'CANCELED')),
    CONSTRAINT ck_quote_discount CHECK (discount_percentage >= 0 AND discount_percentage <= 100)
);

CREATE INDEX IF NOT EXISTS idx_quotes_tenant_branch ON quotes (tenant_id, branch_id);
CREATE INDEX IF NOT EXISTS idx_quotes_tenant_work_order ON quotes (tenant_id, work_order_id);

CREATE TABLE IF NOT EXISTS quote_items (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id         UUID NOT NULL,
    quote_id          UUID NOT NULL REFERENCES quotes(id) ON DELETE CASCADE,
    type              VARCHAR(20) NOT NULL,
    name              VARCHAR(255) NOT NULL,
    quantity          NUMERIC(10,2) NOT NULL DEFAULT 1.00,
    unit_price        NUMERIC(12,2) NOT NULL DEFAULT 0.00,
    total             NUMERIC(12,2) NOT NULL DEFAULT 0.00,
    CONSTRAINT ck_quote_item_type CHECK (type IN ('SERVICE', 'PRODUCT'))
);

CREATE INDEX IF NOT EXISTS idx_quote_items_tenant_quote ON quote_items (tenant_id, quote_id);

-- Link quotes to invoices for checkouts
ALTER TABLE invoices ADD COLUMN IF NOT EXISTS quote_id UUID REFERENCES quotes(id);
CREATE UNIQUE INDEX IF NOT EXISTS uq_invoices_tenant_quote_id ON invoices (tenant_id, quote_id) WHERE quote_id IS NOT NULL;

-- -----------------------------------------------------------------------------
-- ROW-LEVEL SECURITY FOR QUOTES
-- -----------------------------------------------------------------------------

ALTER TABLE quotes ENABLE ROW LEVEL SECURITY;
ALTER TABLE quotes FORCE ROW LEVEL SECURITY;

ALTER TABLE quote_items ENABLE ROW LEVEL SECURITY;
ALTER TABLE quote_items FORCE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS quote_tenant_isolation ON quotes;
CREATE POLICY quote_tenant_isolation ON quotes
    FOR ALL
    USING (tenant_id = NULLIF(current_setting('app.tenant_id', true), '')::uuid)
    WITH CHECK (tenant_id = NULLIF(current_setting('app.tenant_id', true), '')::uuid);

DROP POLICY IF EXISTS quote_items_tenant_isolation ON quote_items;
CREATE POLICY quote_items_tenant_isolation ON quote_items
    FOR ALL
    USING (tenant_id = NULLIF(current_setting('app.tenant_id', true), '')::uuid)
    WITH CHECK (tenant_id = NULLIF(current_setting('app.tenant_id', true), '')::uuid);

GRANT SELECT, INSERT, UPDATE, DELETE ON quotes TO erp_billing_app;
GRANT SELECT, INSERT, UPDATE, DELETE ON quote_items TO erp_billing_app;
