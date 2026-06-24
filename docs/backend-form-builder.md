# Backend Guide: Standalone Form Builder Persistence

This document outlines the requirements and recommended implementation for persisting standalone form structures and their answers in the backend. These forms are independent and not tied to departments by default.

## 1. Technology Stack
- **Language/Framework:** Spring Boot (Java/Kotlin)
- **Database:** PostgreSQL
- **API:** GraphQL (aligning with existing project patterns)

## 2. Database Schema (PostgreSQL)

We recommend using a relational structure with JSONB fields for the flexible block-based content.

### Table: `forms`
Stores the high-level metadata of a form.
```sql
CREATE TABLE forms (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name TEXT NOT NULL,
    description TEXT,
    type TEXT NOT NULL, -- e.g., 'consultation', 'consent', 'referral', 'custom'
    category TEXT,
    is_template BOOLEAN DEFAULT FALSE,
    is_deleted BOOLEAN DEFAULT FALSE,
    created_by UUID, -- Worker ID
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);
```

### Table: `form_versions`
Stores the actual structure of the form at a specific version.
```sql
CREATE TABLE form_versions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    form_id UUID NOT NULL REFERENCES forms(id) ON DELETE CASCADE,
    major_version INTEGER NOT NULL DEFAULT 0,
    minor_version INTEGER NOT NULL DEFAULT 0,
    version_label TEXT NOT NULL, -- Generated: major.minor (e.g., "0.5")
    blocks JSONB NOT NULL, -- Array of FormBlock objects
    theme JSONB, -- FormTheme object
    status TEXT NOT NULL DEFAULT 'DRAFT', -- 'DRAFT', 'FINAL'
    created_at TIMESTAMPTZ DEFAULT NOW(),
    UNIQUE(form_id, major_version, minor_version)
);
```

### Table: `form_answers`
Stores user-submitted answers. Each answer set is linked to a specific version of the form for traceability.
```sql
CREATE TABLE form_answers (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    form_version_id UUID NOT NULL REFERENCES form_versions(id),
    patient_id UUID, -- Optional patient link
    visit_id UUID,   -- Optional visit link
    answers JSONB NOT NULL, -- Key-value map: { "block_id": "value" }
    score NUMERIC,   -- Optional calculated score (e.g., for medical assessments)
    status TEXT NOT NULL DEFAULT 'DRAFT', -- 'DRAFT', 'FINAL'
    submitted_by UUID, -- Worker ID who filled the form
    submitted_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);
```

## 3. Versioning Logic

The system follows a specific `major.minor` versioning rule:

1.  **Initial Version:** A new form starts at version `0.0` (DRAFT).
2.  **Saving Drafts:** While a version is in `DRAFT` status, successive saves overwrite the same version record.
3.  **Finalizing:** When a form is marked as `FINAL`, the current version status is updated.
4.  **Updating Final Forms:** If an update is made to a `FINAL` version, a **new version record** is created.
5.  **Increment Rules:**
    *   The `minor` version increments by 1 (e.g., `0.0` -> `0.1`).
    *   The `minor` version range is `0` to `10`.
    *   When an update occurs on a version where `minor = 10`, the `major` version increments and `minor` resets to `0` (e.g., `0.10` -> `1.0`).

## 4. GraphQL Schema (Standalone)

Align operations with the existing `ApiResponse` and `JSON` scalar patterns found in `user.graphqls`.

### Types
```graphql
type StandaloneForm {
  id: ID!
  name: String!
  description: String
  type: String!
  category: String
  isTemplate: Boolean!
  createdBy: ID
  latestVersion: StandaloneFormVersion
  createdAt: String!
  updatedAt: String!
}

type StandaloneFormVersion {
  id: ID!
  formId: ID!
  versionLabel: String!
  majorVersion: Int!
  minorVersion: Int!
  blocks: JSON!
  theme: JSON
  status: FormStatus!
  createdAt: String!
}

type StandaloneFormAnswer {
  id: ID!
  formVersion: StandaloneFormVersion!
  answers: JSON!
  score: Float
  status: AnswerStatus!
  patientId: ID
  visitId: ID
  submittedBy: ID
  submittedAt: String
  createdAt: String!
  updatedAt: String!
}

# Response types following project patterns
type StandaloneFormResponse implements ApiResponse {
  status: ResponseStatus!
  message: String
  data: StandaloneForm
}

type StandaloneFormListResponse implements ApiResponse {
  status: ResponseStatus!
  message: String
  data: [StandaloneForm!]
}

type StandaloneFormAnswerResponse implements ApiResponse {
  status: ResponseStatus!
  message: String
  data: StandaloneFormAnswer
}
```

