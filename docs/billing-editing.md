# Billing Editing (Error Correction) — Frontend Guide

This backend supports **immutable billing versions**. Editing a bill does **not** change the previous billing; it creates a **new billing version** after synchronizing the visit's products.

## Key concepts

- **`billVisit`**: normal billing (creates billing version 1 if none exists, otherwise next version).
- **`editBillVisit`**: error correction — applies product changes AND creates a new billing version in a **single atomic request**.
- **Latest billing**: use `visitBilling(visitId)` — always returns the latest billing container (newest version).
- **Older billings**: use `visitBillings(visitId)` (list) to show previous versions for audit.

## Why not use normal mutations?

On a billed/completed visit, the normal clinical mutations are **blocked** by the backend:

| Normal mutation | Block message on billed visit |
|---|---|
| `addVisitDepartmentProduct` | "Cannot add products to a completed visit." |
| `updateVisitDepartmentProductQuantity` | "Cannot change the quantity of a product in a billed department. Use editBillVisit to correct the billing." |
| `removeVisitDepartmentProduct` | "Cannot remove a product from a billed department. Use editBillVisit to correct the billing." |

These guards exist to protect the billing audit trail. **All product changes on a billed visit must go through `editBillVisit`.**

## Mutation: `editBillVisit`

### How it works (in order)

**Phase 1 — Product corrections** (inside `editBillVisit` itself):
1. Validates user has no unread notes for the visit.
2. Applies visit department product changes:
   - `addedProducts`: adds new products to the visit department (or un-deletes + updates quantity if the product already exists, even if soft-deleted).
   - `removedProductIds`: soft-deletes products from the visit department.
   - `updatedProducts`: updates quantity on existing products and marks them as `CORRECTION_PENDING`.
3. If the visit was `COMPLETED`, it is reopened to `IN_PROGRESS`.

**Phase 2 — New billing version** (called automatically after Phase 1):
4. Converts the edit input into a billing request.
5. Bills all products (including carried-forward departments not in the request) and creates a new immutable billing version.
6. Stamps product statuses (BILLED/EXEMPTED) and re-completes the visit if all products are fully billed.

Everything happens in **one transaction** — if any step fails, all changes roll back.

---

## Input structure

```graphql
input EditBillVisitInput {
  visitId: UUID!
  departments: [EditBillVisitDepartmentInput!]!
}

input EditBillVisitDepartmentInput {
  visitDepartmentId: UUID!

  # Phase 1: product corrections
  addedProducts: [EditBillVisitAddProductInput!]
  removedProductIds: [UUID!]
  updatedProducts: [EditBillVisitUpdateProductInput!]

  # Phase 2: what gets billed in the new version
  billProducts: [EditBillVisitBillProductInput!]!

  # Payments for this department (carried forward if omitted)
  payments: [BillingPaymentInput!]
  note: String
}

input EditBillVisitAddProductInput {
  productId: UUID!      # catalog product id
  quantity: BigDecimal!  # initial quantity
}

input EditBillVisitUpdateProductInput {
  productId: UUID!       # catalog product id (must already exist on the visit department)
  quantity: BigDecimal   # new quantity (null = keep current)
}

input EditBillVisitBillProductInput {
  productId: UUID!                  # catalog product id (backend maps to visitDepartmentProduct)
  quantity: BigDecimal              # quantity to bill (null = use live quantity)
  coverageType: CoverageType!       # PRIVATE or INSURANCE
  patientInsuranceId: UUID          # required when coverageType = INSURANCE
  isExempted: Boolean               # true = exempted from billing (line total = 0)
}
```

---

## How each correction type works

### Adding a new product to a billed visit

Use `addedProducts` to add a product that was missed. The backend creates a new `VisitDepartmentProduct` row (or un-deletes a soft-deleted one) with status `PENDING`/`CORRECTION_PENDING`.

**You must also include the product in `billProducts`** so it gets billed in the new version.

```graphql
mutation EditBill($input: EditBillVisitInput!) {
  editBillVisit(input: $input) {
    status
    message
  }
}
```

```json
{
  "input": {
    "visitId": "...",
    "departments": [
      {
        "visitDepartmentId": "...",
        "addedProducts": [
          { "productId": "new-product-id", "quantity": 2 }
        ],
        "billProducts": [
          { "productId": "new-product-id", "quantity": 2, "coverageType": "PRIVATE" }
        ],
        "payments": []
      }
    ]
  }
}
```

