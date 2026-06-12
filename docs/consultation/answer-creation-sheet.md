# Consultation Answers Creation Sheet (Backend)

This document defines how backend should create and update consultation answers based on the finalized department form format and current consultation runtime behavior.

## 1. Goal

Store clinician answers in a way that is:
- Strictly tied to the form definition used at submission time.
- Validated by field type.
- Safe for draft save and final submit flows.
- Version-aware so historical answers stay compatible.
- Compatible with dynamic form features used in consultation runtime (conditional rendering, table modes, advanced field types, and action listeners).

## 2. Source of Truth

Use these entities as source of truth:
- Form definition: sections, fields, conditional rules, actions.
- Form status/version: only FINAL forms can be used for final submission.
- Consultation context: consultationId, visitId, patientId, departmentId.

The frontend sends answers as a map keyed by field id:

```json
{
  "answers": {
    "field-uuid-1": "value",
    "field-uuid-2": ["A", "C"],
    "field-uuid-3": { "rows": [["r1c1", "r1c2"]] }
  }
}
```

Current frontend runtime behavior to account for:
- Consultation can render top-level fields and section fields from the same form schema.
- Conditional rendering rules are evaluated in the client before display.
- Table fields can be fixed or variable in shape at runtime.
- Diagnostic and medication field types are submitted as arrays of structured entries.
- Action listener fields submit trigger metadata plus item references.

## 3. Recommended GraphQL Contract

Add these operations in backend schema.

```graphql
input ConsultationAnswersInput {
  consultationId: ID!
  visitId: ID!
  patientId: ID!
  departmentId: ID!
  formId: ID!
  formSchemaVersion: Int!
  status: FormSubmissionStatus!
  answers: String! # JSON string of answers map
  submittedAt: String
}

enum FormSubmissionStatus {
  DRAFT
  FINAL
}

type ConsultationAnswers {
  id: ID!
  consultationId: ID!
  visitId: ID!
  patientId: ID!
  departmentId: ID!
  formId: ID!
  formSchemaVersion: Int!
  status: FormSubmissionStatus!
  answers: String! # JSON
  submittedBy: PublicUser
  submittedAt: String!
  updatedAt: String!
}

type ConsultationAnswersResponse {
  status: Status!
  data: ConsultationAnswers
  messages: [Message]
}

type Mutation {
  createConsultationAnswers(input: ConsultationAnswersInput!): ConsultationAnswersResponse
  updateConsultationAnswers(id: ID!, input: ConsultationAnswersInput!): ConsultationAnswersResponse
}

type Query {
  getConsultationAnswers(consultationId: ID!, departmentId: ID!): ConsultationAnswersResponse
}
```

Note:
- If backend prefers one call, use upsertConsultationAnswers instead of create/update.

## 4. Create vs Update Rules

### Create

Create when no answers record exists for:
- consultationId + departmentId + formId + formSchemaVersion

### Update

Update only when record exists.

If status is FINAL:
- Reject updates unless explicit amendment workflow is supported.
- If amendment is required, create a new revision row and keep old immutable.

## 5. Validation Pipeline (Required)

Run these checks in order.

1. Context checks
- consultationId exists.
- visitId belongs to consultation.
- patientId matches consultation patient.
- departmentId is part of visit/consultation path.

2. Form checks
- formId exists under departmentId.
- formSchemaVersion matches stored finalized form version used by frontend.
- FINAL submission must target a FINAL form.

3. Field integrity checks
- Every answer key must exist in form fields (including section fields).
- Unknown field ids are rejected.
- Required fields must have non-empty value when status=FINAL.

4. Type checks
- Answer shape must match field type contract (section 6).

5. Conditional checks
- If field has conditionalRendering and condition is not met, answer may be ignored or rejected.
- Choose one policy and apply consistently.
- Supported conditions currently used by frontend:
  - `notEmpty`
  - `equals`
  - `checked`
  - `includes`
  - `hasItem` (optionally filtered by `itemType` = `action` or `consumable`)

6. Action consistency checks
- actionListener answers must align with billed/added action entries where applicable.

7. Table shape checks
- Validate rows/columns against field `tableConfig`.
- `mode = fixed`: enforce configured rows and columns.
- `mode = variableRows`: allow rows >= configured base rows.
- `mode = variableColumns`: allow columns >= configured base columns.
- Enforce consistent column width per row (rectangular shape) after normalization.
- Validate headers by placement:
  - `none`: no row/column headers required.
  - `top`: column headers may be present.
  - `left`: row headers may be present.
  - `right`: row headers may be present on the right side.
  - `both`: both top column headers and row headers may be present.

## 6. Answer Type Contract (Canonical)

These are the backend-expected answer shapes by form field type.

### text, email, textarea, date

```json
{ "fieldId": "string" }
```

### number

```json
{ "fieldId": 12.5 }
```

Allow string-number coercion only in DRAFT.
For FINAL, persist as number.

### select, radio

```json
{ "fieldId": "one option value" }
```

Must be one of field.options.

### checkbox

```json
{ "fieldId": ["Option A", "Option C"] }
```

Must be subset of field.options.

### table

```json
{
  "fieldId": {
    "rows": [
      ["c1", "c2", "c3"],
      ["d1", "d2", "d3"]
    ]
  }
}
```

