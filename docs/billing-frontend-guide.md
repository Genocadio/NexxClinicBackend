# Billing Flow — Frontend Guide

Everything the frontend needs to know to bill a visit, collect payments, correct billing, and fetch invoices. Written from the backend implementation (`VisitBillingService`, GraphQL schema, entities).

---

## 1. Mental model (read this first)

Billing is **immutable and versioned**. Every `billVisit` / `editBillVisit` creates a **new billing version**; the previous version is never mutated. The "current bill" is always the **latest version**.

```
Visit (visitId)
└── VisitBilling                     ← one per version ("billing container")
    └── VisitDepartmentBilling       ← one per TOP-LEVEL visit department
        ├── payments[]               ← cash/mobile-money/etc. paid against this department
        └── insuranceBillings[]      ← one per insurance bucket (uninsured + each applied insurer)
            └── items[]              ← one VisitBillingItem per billed product line (snapshot)
```

Money fields (all are `Float`, money = 2 decimals):

| Field | Meaning |
|---|---|
| `totalAmount` | Sum of line totals (unitPrice × quantity) |
| `insuranceCoveredAmount` | Amount the applied insurer pays |
| `patientPayableAmount` | What the patient must pay = total − insurance covered |
| `paidAmount` | Cash actually received (from payments) |
| `outstandingAmount` | **The remaining balance** = patientPayable − paid |

`status` is `UNPAID | PARTIALLY_PAID | PAID`:
- `patientPayable == 0` → `PAID`
- `paid == 0` → `UNPAID`
- `0 < paid < patientPayable` → `PARTIALLY_PAID`
- `paid >= patientPayable` → `PAID`

Product lifecycle: `PENDING` (added, not billed) → `BILLED` (billed) or `EXEMPTED` (billed at zero). `CORRECTION_PENDING` is transient and only set/consumed inside `editBillVisit` — never send it.

**Global gate:** every billing operation (`billVisit`, `editBillVisit`, `recordVisitBillingPayment`, `generateInvoice`) is **blocked while the acting user has unread notes** on the visit. Check note read-state first.

---

## 2. Before billing — how products get onto a visit

Products are added to a visit department **before** billing:

```graphql
mutation {
  addVisitDepartmentProduct(input: {
    visitId: "...",          # the visit
    departmentId: "...",     # the VISIT department id (from visit → departments[])
    productId: "...",        # catalog product id
    quantity: 2.0,           # optional, defaults to 1
    price: 5000.0,           # optional override, defaults to the catalog clinic price
    processorId: "...",      # optional
    status: PENDING          # optional — only PENDING or UNPAID may be sent
  }) { status message data { ... } }
}
```

- Allowed `status` values: **`PENDING`, `UNPAID`** only. `BILLED`, `EXEMPTED`, `CORRECTION_PENDING` are managed exclusively by the billing service.
- Blocked when the visit is `COMPLETED` / `CANCELLED`, or the department is `COMPLETED` / `CANCELLED`, or the department is in **`BILLING`** status — once a department is handed to finance it is frozen for product additions, regardless of whether billing has run yet (see §7 for the freeze).
- Profile-sourced products (`source: PROFILE`) are added automatically when a department profile is applied — they cannot be removed individually.

Other pre-billing product mutations: `updateVisitDepartmentProductQuantity`, `updateVisitDepartmentProductStatus`, `removeVisitDepartmentProduct(visitDepartmentProductId)`.

**Handoff to finance:** when the clinician submits the final consultation (`completeConsultationVisit(input, final: true)`), every non-cancelled department is set to **`BILLING`** status. From then on the department is frozen for clinical/product edits (with the exceptions in §7).

---

## 3. First billing — `billVisit`

This is **the mutation to start with**. It bills the current, not-yet-billed products of one or more top-level departments and records payments in the same call.

> ⚠️ `billVisit` is **first-time only**. If the visit already has any billing, it errors with *"This visit has already been billed. Use editBillVisit to correct the billing."*

### Building the input from the visit

Query the visit first (e.g. `visit(visitId)`), which returns departments → products. Map each product to a `products[]` entry:

