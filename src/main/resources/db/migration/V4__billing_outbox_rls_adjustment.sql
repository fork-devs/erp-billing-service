-- =============================================================================
-- V4: billing_outbox is an internal cross-tenant service queue polled by
-- BillingOutboxRelay (@Scheduled without tenant context). Disable RLS on
-- billing_outbox so outbox event delivery is not blocked under erp_billing_app.
-- =============================================================================
DROP POLICY IF EXISTS billing_outbox_tenant_isolation ON billing_outbox;
ALTER TABLE billing_outbox NO FORCE ROW LEVEL SECURITY;
ALTER TABLE billing_outbox DISABLE ROW LEVEL SECURITY;
