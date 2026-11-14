@US19 @EP08 @erp-billing-service
# User Story: US19 - Manage quotes lifecycle and digital budget approvals
Feature: Quotes Lifecycle and Digital Budget Approvals
  As a service advisor
  I want to create, update in draft, approve, or cancel detailed quotes for labor and spare parts
  So that repair costs are transparent and frozen once approved by the customer

  Background:
    Given the "erp-billing-service" REST API is available at "/api/v1"

  @US19 @happy-path
  # User Story: US19 - Create a draft quote and record customer approval
  Scenario: Service advisor creates a quote and registers formal customer approval
    Given an authenticated user with role "SERVICE_ADVISOR" in tenant "tenant-lima-01"
    When the client sends a "POST" request to "/api/v1/billing/quotes" with JSON payload:
      """
      {
        "workOrderId": "wo-uuid-01",
        "customerId": "customer-uuid-01",
        "currency": "PEN",
        "items": [
          { "description": "Brake Pad Replacement", "quantity": 1, "unitPrice": 180.00 }
        ]
      }
      """
    Then the response status code should be 201
    And the JSON response field "status" should equal "DRAFT"
    When the client sends a "POST" request to "/api/v1/billing/quotes/{quoteId}/approvals"
    Then the response status code should be 200
    And the JSON response field "status" should equal "APPROVED"

  @US19 @negative
  # User Story: US19 - Prevent editing an already approved quote
  Scenario: Attempting to modify an approved quote returns HTTP 409 Conflict
    Given an authenticated user with role "SERVICE_ADVISOR"
    And an existing quote "quote-approved-01" in status "APPROVED"
    When the client sends a "PUT" request to "/api/v1/billing/quotes/quote-approved-01" with JSON payload:
      """
      {
        "items": [
          { "description": "Modified Item", "quantity": 1, "unitPrice": 99.00 }
        ]
      }
      """
    Then the response status code should be 409
    And the JSON response should contain an error message indicating approved quotes cannot be edited