| From the visit | Send as |
|---|---|
| `visit.id` | `input.visitId` |
| each **top-level** department's `id` | `departments[].visitDepartmentId` |
| each product's `id` | `departments[].products[].visitDepartmentProductId` |
| product's `quantity` | `departments[].products[].quantity` (only if you want to change it for billing) |
| product's `price` | `departments[].products[].unitPrice` (only if you want to override the price) |
| child department's products | also `parentVisitDepartmentId` = the visit department that **owns** the product (the child's own id — the API field name "parent" is misleading). Products on child departments bill under their **root** department. |

```graphql
mutation Bill($input: BillVisitInput!) {
  billVisit(input: $input) {
    status
    message
    data {
      id
      visitId
      departments {
        id
        status
        totalAmount
        patientPayableAmount
        paidAmount
        outstandingAmount
        insuranceBillings {
          id
          status
          totalAmount
          patientPayableAmount
          paidAmount
          outstandingAmount
          items { id productName unitPriceSnapshot quantitySnapshot patientPayableAmount }
        }
      }
    }
  }
}
```

### Input shape

```json
{
  "visitId": "uuid",
  "departments": [
    {
      "visitDepartmentId": "uuid",        // REQUIRED — must be a TOP-LEVEL visit department
      "products": [                        // REQUIRED — at least one per department
        {
          "visitDepartmentProductId": "uuid",  // REQUIRED
          "parentVisitDepartmentId": "uuid",   // for products on a child dept: the visit department that OWNS the product (the child's own id)
          "quantity": 2.0,                     // optional; billing-snapshot only (does NOT mutate the product)
          "unitPrice": 5000.0,                 // optional; billing-snapshot only (does NOT mutate the product)
          "patientInsuranceId": "uuid",        // optional; must be linked to the visit AND cover the product
          "isExempted": true                   // optional; see §3.4
        }
      ],
      "payments": [                            // optional
        { "amount": 10000.0, "paymentMethod": "CASH", "reference": "ref-123" }
      ],
      "note": "…"                              // REQUIRED when any product is exempted OR payment < patientPayable (see §3.5)
    }
  ]
}
```

### 3.1 Rules

- Every `visitDepartmentId` must be a **top-level** visit department of this visit, and unique in the payload.
- Every `visitDepartmentProductId` must exist, belong to (a descendant of) the declared department, and be **not yet billed** (`PENDING` / `UNPAID`). Already-`BILLED`/`EXEMPTED` products are rejected → use `editBillVisit`.
- Duplicate product ids are rejected.
- `quantity > 0`, `unitPrice >= 0`, `amount > 0`.

### 3.2 Price & quantity overrides

- `unitPrice` overrides the price **for the billing snapshot only**. The stored product price (`visitDepartmentProduct.price`) is untouched by `billVisit`.
- `quantity` overrides the quantity **for the billing snapshot only** in `billVisit` mode.
- Line total = `unitPrice × quantity` (money-rounded to 2 dp).

### 3.3 Insurance

- Products are grouped into `insuranceBillings` by (root department, applied insurer). A product with no insurer goes into the uninsured bucket (`patientInsurance: null`).
- `patientInsuranceId` — when sent, it must be one of the visit's linked insurances **and** the product must have a `ProductInsuranceCoverage` for that insurer with `covered = true`. Otherwise: error.
- When omitted, the backend auto-picks the first visit insurance whose coverage covers the product.
- Insurance covered amount = `coverage.cost × quantity`, **capped at the line total**. The patient pays the remainder.

### 3.4 Exemptions (free/zero-priced lines)

- `isExempted: true` forces the line to zero: `unitPrice = 0`, `quantity = 1`, `total = 0`, `insurance = 0`, `patient = 0`, product status → `EXEMPTED`.
- **A billing `note` is REQUIRED for the department** when any of its products is exempted.

### 3.5 Payments & the remaining balance

- Payments are per **top-level department** and recorded once per department (distributed across its insurance buckets).
- Total paid per department must never exceed `patientPayableAmount` (overpayment → error).
- The remaining balance is computed and stored immediately: `outstandingAmount = patientPayable − paid`.
- **A billing `note` is REQUIRED for the department when the payment does not cover the full patient payable** (i.e. a balance is left).
- To bill without taking any money, send an empty/omitted `payments` — the bill is created as `UNPAID` with `outstandingAmount = patientPayableAmount`.

### 3.6 What you get back