### Adjusting quantity on a billed product

Use `updatedProducts` to change the quantity. The backend updates the live `VisitDepartmentProduct.quantity` and marks it `CORRECTION_PENDING`.

**You must also include the product in `billProducts`** with the corrected quantity so the new billing snapshot reflects the change.

```json
{
  "input": {
    "visitId": "...",
    "departments": [
      {
        "visitDepartmentId": "...",
        "updatedProducts": [
          { "productId": "existing-product-id", "quantity": 5 }
        ],
        "billProducts": [
          { "productId": "existing-product-id", "quantity": 5, "coverageType": "INSURANCE", "patientInsuranceId": "..." }
        ],
        "payments": []
      }
    ]
  }
}
```

### Removing a billed product

Use `removedProductIds` to soft-delete a product. The backend sets `deleted = true` on the `VisitDepartmentProduct`.

**Do NOT include the removed product in `billProducts`.**

```json
{
  "input": {
    "visitId": "...",
    "departments": [
      {
        "visitDepartmentId": "...",
        "removedProductIds": ["product-to-remove-id"],
        "billProducts": [
          { "productId": "other-product-id", "quantity": 1, "coverageType": "PRIVATE" }
        ],
        "payments": []
      }
    ]
  }
}
```

### Changing coverage type / insurance / exemption

Update the `billProducts` entry for the product — no Phase 1 correction is needed since only the billing snapshot changes, not the live product.

```json
{
  "billProducts": [
    {
      "productId": "product-id",
      "quantity": 3,
      "coverageType": "INSURANCE",
      "patientInsuranceId": "new-insurance-id",
      "isExempted": false
    }
  ]
}
```

### Adjusting price

Unit price is **always derived from the product catalog / applied insurance** — it is not client-settable. To change the billed price, the catalog price or insurance coverage must be updated upstream.

---

## Combined example: add + update + remove in one request

```json
{
  "input": {
    "visitId": "visit-123",
    "departments": [
      {
        "visitDepartmentId": "dept-456",
        "addedProducts": [
          { "productId": "new-prod-1", "quantity": 2 }
        ],
        "removedProductIds": ["old-prod-1"],
        "updatedProducts": [
          { "productId": "existing-prod-1", "quantity": 10 }
        ],
        "billProducts": [
          { "productId": "new-prod-1", "quantity": 2, "coverageType": "PRIVATE" },
          { "productId": "existing-prod-1", "quantity": 10, "coverageType": "INSURANCE", "patientInsuranceId": "ins-789" },
          { "productId": "unchanged-prod-1", "quantity": 1, "coverageType": "PRIVATE" }
        ],
        "payments": [
          { "amount": 50.00, "paymentMethod": "CASH" }
        ],
        "note": "Corrected billing: added new prod, removed old prod, updated qty"
      }
    ]
  }
}
```

Note: `unchanged-prod-1` was already billed and is not modified — it must still appear in `billProducts` to be carried into the new billing version.

---

## UI flow

1. Fetch latest billing + visit data:
   - `visitBilling(visitId)` — current billing version
   - `visit(visitId)` — current visit with departments and products
2. User enters edit mode and makes corrections in the UI.
3. Build `EditBillVisitInput`:
   - Compute `addedProducts` (products not previously on the visit)
   - Compute `removedProductIds` (products to soft-delete)
   - Compute `updatedProducts` (products with changed quantity)
   - Build `billProducts` from the **final corrected product list** (all products that should appear in the new billing version)
   - Carry forward payments or add new ones
   - Add a billing note if items are exempted or payment is partial
4. Call `editBillVisit`.
5. Refresh `visitBilling(visitId)` to show the latest version.

---

## Constraints

- Editing is intended for **error correction**, not refunds.
- Backend blocks billing/editing if the user has **unread notes** for the visit.
- `editBillVisit` is restricted to users with roles: `FINANCE`, `ADMIN`, `CLINIC_ADMIN`.
- Profile-sourced products cannot be removed via `removedProductIds` — use `changeVisitDepartmentProfile` instead.
- The entire `editBillVisit` is atomic — any validation failure rolls back all product changes.
