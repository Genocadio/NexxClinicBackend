# Department Forms GraphQL Schema Documentation

## Overview
This document defines the complete GraphQL schema and implementation requirements for department forms functionality in the NexxClinic system.

## 1. Form Management GraphQL Operations

### 1.1 Queries

#### Get Forms for Department
```graphql
query GetFormsForDepartment($departmentId: ID!) {
  getForms(departmentId: $departmentId) {
    status
    messages {
      text
      type
    }
    data {
      id
      departmentId
      title
      status
      currentVersionNumber
      createdAt
      updatedAt
    }
  }
}
```

#### Get Single Form
```graphql
query GetForm($departmentId: ID!, $formId: ID!) {
  getForm(departmentId: $departmentId, formId: $formId) {
    status
    messages {
      text
      type
    }
    data {
      id
      departmentId
      title
      description
      status
      currentVersionNumber
      currentSchemaVersion
      createdAt
      updatedAt
      sections {
        id
        title
        boldTitle
        italicTitle
        underlineTitle
        centerTitle
        columns
        order
        fields {
          id
          label
          type
          placeholder
          required
          order
          hideLabel
          boldLabel
          italicLabel
          underlineLabel
          centerLabel
          options
          tableConfig {
            mode
            rows
            columns
            headerPlacement
            columnHeaders
            rowHeaders
          }
          conditionalRendering {
            dependsOn
            condition
            value
            itemType
          }
        }
      }
      fields {
        id
        label
        type
        placeholder
        required
        order
        hideLabel
        boldLabel
        italicLabel
        underlineLabel
        centerLabel
        options
        tableConfig {
          mode
          rows
          columns
          headerPlacement
          columnHeaders
          rowHeaders
        }
        conditionalRendering {
          dependsOn
          condition
          value
          itemType
        }
      }
      actions {
        id
        name
        type
        quantity
        price
        isQuantifiable
        backendId
      }
    }
  }
}
```

#### Get Form Version History
```graphql
query GetFormVersionHistory($departmentId: ID!, $formId: ID!) {
  getFormVersionHistory(departmentId: $departmentId, formId: $formId) {
    status
    messages {
      text
      type
    }
    data {
      id
      versionNumber
      status
      createdAt
    }
  }
}
```

#### Get Consultation Answers
```graphql
query ConsultationGetAnswers($consultationId: ID!, $departmentId: ID!, $formId: ID!) {
  getConsultationAnswers(consultationId: $consultationId, departmentId: $departmentId, formId: $formId) {
    status
    data {
      id
      consultationId
      visitId
      patientId
      departmentId
      formId
      formSchemaVersion
      status
      answers
      submittedAt
      updatedAt
    }
    messages {
      text
      type
    }
  }
}
```

### 1.2 Mutations

#### Create Form
```graphql
mutation CreateForm($departmentId: ID!, $input: FormInput!) {
  createForm(departmentId: $departmentId, input: $input) {
    status
    messages {
      text
      type
    }
    data {
      # Same structure as GetForm query
      ...FormBuilderData
    }
  }
}
```

#### Update Form
```graphql
mutation UpdateForm($departmentId: ID!, $formId: ID!, $input: FormInput!) {
  updateForm(departmentId: $departmentId, formId: $formId, input: $input) {
    status
    messages {
      text
      type
    }
    data {
      # Same structure as GetForm query
      ...FormBuilderData
    }
  }
}
```

#### Finalize Form
```graphql
mutation FinalizeForm($departmentId: ID!, $formId: ID!) {
  finalizeForm(departmentId: $departmentId, formId: $formId) {
    status
    messages {
      text
      type
    }
    data {
      # Same structure as GetForm query
      ...FormBuilderData
    }
  }
}
```

#### Upsert Consultation Answers
```graphql
mutation UpsertConsultationAnswers($input: ConsultationAnswersInput!) {
  upsertConsultationAnswers(input: $input) {
    status
    data {
      id
      consultationId
      visitId
      patientId
      departmentId
      formId
      formSchemaVersion
      status
      answers
      submittedAt
      updatedAt
    }
    messages {
      text
      type
    }
  }
}
```

#### Generate Consultation PDF
```graphql
mutation GenerateConsultationPdf($consultationId: ID!, $departmentId: ID!, $formId: ID!) {
  generateConsultationPdf(consultationId: $consultationId, departmentId: $departmentId, formId: $formId) {
    status
    pdfBase64
    messages {
      text
      type
    }
  }
}
```

## 2. Required Backend Schema Types

### 2.1 Form Types