- The new `VisitBilling` (latest version) with all departments, insurance buckets, items and payments.
- The success `message` tells you if the visit is **not fully billed** — it lists the products still `PENDING`. When every non-deleted product is `BILLED`/`EXEMPTED`, the **visit automatically completes** (`status: COMPLETED`).

---

## 4. Collecting the remaining balance — `recordVisitBillingPayment`

Use this for **later / partial payments** on a bill that already exists (the first payment can also be passed to `billVisit`/`editBillVisit`).

```graphql
mutation Pay($input: RecordVisitBillingPaymentInput!) {
  recordVisitBillingPayment(input: {
    departmentInsuranceBillingId: "…",   // REQUIRED — insuranceBillings[].id from the latest visitBilling
    amount: 5000.0,                      // REQUIRED — must be > 0 and must not exceed the outstanding
    paymentMethod: MOBILE_MONEY,         // REQUIRED
    reference: "mtn-ref-123",            // optional
    note: "partial payment"              // REQUIRED when the payment does NOT cover the full outstanding
  }) { status message data { departments { status paidAmount outstandingAmount } } }
}
```

- Target the **`insuranceBillings[].id`** of the **latest** billing version (paying an old version is rejected: *"Payment must be recorded against the latest billing version."*).
- The note rule mirrors §3.5: **a note is required whenever the payment leaves a balance** (i.e. `paid + amount < patientPayable`).
- Payments are added to the department's `payments[]` and cascade into `paidAmount`/`outstandingAmount`/`status` on both the insurance billing and the department billing.

---

## 5. Correcting billing — `editBillVisit`

**Editing is error correction.** The backend first syncs the visit's products (add/remove/update), then creates a **new billing version** from the corrected state. The old version remains for audit. Payments from the previous version are **carried forward automatically** unless you supply new ones. Invoices of previous versions are **invalidated**.

> Use `editBillVisit` for EVERYTHING that touches a billed visit. The normal product mutations are frozen once a department is billed (see §7).

### Input shape

```json
{
  "visitId": "uuid",
  "departments": [
    {
      "visitDepartmentId": "uuid",       // MUST include EVERY top-level department that has (non-deleted) products
      "addedProducts": [                  // products to add in this correction
        { "productId": "uuid", "quantity": 2.0 }
      ],
      "removedProductIds": ["uuid"],      // products to remove (soft delete, keeps billing history)
      "updatedProducts": [                // quantity corrections on existing products
        { "productId": "uuid", "quantity": 3.0 }
      ],
      "billProducts": [                   // REQUIRED — the full bill set for the NEW version
        {
          "productId": "uuid",            // NOTE: productId (not visitDepartmentProductId) in edit mode
          "quantity": 3.0,
          "unitPrice": 4800.0,
          "patientInsuranceId": "uuid",
          "isExempted": false
        }
      ],
      "payments": [                       // optional — previous payments are carried forward when omitted
        { "amount": 10000.0, "paymentMethod": "CASH", "reference": "…" }
      ],
      "note": "…"                         // same note rules as billVisit
    }
  ]
}
```

### How the parts map to editing scenarios

| Scenario | What to send |
|---|---|
| **Add a new product** | `addedProducts: [{ productId, quantity }]` **and** include it in `billProducts` so it is billed in the new version. |
| **Remove a product** | `removedProductIds: [productId]` and **exclude** it from `billProducts`. The product is soft-deleted (billing history preserved). ⚠️ Each department entry must still bill **at least one** product — you cannot remove the last product of a department (`billProducts` may not end up empty). |
| **Change quantity** | `updatedProducts: [{ productId, quantity }]` and the **same quantity** in the matching `billProducts.quantity` (mismatch → error). |
| **Adjust price** | Only in `billProducts.unitPrice` (snapshot for the new version). No separate "update price" field. |
| **Exempt a product** | `billProducts.isExempted: true` (plus the department `note`). |
| **Change which insurance applies** | `billProducts.patientInsuranceId` (must be linked to the visit and cover the product). |
| **Add a payment / record balance** | `payments: [...]`, or leave it empty to carry the previous payments forward. |

### Rules & gotchas

