# Billing Editing (Error Correction) — Frontend Guide

This backend supports **immutable billing versions**. Editing a bill does **not** change the previous billing; it creates a **new billing version** after synchronizing the visit’s products.

## Key concepts

- **`billVisit`**: normal billing (creates billing version `1` if none exists, otherwise next version).
- **`editBillVisit`**: error correction (also creates a new billing version) **and** first applies add/remove/update to the visit department products.
- **Latest billing**: use `visitBilling(visitId)` — always returns the latest billing container (newest).
- **Older billings**: use `visitBillings(visitId)` (list) to show previous versions for audit.

## Mutation: `editBillVisit`

### What it does (in order)
1. Validates user has no unread notes for the visit.
2. Applies visit product corrections:
   - `addedProducts`: adds products to the visit department (or updates quantity if already exists).
   - `removedProductIds`: removes those products from the visit department.
   - `updatedProducts`: updates quantity and resets product billing status to `UNPAID`.
3. Creates a new billing version and bills using `billProducts` + `payments` (billProducts are referenced by `productId`; backend maps them to the visitDepartmentProduct rows).
4. Billing items are snapshotted per version (`VisitDepartmentProductSnapshot`).

### Minimal example
```graphql
mutation EditBill($input: EditBillVisitInput!) {
  editBillVisit(input: $input) {
    status
    message
    data {
      id
      visitId
      createdAt
      departments {
        id
        status
        totalAmount
        insuranceBillings {
          id
          status
          totalAmount
        }
      }
    }
  }
}
```

### Input rules

#### 1) `departments[*].addedProducts`
Use when the original billing missed an item.

- `productId`: the catalog product id
- `quantity`


Frontend must also include the newly-added product in `billProducts` so it gets billed in the new version.

#### 2) `departments[*].removedProductIds`
Use when an item was mistakenly added/billed.

Frontend should:
- remove it from the visit UI list
- and ensure it is **not** included in `billProducts`.

#### 3) `departments[*].updatedProducts`
Use when quantity was incorrect.

If a product is updated, it should also appear in `billProducts` with the corrected `quantity` and/or `unitPrice` (billing uses snapshots).

#### 4) `departments[*].billProducts`
This controls the **new billing version** selections:
- `productId`
- optional overrides: `quantity`, `unitPrice`, `patientInsuranceId`, `isExempted`

#### 5) Payments
Payments are recorded on the new billing version.

## UI flow recommendation

1. Fetch latest billing + visit:
   - `visitBilling(visitId)`
   - `visit(visitId)`
2. User enters edit mode and makes corrections.
3. Build `EditBillVisitInput`:
   - Compute `addedProducts` (new rows)
   - Compute `removedVisitDepartmentProductIds` (deleted rows)
   - Compute `updatedProducts` (changed quantity/processor)
   - Build `billProducts` from the final corrected product list
   - Add payments if needed
4. Call `editBillVisit`.
5. Refresh `visitBilling(visitId)` to show the latest version.

## Notes / constraints

- Editing is intended for **error correction**, not refunds.
- Backend blocks billing/editing if the user has **unread notes** for the visit.
