# Department Form — Backend Format Guide

This document defines the canonical JSON payload the frontend sends when creating or updating a department form definition. The backend stores this payload (or a normalized version thereof) in Postgres and uses it to render forms for clinicians.

---

## Table of Contents

1. [Top-Level Payload](#1-top-level-payload)
2. [Field Object](#2-field-object)
   - 2.1 [Common Properties](#21-common-properties)
   - 2.2 [Field Types Reference](#22-field-types-reference)
   - 2.3 [Type-Specific Rules](#23-type-specific-rules)
3. [Section Object](#3-section-object)
4. [Action Object](#4-action-object)
5. [Conditional Rendering](#5-conditional-rendering)
6. [Table Config](#6-table-config)
7. [Submission Answers Payload](#7-submission-answers-payload)
   - 7.1 [Per-Type Answer Shape](#71-per-type-answer-shape)
8. [Status Lifecycle](#8-status-lifecycle)
9. [Versioning Rules](#9-versioning-rules)
10. [Full Example](#10-full-example)

---

## 1. Top-Level Payload

Sent as the body of `POST /api/forms` (create) or `PUT /api/forms/{formId}` (update).

```json
{
  "departmentId": "dept-uuid-here",
  "title": "General Outpatient Consultation Form",
  "description": "Standard form used for all GP consultations.",
  "status": "draft",
  "schemaVersion": 1,
  "sections": [ /* Section[] — see §3 */ ],
  "fields": [ /* FormField[] — top-level / unsectioned fields — see §2 */ ],
  "actions": [ /* FormAction[] — see §4 */ ],
  "meta": {
    "clientGeneratedAt": "2026-03-12T08:00:00.000Z",
    "clientSchemaHash": "sha256-optional-fingerprint"
  }
}
```

| Property | Type | Required | Notes |
|---|---|---|---|
| `departmentId` | `string` (UUID) | ✅ | Must reference an existing department. |
| `title` | `string` | ✅ | Max 255 chars. |
| `description` | `string` | ❌ | Free-text summary. |
| `status` | `"draft" \| "published" \| "archived"` | ✅ | Only `"draft"` is accepted on create. Use a separate publish endpoint to promote. |
| `schemaVersion` | `integer` | ✅ | Monotonically incrementing. Frontend sends previous value + 1 on update. Backend rejects if the stored version doesn't match `schemaVersion - 1`. |
| `sections` | `Section[]` | ✅ | Can be empty array. |
| `fields` | `FormField[]` | ✅ | Top-level fields not inside any section. Can be empty array. |
| `actions` | `FormAction[]` | ✅ | Billable actions/consumables linked to this form. Can be empty array. |
| `meta` | `object` | ❌ | Client diagnostics only — not used by backend logic. |

> **Note:** Either `sections` or `fields` (or both) may contain items. A form may have both sectioned and unsectioned fields.

---

## 2. Field Object

### 2.1 Common Properties

Every field object shares these properties regardless of type:

```json
{
  "id": "field-uuid",
  "label": "Chief Complaint",
  "type": "textarea",
  "placeholder": "Describe the patient's complaint...",
  "required": true,
  "order": 1,
  "hideLabel": false,
  "boldLabel": false,
  "italicLabel": false,
  "underlineLabel": false,
  "centerLabel": false,
  "conditionalRendering": null
}
```

| Property | Type | Required | Notes |
|---|---|---|---|
| `id` | `string` (UUID) | ✅ | Client-generated stable ID. |
| `label` | `string` | ✅ | Display label. |
| `type` | `FieldType` (see §2.2) | ✅ | Determines which extra properties are valid. |
| `placeholder` | `string` | ❌ | Hint text inside the input. |
| `required` | `boolean` | ✅ | Whether the field must be filled before form submission. |
| `order` | `integer` | ✅ | 1-based render order within its container (section or top-level). |
| `hideLabel` | `boolean` | ❌ | Default `false`. |
| `boldLabel` | `boolean` | ❌ | Default `false`. |
| `italicLabel` | `boolean` | ❌ | Default `false`. |
| `underlineLabel` | `boolean` | ❌ | Default `false`. |
| `centerLabel` | `boolean` | ❌ | Default `false`. |
| `conditionalRendering` | `ConditionalRendering \| null` | ❌ | See §5. |

---

### 2.2 Field Types Reference

| Type Key | UI Component | Extra Properties |
|---|---|---|
| `text` | Single-line text input | — |
| `email` | Email input (with format validation) | — |
| `number` | Numeric input | — |
| `textarea` | Multi-line text area | — |
| `date` | Date picker | — |
| `select` | Single-choice dropdown | `options` |
| `radio` | Radio button group | `options` |
| `checkbox` | Multi-select checkboxes | `options` |
| `table` | Grid / spreadsheet | `tableConfig` |
| `diagnosticRecord` | Add-diagnosis list widget | — |
| `medicationLongForm` | Full medication entry list | — |
| `medicationMiniForm` | Lightweight medication entry list | — |
| `actionListener` | Hidden trigger for billable actions | — |

---

### 2.3 Type-Specific Rules

#### `select` / `radio` / `checkbox`

Must include an `options` array of non-empty strings:

```json
{
  "type": "select",
  "options": ["Male", "Female", "Other", "Prefer not to say"]
}
```

#### `table`

Must include a `tableConfig` object (see §6).

#### `diagnosticRecord`

No extra properties needed. The answer (see §7) is a list of diagnosis entries the clinician builds at runtime.

```json
{ "type": "diagnosticRecord" }
```

#### `medicationLongForm`

No extra properties needed. Each entry captures: name, frequency, amount, days, and optional notes.

```json
{ "type": "medicationLongForm" }
```

#### `medicationMiniForm`

No extra properties needed. Each entry captures: name and optional notes.

```json
{ "type": "medicationMiniForm" }
```

#### `actionListener`

Connects this field to a `FormAction`. The clinician checks or triggers this field to add a billable action to the encounter's bill.

```json
{ "type": "actionListener" }
```

---

## 3. Section Object

Sections group fields into visually distinct panels with customisable column layouts.

```json
{
  "id": "section-uuid",
  "title": "Patient History",
  "boldTitle": true,
  "italicTitle": false,
  "underlineTitle": false,
  "centerTitle": false,
  "columns": 2,
  "order": 1,
  "fields": [ /* FormField[] */ ]
}
```

| Property | Type | Required | Notes |
|---|---|---|---|
| `id` | `string` (UUID) | ✅ | Client-generated stable ID. |
| `title` | `string` | ✅ | Section heading. |
| `boldTitle` | `boolean` | ❌ | Default `false`. |
| `italicTitle` | `boolean` | ❌ | Default `false`. |
| `underlineTitle` | `boolean` | ❌ | Default `false`. |
| `centerTitle` | `boolean` | ❌ | Default `false`. |
| `columns` | `1 \| 2 \| 3 \| 4` | ✅ | Number of columns the fields render in. |
| `order` | `integer` | ✅ | 1-based render order relative to other sections and top-level fields. |
| `fields` | `FormField[]` | ✅ | Can be empty array. |

---

## 4. Action Object

Links a billable item (action or consumable) to the form. When an `actionListener` field is triggered by the clinician, the backend adds the linked action to the encounter's bill.

```json
{
  "id": "action-uuid",
  "name": "Malaria RDT",
  "type": "consumable",
  "quantity": 1,
  "price": 500,
  "isQuantifiable": false,
  "backendId": "ITEM-00123"
}
```

| Property | Type | Required | Notes |
|---|---|---|---|
| `id` | `string` (UUID) | ✅ | Client-generated. |
| `name` | `string` | ✅ | Display name. |
| `type` | `"action" \| "consumable"` | ✅ | Determines billing category. |
| `quantity` | `integer` | ✅ | Default quantity when triggered. Min `1`. |
| `price` | `number` | ✅ | Unit price in the facility's base currency. |
| `isQuantifiable` | `boolean` | ❌ | If `true`, clinician can adjust quantity at submission time. |
| `backendId` | `string` | ❌ | ID of the corresponding item in the billing/inventory system. |

---

## 5. Conditional Rendering

A field can be shown or hidden based on the value of another field in the same form.

```json
{
  "conditionalRendering": {
    "dependsOn": "field-uuid-of-trigger-field",
    "condition": "equals",
    "value": "Yes"
  }
}
```

| Property | Type | Required | Notes |
|---|---|---|---|
| `dependsOn` | `string` (field UUID) | ✅ | The field whose value drives visibility. Must resolve to a field in the same form. |
| `condition` | `ConditionType` | ✅ | See condition types below. |
| `value` | `string` | Conditional | Required for `equals`, `includes`, `hasItem`. |
| `itemType` | `"action" \| "consumable"` | Conditional | Required only when `condition = "hasItem"`. |

**Condition types:**

| Condition | Meaning |
|---|---|
| `notEmpty` | Show when the trigger field has any non-empty value. |
| `equals` | Show when the trigger field value exactly matches `value`. |
| `checked` | Show when the trigger field (checkbox/radio) is checked/selected. |
| `includes` | Show when the trigger field value contains the string `value`. |
| `hasItem` | Show when an `actionListener` has a specific action/consumable added. |

---

## 6. Table Config

Required when `type = "table"`.

```json
{
  "tableConfig": {
    "mode": "variableRows",
    "rows": 3,
    "columns": 4,
    "headerPlacement": "top",
    "columnHeaders": ["Test", "Result", "Unit", "Reference Range"],
    "rowHeaders": []
  }
}
```

| Property | Type | Required | Notes |
|---|---|---|---|
| `mode` | `"fixed" \| "variableRows" \| "variableColumns"` | ✅ | `fixed` — locked grid; `variableRows` — clinician can add rows; `variableColumns` — clinician can add columns. |
| `rows` | `integer` | ✅ | Initial/fixed row count. Min `1`. |
| `columns` | `integer` | ✅ | Initial/fixed column count. Min `1`. |
| `headerPlacement` | `"none" \| "top" \| "left" \| "right" \| "both"` | ✅ | Where header labels appear. |
| `columnHeaders` | `string[]` | ❌ | Required when `headerPlacement` is `"top"` or `"both"`. Length must equal `columns`. |
| `rowHeaders` | `string[]` | ❌ | Required when `headerPlacement` is `"left"`, `"right"`, or `"both"`. Length must equal `rows`. |

---

## 7. Submission Answers Payload

When a clinician submits a filled form, the frontend sends:

```json
{
  "formId": "form-uuid",
  "formSchemaVersion": 3,
  "visitId": "visit-uuid",
  "patientId": "patient-uuid",
  "submittedBy": "user-uuid",
  "submittedAt": "2026-03-12T10:30:00.000Z",
  "answers": {
    "<fieldId>": <AnswerValue>
  }
}
```

The `answers` map uses field IDs as keys. The value type depends on the field type.

### 7.1 Per-Type Answer Shape

#### `text` / `email` / `number` / `textarea` / `date`
```json
{ "field-uuid": "string or number value" }
```

#### `select` / `radio`
```json
{ "field-uuid": "selected option string" }
```

#### `checkbox`
```json
{ "field-uuid": ["Option A", "Option C"] }
```

#### `table`
```json
{
  "field-uuid": {
    "rows": [
      ["CBC", "12.5", "g/dL", "11.5–16.5"],
      ["WBC", "7.2", "×10³/μL", "4.5–11.0"]
    ]
  }
}
```
Each inner array maps 1-to-1 with table columns.

#### `diagnosticRecord`
```json
{
  "field-uuid": [
    {
      "id": "entry-uuid",
      "diagnosis": "Malaria, Uncomplicated",
      "description": "Positive RDT, started Artemether-Lumefantrine"
    },
    {
      "id": "entry-uuid-2",
      "diagnosis": "Anaemia",
      "description": null
    }
  ]
}
```

| Property | Type | Required |
|---|---|---|
| `id` | `string` (UUID) | ✅ — client-generated |
| `diagnosis` | `string` | ✅ |
| `description` | `string \| null` | ❌ |

#### `medicationLongForm`
```json
{
  "field-uuid": [
    {
      "id": "entry-uuid",
      "name": "Artemether-Lumefantrine",
      "frequency": "Twice daily",
      "amount": "80/480mg",
      "days": "3",
      "notes": "Take with food"
    }
  ]
}
```

| Property | Type | Required |
|---|---|---|
| `id` | `string` (UUID) | ✅ |
| `name` | `string` | ✅ |
| `frequency` | `string` | ✅ |
| `amount` | `string` | ✅ |
| `days` | `string` | ✅ |
| `notes` | `string \| null` | ❌ |

#### `medicationMiniForm`
```json
{
  "field-uuid": [
    {
      "id": "entry-uuid",
      "name": "Paracetamol 500mg",
      "notes": "PRN for pain"
    }
  ]
}
```

| Property | Type | Required |
|---|---|---|
| `id` | `string` (UUID) | ✅ |
| `name` | `string` | ✅ |
| `notes` | `string \| null` | ❌ |

#### `actionListener`
```json
{
  "field-uuid": {
    "triggered": true,
    "quantity": 2
  }
}
```

| Property | Type | Notes |
|---|---|---|
| `triggered` | `boolean` | `true` if the clinician activated this action. |
| `quantity` | `integer` | Final quantity (clinician may have adjusted if `isQuantifiable = true`). |

---

## 8. Status Lifecycle

```
draft ──► published ──► archived
  ▲            │
  └────────────┘ (re-draft is not allowed; create a new version)
```

| Status | Meaning |
|---|---|
| `draft` | Editable. Not visible to clinicians. |
| `published` | Locked. Active and visible to clinicians. |
| `archived` | Locked. No longer visible. Retained for historical submissions. |

**Publish rule:** Only one form per department may be `published` at a time. Publishing a new draft automatically archives the previous published form in the same transaction.

---

## 9. Versioning Rules

- `schemaVersion` starts at `1` on first save.
- Every `PUT` must include `schemaVersion = currentStoredVersion + 1`.
- Backend rejects (`409 Conflict`) if versions don't match (optimistic concurrency).
- Once `published`, a form **cannot** be mutated. A new draft must be created if changes are needed.

---

## 10. Full Example

A complete payload for a two-section consultation form:

```json
{
  "departmentId": "3f2a1bc4-0000-0000-0000-000000000001",
  "title": "General Outpatient Consultation",
  "description": "Standard GP form covering chief complaint, history, examination findings, diagnosis, and medications.",
  "status": "draft",
  "schemaVersion": 1,
  "fields": [],
  "sections": [
    {
      "id": "sec-0001",
      "title": "Chief Complaint & History",
      "boldTitle": true,
      "italicTitle": false,
      "underlineTitle": false,
      "centerTitle": false,
      "columns": 1,
      "order": 1,
      "fields": [
        {
          "id": "f-0001",
          "label": "Chief Complaint",
          "type": "textarea",
          "placeholder": "Describe the patient's main complaint...",
          "required": true,
          "order": 1,
          "hideLabel": false,
          "boldLabel": false,
          "italicLabel": false,
          "underlineLabel": false,
          "centerLabel": false,
          "conditionalRendering": null
        },
        {
          "id": "f-0002",
          "label": "Duration of Symptoms",
          "type": "text",
          "placeholder": "e.g. 3 days",
          "required": true,
          "order": 2,
          "conditionalRendering": null
        }
      ]
    },
    {
      "id": "sec-0002",
      "title": "Diagnosis & Treatment",
      "boldTitle": true,
      "italicTitle": false,
      "underlineTitle": false,
      "centerTitle": false,
      "columns": 1,
      "order": 2,
      "fields": [
        {
          "id": "f-0003",
          "label": "Diagnoses",
          "type": "diagnosticRecord",
          "required": false,
          "order": 1,
          "conditionalRendering": null
        },
        {
          "id": "f-0004",
          "label": "Prescribed Medications",
          "type": "medicationLongForm",
          "required": false,
          "order": 2,
          "conditionalRendering": null
        },
        {
          "id": "f-0005",
          "label": "Malaria RDT",
          "type": "actionListener",
          "required": false,
          "order": 3,
          "conditionalRendering": null
        }
      ]
    }
  ],
  "actions": [
    {
      "id": "act-0001",
      "name": "Malaria RDT",
      "type": "consumable",
      "quantity": 1,
      "price": 500,
      "isQuantifiable": false,
      "backendId": "ITEM-00123"
    }
  ],
  "meta": {
    "clientGeneratedAt": "2026-03-12T08:00:00.000Z"
  }
}
```
