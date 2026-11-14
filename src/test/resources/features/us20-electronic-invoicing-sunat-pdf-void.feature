@US20 @EP08 @erp-billing-service
# User Story: US20 - Issue SUNAT electronic invoices, download PDF, and manage tax voids
Feature: SUNAT Electronic Invoicing, PDF Generation, and Tax Voids
  As a service advisor or workshop administrator
  I want to issue SUNAT-validated Facturas and Boletas, query CDR/XML status, download PDFs, and void invoices
  So that the workshop complies with Peruvian tax regulations without manual re-entry

  Background:
    Given the "erp-billing-service" REST API is available at "/api/v1/billing"

  @US20 @happy-path
  # User Story: US20 - Issue an electronic Factura and retrieve PDF representation
  Scenario: Service advisor issues an electronic Factura for a corporate customer with valid RUC
    Given an authenticated user with role "SERVICE_ADVISOR" in tenant "tenant-lima-01"
    And an approved quote or completed work order "wo-uuid-01"
    When the client sends a "POST" request to "/api/v1/billing/invoices" with JSON payload:
      """
      {
        "workOrderId": "wo-uuid-01",
        "documentType": "FACTURA",
        "customerRuc": "20512345678",
        "currency": "PEN",
        "totalAmount": 212.40
      }
      """
    Then the response status code should be 201
    And the JSON response should contain "id", "seriesNumber", and "sunatStatus"
    When the client sends a "GET" request to "/api/v1/billing/invoices/{invoiceId}/pdf"
    Then the response status code should be 200
    And the response content type should be "application/pdf"

  @US20 @negative
  # User Story: US20 - Reject Factura issuance without valid 11-digit RUC
  Scenario: Issuing a Factura with an invalid RUC returns HTTP 400 Bad Request
    Given an authenticated user with role "SERVICE_ADVISOR"
    When the client sends a "POST" request to "/api/v1/billing/invoices" with JSON payload:
      """
      {
        "workOrderId": "wo-uuid-01",
        "documentType": "FACTURA",
        "customerRuc": "12345",
        "currency": "PEN",
        "totalAmount": 100.00
      }
      """
    Then the response status code should be 400
    And the JSON response should contain a validation error for "customerRuc"