Row cell counts should match configured columns unless table mode allows extension.

Backend normalization recommendation:
- For `variableRows`, preserve all submitted rows.
- For `variableColumns`, preserve max submitted columns and normalize short rows with empty values.
- Persist final normalized table shape alongside answers if helpful for auditing.

### diagnosticRecord

```json
{
  "fieldId": [
    {
      "id": "entry-uuid",
      "diagnosis": "Malaria",
      "description": "Positive RDT"
    }
  ]
}
```

### medicationLongForm

```json
{
  "fieldId": [
    {
      "id": "entry-uuid",
      "name": "Drug",
      "frequency": "Twice daily",
      "amount": "500mg",
      "days": "5",
      "notes": "after meals"
    }
  ]
}
```

### medicationMiniForm

```json
{
  "fieldId": [
    {
      "id": "entry-uuid",
      "name": "Paracetamol",
      "notes": "PRN"
    }
  ]
}
```

### actionListener

```json
{
  "fieldId": {
    "triggered": true,
    "quantity": 2,
    "items": [
      {
        "type": "action",
        "backendId": "visit-action-id",
        "catalogItemId": "action-master-id",
        "name": "X-Ray",
        "quantity": 2
      }
    ]
  }
}
```

Minimum required shape:
- triggered
- quantity

Recommended extension:
- items array for reliable reconciliation with billing records.

## 7. Conditional Rendering Contract

Frontend evaluates conditional rendering before submission visibility, but backend must still validate safely.

Recommended behavior:
- If a field is hidden by condition and has a stale stored answer, either:
  - ignore hidden-field answers, or
  - reject with explicit validation message.
- Apply a single policy consistently across DRAFT and FINAL.

`hasItem` condition semantics:
- `dependsOn` should point to an `actionListener` field.
- Evaluate against `dependsOn.items`.
- If `itemType` provided, filter to matching item type.
- If `value` provided, match item name (case-insensitive contains is acceptable if documented).
- If `value` omitted, condition passes when filtered items count is > 0.

## 8. Storage Model

Recommended table: consultation_form_answers

Columns:
- id (uuid pk)
- consultation_id (uuid)
- visit_id (uuid)
- patient_id (uuid)
- department_id (uuid)
- form_id (uuid)
- form_schema_version (int)
- status (DRAFT | FINAL)
- answers_json (jsonb)
- submitted_by (uuid)
- submitted_at (timestamp)
- updated_at (timestamp)
- revision (int, default 1)

Unique index:
- (consultation_id, department_id, form_id, form_schema_version, revision)

Fast lookup index:
- (consultation_id, department_id, status)

## 9. Draft and Final Workflow

### Save Draft
- Validate minimal structure.
- Allow incomplete required fields.
- Keep previous draft and overwrite same revision or increment revision based on policy.
- Allow relaxed coercion where needed for compatibility:
  - `number`: string-to-number coercion allowed.
  - advanced arrays (`diagnosticRecord`, `medication*`): tolerate partial item drafts only if explicitly supported by policy.

### Final Submit
- Enforce all required/type/conditional rules.
- Lock answer record (immutable) or create next revision only via amendment flow.
- Trigger downstream side effects if needed:
  - billing sync for actionListener
  - audit log entry
- Enforce canonical shapes strictly for advanced field types.

## 10. Error Response Examples

Unknown field:

```json
{
  "status": "ERROR",
  "messages": [{ "type": "VALIDATION", "text": "Unknown field id: f-999" }]
}
```

Wrong type:

```json
{
  "status": "ERROR",
  "messages": [{ "type": "VALIDATION", "text": "Field f-101 expects array of options for checkbox" }]
}
```

Version mismatch:

```json
{
  "status": "ERROR",
  "messages": [{ "type": "CONFLICT", "text": "formSchemaVersion mismatch: expected 4, got 3" }]
}
```

Condition mismatch (example):

```json
{
  "status": "ERROR",
  "messages": [{ "type": "VALIDATION", "text": "Field f-204 is hidden by condition hasItem and cannot be submitted" }]
}
```

Table shape mismatch (example):

```json
{
  "status": "ERROR",
  "messages": [{ "type": "VALIDATION", "text": "Field f-301 table row 2 has 4 cells, expected 3" }]
}
```

## 11. Frontend Compatibility Note

Current consultation UI may send simpler values for some advanced types in draft mode.
Backend should support this transition safely:
- Accept relaxed shape for DRAFT.
- Normalize server-side to canonical shape where possible.
- Require canonical shape for FINAL.

Current frontend now submits canonical structured arrays for:
- `diagnosticRecord`
- `medicationLongForm`
- `medicationMiniForm`

Current frontend table behavior:
- preserves dynamic shape (rows/columns) for variable modes.
- includes values as `fieldId.rows[][]` in payload.

## 12. Minimal Implementation Checklist

- Add create/update (or upsert) answers mutation.
- Add get answers query by consultationId + departmentId.
- Implement validation against form schema and version.
- Persist answers as jsonb with status and audit metadata.
- Enforce FINAL immutability or revision strategy.
- Add integration tests for every field type shape.
- Add integration tests for conditional `hasItem` behavior.
- Add integration tests for `tableConfig.mode` and `headerPlacement` variants.