```graphql
enum FormStatus {
  DRAFT
  FINAL
}

enum FieldType {
  text
  email
  number
  date
  textarea
  select
  radio
  checkbox
  table
  signature
  file
  heading
  paragraph
}

enum TableMode {
  static
  dynamic
}

enum ConditionalCondition {
  equals
  not_equals
  contains
  not_contains
  greater_than
  less_than
  is_empty
  is_not_empty
}

type ConditionalRendering {
  dependsOn: String!
  condition: ConditionalCondition!
  value: String
  itemType: String
}

type TableConfig {
  mode: TableMode!
  rows: Int
  columns: Int
  headerPlacement: String
  columnHeaders: [String]
  rowHeaders: [String]
}

type FormField {
  id: String!
  label: String!
  type: FieldType!
  placeholder: String
  required: Boolean!
  order: Int!
  hideLabel: Boolean!
  boldLabel: Boolean!
  italicLabel: Boolean!
  underlineLabel: Boolean!
  centerLabel: Boolean!
  options: [String]
  tableConfig: TableConfig
  conditionalRendering: ConditionalRendering
}

type FormSection {
  id: String!
  title: String!
  boldTitle: Boolean!
  italicTitle: Boolean!
  underlineTitle: Boolean!
  centerTitle: Boolean!
  columns: Int!
  order: Int!
  fields: [FormField]
}

type FormAction {
  id: String!
  name: String!
  type: String!
  quantity: Int!
  price: Float!
  isQuantifiable: Boolean!
  backendId: String
}

type Form {
  id: ID!
  departmentId: ID!
  title: String!
  description: String
  status: FormStatus!
  currentVersionNumber: String!
  currentSchemaVersion: Int!
  createdAt: String!
  updatedAt: String!
  sections: [FormSection]
  fields: [FormField]
  actions: [FormAction]
}

type FormVersion {
  id: ID!
  formId: ID!
  versionNumber: String!
  status: FormStatus!
  createdAt: String!
}
```

### 2.2 Input Types

```graphql
input FormInput {
  title: String!
  description: String
  schemaVersion: Int!
  fields: [FormFieldInput]
  sections: [FormSectionInput]
  actions: [FormActionInput]
}

input FormFieldInput {
  id: String!
  label: String!
  type: FieldType!
  placeholder: String
  required: Boolean!
  order: Int!
  hideLabel: Boolean!
  boldLabel: Boolean!
  italicLabel: Boolean!
  underlineLabel: Boolean!
  centerLabel: Boolean!
  options: [String]
  tableConfig: TableConfigInput
  conditionalRendering: ConditionalRenderingInput
}

input FormSectionInput {
  id: String!
  title: String!
  boldTitle: Boolean!
  italicTitle: Boolean!
  underlineTitle: Boolean!
  centerTitle: Boolean!
  columns: Int!
  order: Int!
  fields: [FormFieldInput]
}

input FormActionInput {
  id: String!
  name: String!
  type: String!
  quantity: Int!
  price: Float!
  isQuantifiable: Boolean!
  backendId: String
}

input TableConfigInput {
  mode: TableMode!
  rows: Int
  columns: Int
  headerPlacement: String
  columnHeaders: [String]
  rowHeaders: [String]
}

input ConditionalRenderingInput {
  dependsOn: String!
  condition: ConditionalCondition!
  value: String
  itemType: String
}
```

### 2.3 Consultation Answers Types

```graphql
enum AnswerStatus {
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
  formSchemaVersion: String!
  status: AnswerStatus!
  answers: String!  # JSON string of form answers
  submittedAt: String
  updatedAt: String!
}

input ConsultationAnswersInput {
  consultationId: ID!
  visitId: ID!
  patientId: ID!
  departmentId: ID!
  formId: ID!
  formSchemaVersion: String!
  status: AnswerStatus!
  answers: String!  # JSON string of form answers
}
```

## 3. Database Schema Requirements

### 3.1 Forms Table
```sql
CREATE TABLE forms (
  id VARCHAR(255) PRIMARY KEY,
  department_id VARCHAR(255) NOT NULL,
  title VARCHAR(500) NOT NULL,
  description TEXT,
  status ENUM('DRAFT', 'FINAL') NOT NULL DEFAULT 'DRAFT',
  current_version_number VARCHAR(50) NOT NULL,
  current_schema_version INT NOT NULL DEFAULT 1,
  form_data JSON NOT NULL,  # Contains fields, sections, actions
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  
  INDEX idx_department_id (department_id),
  INDEX idx_status (status),
  INDEX idx_created_at (created_at)
);
```

### 3.2 Form Versions Table
```sql
CREATE TABLE form_versions (
  id VARCHAR(255) PRIMARY KEY,
  form_id VARCHAR(255) NOT NULL,
  version_number VARCHAR(50) NOT NULL,
  status ENUM('DRAFT', 'FINAL') NOT NULL,
  form_data JSON NOT NULL,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  
  FOREIGN KEY (form_id) REFERENCES forms(id) ON DELETE CASCADE,
  INDEX idx_form_id (form_id),
  INDEX idx_version_number (version_number),
  UNIQUE KEY unique_form_version (form_id, version_number)
);
```

