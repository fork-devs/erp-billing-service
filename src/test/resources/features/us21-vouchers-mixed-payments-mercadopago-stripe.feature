@US21 @EP08 @erp-billing-service
# User Story: US21 - Manage vouchers, mixed payments, and digital checkouts with Mercado Pago and Stripe
Feature: Vouchers, Mixed Payments, and Digital Payment Gateways
  As a service advisor or workshop cashier
  I want to issue sales vouchers, record partial/mixed payments, and create Mercado Pago or Stripe checkouts
  So that customer settlements and payment reconciliations are automated

  Background:
    Given the "erp-billing-service" REST API is available at "/api/v1"

  @US21 @happy-path
  # User Story: US21 - Process mixed checkout and record voucher payment
  Scenario: Cashier processes a mixed cash and card checkout for an open invoice
    Given an authenticated user with role "SERVICE_ADVISOR" in tenant "tenant-lima-01"
    And an unpaid invoice "inv-uuid-01" with balance 200.00 PEN
    When the client sends a "POST" request to "/api/v1/billing/checkouts" with JSON payload:
      """
      {
        "invoiceId": "inv-uuid-01",
        "payments": [
          { "method": "CASH", "amount": 100.00 },
          { "method": "CARD", "amount": 100.00 }
        ]
      }
      """
    Then the response status code should be 200
    And the JSON response field "paymentStatus" should equal "PAID"

  @US21 @negative
  # User Story: US21 - Reject Mercado Pago webhook with invalid HMAC signature
  Scenario: Receiving a Mercado Pago webhook with an invalid HMAC signature returns HTTP 401
    Given an incoming webhook payload from Mercado Pago
    When the client sends a "POST" request to "/api/v1/payments/mercadopago/webhooks" with an invalid "x-signature" header
    Then the response status code should be 401
    And the JSON response should contain an error message indicating invalid webhook signature
