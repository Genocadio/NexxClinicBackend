# Product Insurance Data Management

## Overview

This document explains how product insurance data is modeled and managed in the GraphQL API.

The system supports two management styles:
1. Product-level management where insurance coverages can be sent as part of product create or product update.
2. Coverage-level management where a single product insurance coverage can be created, updated, fetched, or deleted independently.

Both styles are supported at the same time.

## Data Model

### Product

Product stores core medicine/service metadata and optional private pricing:

- id: UUID
- name: String (required)
- genericName: String (optional)
- code: String (required, unique)
- description: String (required)
- type: ProductType (required)
- unit: ProductUnit (required)
- metadata: JSON object (stored as JSON string, defaults to {})
- privateRhicPrice: decimal (nullable)
- clinicPrice: decimal (nullable)
- notPaid: boolean (defaults to false) — when true, PRIVATE billing lines for this product bill at 0 without a price lookup
- createdAt, updatedAt: timestamps

Entity reference:
- src/main/java/com/nexxserve/nexxclinic/entity/Product.java

### ProductInsuranceCoverage

Each record links one product to one insurance provider and stores coverage policy fields.

- id: UUID
- productId: UUID (required)
- insuranceProviderId: UUID (required, references InsuranceProvider)
- cost: decimal (required, defaults to 0)
- covered: boolean (derived from cost > 0)
- notPaid: boolean (defaults to false) — when true, INSURANCE billing lines using this coverage bill at 0 without a price lookup
- requireMedicalAdvisor: boolean
- mustPrescribedBy: MustPrescribedBy (defaults to ALL)
- drugAdministrationFrequency: DrugAdministrationFrequency (defaults to CUSTOM_HOURS)
- authorizationRequestReasons: list of strings (defaults to empty list)
- createdAt, updatedAt: timestamps

Rules:
1. One product can have many insurance coverages.
2. A product cannot have duplicate coverage rows for the same insurance provider.
3. Unique constraint is enforced for (product_id, insurance_provider_id).

Entity reference:
- src/main/java/com/nexxserve/nexxclinic/entity/ProductInsuranceCoverage.java

Repository reference:
- src/main/java/com/nexxserve/nexxclinic/repository/ProductInsuranceCoverageRepository.java

## Enums

### ProductType

- DRUG
- MEDICAL_ACT
- BIOLOGICAL_ACT
- CONSUMABLE_DEVICE

Java enum:
- src/main/java/com/nexxserve/nexxclinic/model/ProductType.java

### ProductUnit

Includes tablet, bottle, boxes, tubes, piece, dose, kit, UNKNOWN, and PCS.

Java enum:
- src/main/java/com/nexxserve/nexxclinic/model/ProductUnit.java

### MustPrescribedBy

Current value:
- ALL

Java enum:
- src/main/java/com/nexxserve/nexxclinic/model/MustPrescribedBy.java

### DrugAdministrationFrequency

Current value:
- CUSTOM_HOURS

Java enum:
- src/main/java/com/nexxserve/nexxclinic/model/DrugAdministrationFrequency.java

## GraphQL Inputs

### Product-level inputs

- CreateProductInput
- UpdateProductInput
- SearchProductsInput

### Coverage-level inputs

- CreateProductInsuranceCoverageInput
- UpdateProductInsuranceCoverageInput

Schema reference:
- src/main/resources/graphql/user.graphqls

## GraphQL Operations

All operations return ApiResponse with:
- status
- message
- errors
- data

### Product Queries

1. product(productId: ID!): fetch one product with insuranceCoverages.
2. products(input: SearchProductsInput): paginated list with optional filters:
- name
- type
- page
- size

### Product Mutations

1. createProduct(input: CreateProductInput!)
2. updateProduct(productId: ID!, input: UpdateProductInput!)

When insuranceCoverages is sent in updateProduct, the product coverage set is replaced by the provided list.

### Coverage-level Query

1. productInsuranceCoverage(productInsuranceCoverageId: ID!): fetch one coverage by id.

### Coverage-level Mutations

1. createProductInsuranceCoverage(productId: ID!, input: CreateProductInsuranceCoverageInput!)
2. updateProductInsuranceCoverage(productInsuranceCoverageId: ID!, input: UpdateProductInsuranceCoverageInput!)
3. deleteProductInsuranceCoverage(productInsuranceCoverageId: ID!)

These endpoints manage one coverage row at a time and do not require updating the whole product.

Controller references:
- src/main/java/com/nexxserve/nexxclinic/graphql/ProductQueryController.java
- src/main/java/com/nexxserve/nexxclinic/graphql/ProductMutationController.java

Service reference:
- src/main/java/com/nexxserve/nexxclinic/service/ProductService.java

## Validation and Business Rules

### Product rules

1. name, code, description, type, unit are required on create.
2. code must be unique (case-insensitive).
3. privateRhicPrice and clinicPrice must be >= 0 when provided.
4. metadata must be valid JSON value.

### Coverage rules

1. insuranceProviderId is required.
2. Insurance provider must exist.
3. cost must be >= 0.
4. Duplicate coverage per product and insurance provider is rejected.
5. covered is computed from cost (cost > 0).

## Response Shape Notes

### Product payload

Product response includes:
- core product fields
- privateRhicPrice and clinicPrice
- insuranceCoverages array

Each insurance coverage contains nested insuranceProvider object with:
- id
- insuranceName
- acronym
- defaultCoveragePercentage
- supportedByClinic
- iconUrl

### Pagination payload

Products query returns:
- products: list
- pagination: { total, perPage, currentPage, totalPages }

## Example GraphQL Operations

### Create one coverage only

```graphql
mutation {
  createProductInsuranceCoverage(
    productId: "f5a7815f-52d2-4f0b-a315-f2abbbfe1c90"
    input: {
      insuranceProviderId: "2f2d0ac8-d12e-4e7f-a917-f8f8512d95a8"
      cost: 5000
      requireMedicalAdvisor: false
      mustPrescribedBy: ALL
      drugAdministrationFrequency: CUSTOM_HOURS
      authorizationRequestReasons: []
    }
  ) {
    status
    message
    data
  }
}
```

### Update one coverage only

```graphql
mutation {
  updateProductInsuranceCoverage(
    productInsuranceCoverageId: "5c5963f5-6ff6-4ec0-9174-f3f718ce0f50"
    input: {
      insuranceProviderId: "2f2d0ac8-d12e-4e7f-a917-f8f8512d95a8"
      cost: 8000
      requireMedicalAdvisor: true
      mustPrescribedBy: ALL
      drugAdministrationFrequency: CUSTOM_HOURS
      authorizationRequestReasons: ["HIGH_COST"]
    }
  ) {
    status
    message
    data
  }
}
```

### Fetch one coverage

```graphql
query {
  productInsuranceCoverage(
    productInsuranceCoverageId: "5c5963f5-6ff6-4ec0-9174-f3f718ce0f50"
  ) {
    status
    message
    data
  }
}
```

### Fetch products with pagination and type filter

```graphql
query {
  products(input: { name: "paracetamol", type: DRUG, page: 0, size: 20 }) {
    status
    message
    data
  }
}
```

## Recommended Usage

1. Use product-level coverage updates when replacing a full coverage set in one operation.
2. Use coverage-level operations for incremental day-to-day maintenance.
3. Prefer coverage-level update/delete for admin edits to avoid accidental replacement of other coverage rows.