- **Must include every root department that has (non-deleted) products.** Omitting one → error: *"editBillVisit must include every department that has products…"*. The edit is a complete re-projection of the bill.
- **Quantity conflicts** between `updatedProducts.quantity` and `billProducts.quantity` for the same product → error. Keep them identical.
- **Products with billing history are never hard-deleted.** `removedProductIds` soft-deletes the row (`deleted = true`); historical billing items keep pointing at it.
- **Every department entry must bill ≥ 1 product.** An edit that empties a department (`billProducts` resolves to zero billable products, e.g. removing its last product) is rejected: *"Each department must contain at least one product to bill."* If a department truly has nothing billable left, leave it out of the payload (departments with no active products are not required).
- **Profile products** (`source: PROFILE`) cannot be removed from billing → *"Change the visit department's profile instead."*
- **Overpayment guard:** if the corrected bill is **smaller than what was already paid**, the edit is rejected (*"The corrected bill … is smaller than the amount already paid. Keep the paid product or adjust the payments…"*). There is **no refund mechanism** — keep the paid product or apply a smaller payment set. Corrections are meant to fix errors, not refund money.
- **Carry-forward:** if `payments` is omitted/empty for a department, the previous version's payments (and paid amounts) are carried into the new version. If you *do* send payments, those replace the carried ones.
- **Note rules are the same as `billVisit`**: required when any product is exempted or when payments don't cover the full patient payable.
- A `COMPLETED` visit is reopened (`IN_PROGRESS`) during the correction and re-completes afterwards if fully billed.

---

## 6. What you CANNOT do directly after billing (the freeze)

Once a department is in `BILLING` (and has billing rows) or the visit is `COMPLETED`, these are blocked and **must go through `editBillVisit`**:

| Mutation | Blocked when | Message hint |
|---|---|---|
| `addVisitDepartmentProduct` | department `COMPLETED`/`CANCELLED`, or any `BILLING` status | *"Use editBillVisit to correct the billing."* |
| `updateVisitDepartmentProductQuantity` | product `BILLED`/`EXEMPTED`, or billed department | *"Use editBillVisit…"* |
| `updateVisitDepartmentProductStatus` | billed department; `BILLED`/`EXEMPTED`/`CORRECTION_PENDING` not settable by clients | — |
| `removeVisitDepartmentProduct` | billed department, product with billing history, or `PROFILE` product | — |
| `updateVisitDepartmentStatus` | terminal (`COMPLETED`/`CANCELLED`) and billed-`BILLING` states | — |
| `changeVisitDepartmentProfile` | billed department | — |

Also note: `billVisit` itself is blocked once the visit is billed — only `editBillVisit` can create further versions.

---

## 7. Invoices

**Where they live:** one PDF invoice per `DepartmentInsuranceBilling` (i.e. per insurer bucket within a department). Invoices are **generated on demand** and uploaded to Supabase Storage; the stored object path is what `generateInvoice` signs.

**How to get one (this is also how you fetch an existing one):**

```graphql
mutation {
  generateInvoice(departmentInsuranceBillingId: "…") {
    status
    message
    data { signedUrl }
  }
}
```

- `departmentInsuranceBillingId` = `insuranceBillings[].id` from the latest `visitBilling(visitId)`.
- **Idempotent**: if the invoice already exists, it returns the existing signed URL (*"Invoice already exists."*). Call it again any time to re-fetch.
- The `signedUrl` is **valid for 5 minutes** (300 s). Regenerate by calling `generateInvoice` again — no separate "get invoice" query exists.
- Guarded: only for the **latest billing version**; only after **all visit products are billed**; blocked by unread notes.
- `editBillVisit` **invalidates** invoices of previous versions and the affected insurance billings — regenerate after an edit.

---

## 8. Reading billing data

```graphql
query {
  visitBilling(visitId: "…") { status message data {
    id visitId createdAt updatedAt
    departments {
      id
      visitDepartment { id department { id name } status }
      status totalAmount insuranceCoveredAmount patientPayableAmount paidAmount outstandingAmount
      payments { id amount paymentMethod reference createdAt updatedAt }
      insuranceBillings {
        id
        patientInsurance { id insuranceProviderId insuranceCardNumber principalMemberName }
        status totalAmount insuranceCoveredAmount patientPayableAmount paidAmount outstandingAmount
        items {
          id visitDepartmentProductId productId productName
          unitPriceSnapshot quantitySnapshot insuranceCoveredAmount patientPayableAmount
          createdAt updatedAt
        }
        createdAt updatedAt
      }
      createdAt updatedAt
    }
  } }
}
```