### Operations
```graphql
extend type Query {
  # Form CRUD
  getStandaloneForms(isTemplate: Boolean, category: String): StandaloneFormListResponse!
  getStandaloneForm(id: ID!): StandaloneFormResponse!
  getStandaloneFormVersion(versionId: ID!): StandaloneFormVersion # For historical rendering
  
  # Answer CRUD
  getStandaloneAnswers(formId: ID, patientId: ID): [StandaloneFormAnswer!]!
  getStandaloneAnswer(id: ID!): StandaloneFormAnswerResponse!
}

extend type Mutation {
  # Form Management
  createStandaloneForm(input: StandaloneFormInput!): StandaloneFormResponse!
  updateStandaloneForm(id: ID!, input: StandaloneFormInput!, markFinal: Boolean): StandaloneFormResponse!
  duplicateStandaloneForm(sourceFormId: ID!): StandaloneFormResponse!
  deleteStandaloneForm(id: ID!, confirmDeleteAnswers: Boolean): BooleanResponse!

  # Answer Management
  saveStandaloneAnswer(formVersionId: ID!, answers: JSON!, status: AnswerStatus, score: Float): StandaloneFormAnswerResponse!
  updateStandaloneAnswer(answerId: ID!, answers: JSON!, status: AnswerStatus, score: Float): StandaloneFormAnswerResponse!
  deleteStandaloneAnswer(answerId: ID!): BooleanResponse!
  generateStandaloneFormPdf(answerId: ID!): StringResponse! # Returns signed URL
}
```

## 5. Key Features & Business Rules

### Standalone Nature
These forms must be manageable and answerable without any required link to a `departmentId`. They should be fetchable as a list and updated directly.

### Templates
- Any form can be marked as a template (`is_template = true`).
- A "Start from template" or "Start from existing form" feature is implemented via the `duplicateStandaloneForm` mutation, which clones the latest `FINAL` version of a source form into a new form (v0.0).

### Deletion Safety
- **Restriction:** You **cannot** delete a form version if it has any associated answers.
- **Override:** If a user wants to delete a form/version that has answers, they must explicitly provide a confirmation flag (e.g., `confirmDeleteAnswers: true`). Otherwise, the backend should return an error.

### Traceability
Each `StandaloneFormAnswer` record must reference a specific `form_version_id`. This ensures that even if the form is updated to a newer version later, the original answer remains associated with the exact structure (blocks) that the user saw when they filled it.

## 6. Implementation Notes for Backend
- Store `blocks` as `JSONB` to allow for varying field types without schema changes.
- Ensure `updated_at` on the `forms` table is updated whenever a new version is created or the name/description is changed.
- The `duplicateStandaloneForm` should deep-copy the `blocks` and `theme` from the source's latest version.

## 7. Advanced Recommendations

### Assessment Scoring
For forms used as medical assessments (e.g., GAD-7, PHQ-9), the backend should support a `score` field. 
- **Frontend Calculation:** The frontend calculates the score for immediate feedback.
- **Backend Verification:** The backend should ideally re-verify the score based on the submitted answers to ensure data integrity before saving.

### Backend Validation
Since `answers` is a `JSONB` field, it's highly recommended to implement backend-side validation:
1.  **Structure Check:** Ensure every `block_id` in the answers exists in the referenced `form_version`.
2.  **Required Fields:** Verify that fields marked as `required: true` in the form structure are present in the answers.
3.  **Type Safety:** If a field is a `number`, ensure the answer is actually a number.

### PDF Generation
Medical records often require a printable format. 
- Implement an endpoint that takes a `StandaloneFormAnswer` and its corresponding `StandaloneFormVersion`.
- Use a template engine (like Thymeleaf) or a PDF library to generate a document that mirrors the form layout with the user's answers.

### Audit Trails
For compliance (e.g., HIPAA), track not just who created the form, but every change made to it.
- Consider a `form_audit_logs` table.
- Log `action` (CREATE, UPDATE, DELETE, FINALIZE), `actor_id`, and `timestamp`.

### Optimistic Locking
To prevent concurrent edits from overwriting each other, add a `version` (integer) column to the `forms` table and use optimistic locking in Spring Boot (`@Version`).

### Role-Based Access Control (RBAC)
If certain forms should only be accessible to specific roles (e.g., only `CLINICIAN` can see 'Consultation' forms):
- Add a `restricted_to_roles` (TEXT array) column to the `forms` table.
- Filter the results in `getStandaloneForms` based on the current user's roles.