### 3.3 Consultation Answers Table
```sql
CREATE TABLE consultation_answers (
  id VARCHAR(255) PRIMARY KEY,
  consultation_id VARCHAR(255) NOT NULL,
  visit_id VARCHAR(255) NOT NULL,
  patient_id VARCHAR(255) NOT NULL,
  department_id VARCHAR(255) NOT NULL,
  form_id VARCHAR(255) NOT NULL,
  form_schema_version VARCHAR(50) NOT NULL,
  status ENUM('DRAFT', 'FINAL') NOT NULL DEFAULT 'DRAFT',
  answers JSON NOT NULL,  # Form field answers as JSON
  submitted_at TIMESTAMP NULL,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  
  FOREIGN KEY (form_id) REFERENCES forms(id),
  INDEX idx_consultation_id (consultation_id),
  INDEX idx_visit_id (visit_id),
  INDEX idx_patient_id (patient_id),
  INDEX idx_department_id (department_id),
  INDEX idx_status (status),
  UNIQUE KEY unique_consultation_form (consultation_id, form_id, form_schema_version)
);
```

## 4. Form Versioning System

### 4.1 Version Management
- **Schema Version**: Currently hardcoded to 1 in frontend
- **Form Version**: Auto-incremented when finalizing forms
- **Version History**: Track all form versions with status
- **Backward Compatibility**: Support older form versions in consultations

### 4.2 Version Control Rules
1. **Draft Forms**: Can be edited, version number doesn't change
2. **Finalized Forms**: Create new version when edited
3. **Active Forms**: Only FINAL forms can be used in consultations
4. **Version History**: Maintain complete history of all changes

## 5. Form Answer Storage

### 5.1 Answer Format
Answers are stored as JSON string with the following structure:
```json
{
  "field_id_1": "value1",
  "field_id_2": "value2",
  "table_field_id": [
    {"row": 0, "col": 0, "value": "cell1"},
    {"row": 0, "col": 1, "value": "cell2"}
  ],
  "checkbox_field_id": true,
  "date_field_id": "2024-01-01",
  "file_field_id": ["file_id_1", "file_id_2"]
}
```

### 5.2 Answer Status Flow
1. **DRAFT**: Auto-save while user is filling form
2. **FINAL**: Submitted when user completes consultation
3. **Edit**: Can edit DRAFT answers, FINAL requires new submission

## 6. Required Backend Endpoints

### 6.1 Form Management
- `POST /graphql` - Create form (createForm mutation)
- `PUT /graphql` - Update form (updateForm mutation)
- `POST /graphql` - Finalize form (finalizeForm mutation)
- `GET /graphql` - Get forms (getForms query)
- `GET /graphql` - Get single form (getForm query)
- `GET /graphql` - Get form version history (getFormVersionHistory query)

### 6.2 Consultation Management
- `POST /graphql` - Save/update consultation answers (upsertConsultationAnswers mutation)
- `GET /graphql` - Get consultation answers (getConsultationAnswers query)
- `POST /graphql` - Generate consultation PDF (generateConsultationPdf mutation)

## 7. Validation Requirements

### 7.1 Form Validation
- Required fields must be marked as required
- Field types must match expected input formats
- Conditional rendering logic must be validated
- Table configurations must be valid

### 7.2 Answer Validation
- Required fields must have values
- Field types must match input validation
- JSON structure must be valid
- Schema version must match form version

## 8. File Upload Support

### 8.1 File Field Types
- Support for file upload fields in forms
- Store file metadata in form answers
- Handle file storage and retrieval
- Support multiple files per field

### 8.2 File Storage Schema
```sql
CREATE TABLE form_files (
  id VARCHAR(255) PRIMARY KEY,
  consultation_answer_id VARCHAR(255) NOT NULL,
  field_id VARCHAR(255) NOT NULL,
  file_name VARCHAR(500) NOT NULL,
  file_path VARCHAR(1000) NOT NULL,
  file_size INT NOT NULL,
  mime_type VARCHAR(255) NOT NULL,
  uploaded_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  
  FOREIGN KEY (consultation_answer_id) REFERENCES consultation_answers(id) ON DELETE CASCADE,
  INDEX idx_consultation_answer_id (consultation_answer_id),
  INDEX idx_field_id (field_id)
);
```

## 9. Performance Considerations

### 9.1 Database Indexing
- Index on department_id for form queries
- Index on consultation_id for answer retrieval
- Index on status for filtering
- Composite indexes for common query patterns

### 9.2 Caching Strategy
- Cache active forms by department
- Cache form schemas for consultations
- Invalidate cache on form updates
- Use CDN for form assets

## 10. Security Considerations

### 10.1 Access Control
- Department-based access control
- Role-based permissions for form management
- Consultation access limited to assigned users
- Audit trail for form changes

### 10.2 Data Validation
- Sanitize all form inputs
- Validate JSON structures
- Prevent injection attacks
- Rate limiting for form submissions

## 11. Error Handling

### 11.1 Response Format
```json
{
  "status": "SUCCESS|ERROR",
  "messages": [
    {
      "text": "Error message",
      "type": "ERROR|WARNING|INFO"
    }
  ],
  "data": { ... }
}
```

### 11.2 Common Error Cases
- Form not found
- Invalid form schema
- Permission denied
- Validation errors
- Database constraints

This documentation provides the complete specification for implementing department forms functionality in the backend.