- `visitBilling(visitId)` returns **only the latest version** (the authoritative one).
- Version history (`visitBillings`) exists in the backend service but is **not currently exposed** as a GraphQL query — ask the backend team if you need it for an audit UI.

### Response field notes

- `VisitBillingItem` exposes `unitPriceSnapshot`, `quantitySnapshot`, `insuranceCoveredAmount`, `patientPayableAmount` — note that **`lineTotal` is computed internally but NOT exposed in the GraphQL schema** (frontend can compute it as `unitPriceSnapshot × quantitySnapshot`).
- Money fields are returned as floats with 2 decimals; quantities with 4.

---

## 9. Payment methods

```graphql
enum PaymentMethod { CASH MOBILE_MONEY CARD BANK_TRANSFER CHEQUE MIXED }
```

---

## 10. Roles

| Operation | Roles |
|---|---|
| `billVisit` | ADMIN, CLINIC_ADMIN, RECEPTION, NURSE, CLINICIAN, FINANCE |
| `editBillVisit` | ADMIN, CLINIC_ADMIN, FINANCE |
| `recordVisitBillingPayment` | ADMIN, CLINIC_ADMIN, RECEPTION, CLINICIAN, FINANCE |
| `generateInvoice` | ADMIN, CLINIC_ADMIN, RECEPTION, CLINICIAN, FINANCE |
| `visitBilling` (query) | ADMIN, CLINIC_ADMIN, RECEPTION, NURSE, CLINICIAN, FINANCE |
| `flushSoftDeletedVisitProducts` | ADMIN, CLINIC_ADMIN, FINANCE |

---

## 11. End-to-end recipe (happy path)

1. **Clinician** adds products: `addVisitDepartmentProduct(...)` per product (or via department profile).
2. **Clinician** submits final consultation → departments become `BILLING`, frozen.
3. **Reception/Finance** opens the visit, reads notes, then:
   - build `BillVisitInput` from the visit's departments/products (§3),
   - call `billVisit(input)` — include payments and note if balance/exemptions,
   - visit auto-`COMPLETED` when fully billed.
4. **Later payments:** `recordVisitBillingPayment({ departmentInsuranceBillingId, amount, paymentMethod, … })` — note required if a balance remains.
5. **Invoice:** `generateInvoice(departmentInsuranceBillingId)` → open/download `signedUrl`.
6. **Error found after billing:** fix via `editBillVisit` (§5) — new version, payments carried, invoices invalidated → regenerate invoice.

---

## 12. Error-message cheat sheet (what to show the user)

| Message | Meaning / action |
|---|---|
| *"This visit has already been billed. Use editBillVisit…"* | You called `billVisit` on a billed visit — switch to `editBillVisit`. |
| *"You have unread notes. Please read them before billing/editing/payments/invoice."* | Mark the visit's notes read for the acting user, then retry. |
| *"Payment amount would exceed the patient payable amount."* | Payment input is too large — cap at `outstandingAmount`. |
| *"A billing note is required when items are exempted or the patient payment is less than the payable amount."* | Add `note` to the department entry. |
| *"editBillVisit must include every department that has products. Missing: […]"* | Include ALL root departments with products in the edit payload. |
| *"Quantity mismatch for product '…': updatedProducts.quantity … differs from billProducts.quantity"* | Send the same quantity in both places. |
| *"The corrected bill for … is smaller than the amount already paid."* | No refund path — keep the paid product or adjust the payments. |
| *"… is a profile product and cannot be removed from billing."* | Change the visit department's profile instead. |
| *"Payment must be recorded against the latest billing version."* | Refetch `visitBilling(visitId)` and use the current ids. |
| *"Invoices can only be generated for the latest billing version."* / *"…only be generated after all visit products are billed."* | Refetch; bill the remaining pending products first. |
| *"Invalid billing selection: product '…' is already billed or exempted…"* | Product already billed — correct via `editBillVisit`. |

---

## 13. Maintenance / cleanup

`flushSoftDeletedVisitProducts(visitId)` (ADMIN/CLINIC_ADMIN/FINANCE) hard-deletes soft-deleted product rows that have no billing history — useful after several edits. It returns `{ deletedCount }`.
