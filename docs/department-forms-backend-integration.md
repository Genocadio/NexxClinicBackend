# Department Forms Backend Integration Guide

## Overview
This document provides the exact implementation details for integrating department forms functionality in the backend, including form creation, updating, versioning, and deletion operations as implemented in the frontend.

## 1. Form Creation Mutation

### 1.1 GraphQL Mutation
```graphql
mutation CreateForm($departmentId: ID!, $input: FormInput!) {
  createForm(departmentId: $departmentId, input: $input) {
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

### 1.2 FormInput Structure
```typescript
{
  title: string!           // Form title (required)
  description: string      // Form description (optional)
  schemaVersion: Int!      // Currently hardcoded to 1
  fields: [FormFieldInput] // Root-level fields
  sections: [FormSectionInput] // Form sections
  actions: [FormActionInput] // Form actions
}
```

### 1.3 Backend Implementation Requirements

#### Form Creation Process:
1. **Validate Input**: Ensure all required fields are present
2. **Generate Form ID**: Create unique form identifier
3. **Set Initial Status**: New forms start as "DRAFT"
4. **Set Version Number**: Initial version is "1.0"
5. **Store Schema Version**: Current schema version (1)
6. **Save Form Data**: Store complete form structure
7. **Return Response**: Include all form data in response

#### Database Insert:
```sql
INSERT INTO forms (
  id, 
  department_id, 
  title, 
  description, 
  status, 
  current_version_number, 
  current_schema_version, 
  form_data, 
  created_at, 
  updated_at
) VALUES (
  ?, -- generated UUID
  ?, -- departmentId
  ?, -- title
  ?, -- description
  'DRAFT', -- initial status
  '1.0', -- initial version
  1, -- schema version
  ?, -- JSON of fields, sections, actions
  NOW(), -- created_at
  NOW()  -- updated_at
);
```

## 2. Form Updating Mutation

### 2.1 GraphQL Mutation
```graphql
mutation UpdateForm($departmentId: ID!, $formId: ID!, $input: FormInput!) {
  updateForm(departmentId: $departmentId, formId: $formId, input: $input) {
    status
    messages {
      text
      type
    }
    data {
      # Same structure as CreateForm response
      ...FormBuilderData
    }
  }
}
```

### 2.2 Update Logic

#### For DRAFT Forms:
1. **Update existing form** without creating new version
2. **Keep same version number**
3. **Update form data** with new structure
4. **Update timestamps**

#### For FINAL Forms:
1. **Create new version** of the form
2. **Increment version number** (e.g., 1.0 → 1.1 → 2.0)
3. **Keep old version** in form_versions table
4. **Update main form** with new data
5. **Maintain version history**

#### Backend Implementation:
```sql
-- For DRAFT forms
UPDATE forms 
SET 
  title = ?, 
  description = ?, 
  form_data = ?, 
  updated_at = NOW() 
WHERE id = ? AND department_id = ? AND status = 'DRAFT';

-- For FINAL forms (create new version)
INSERT INTO form_versions (
  id, 
  form_id, 
  version_number, 
  status, 
  form_data, 
  created_at
) VALUES (
  ?, -- new version UUID
  ?, -- formId
  ?, -- new version number
  'FINAL', -- status
  ?, -- current form data
  NOW()
);

-- Update main form
UPDATE forms 
SET 
  title = ?, 
  description = ?, 
  current_version_number = ?, 
  form_data = ?, 
  updated_at = NOW() 
WHERE id = ?;
```

## 3. Form Versioning System

### 3.1 Finalize Form Mutation
```graphql
mutation FinalizeForm($departmentId: ID!, $formId: ID!) {
  finalizeForm(departmentId: $departmentId, formId: $formId) {
    status
    messages {
      text
      type
    }
    data {
      # Same structure as CreateForm response
      ...FormBuilderData
    }
  }
}
```

### 3.2 Versioning Logic

#### Finalization Process:
1. **Validate Form**: Ensure form has required fields
2. **Create Version Entry**: Save current state to form_versions
3. **Update Status**: Change from "DRAFT" to "FINAL"
4. **Increment Version**: If already finalized, create new version
5. **Update Timestamps**: Set new updated_at

#### Version Number Format:
- **Initial**: "1.0"
- **Minor updates**: "1.1", "1.2", etc.
- **Major updates**: "2.0", "3.0", etc.

#### Backend Implementation:
```sql
-- Create version entry
INSERT INTO form_versions (
  id, 
  form_id, 
  version_number, 
  status, 
  form_data, 
  created_at
) SELECT 
  UUID(), 
  id, 
  COALESCE(
    (SELECT MAX(CAST(SUBSTRING(version_number, 3) AS DECIMAL)) + 0.1 
     FROM form_versions 
     WHERE form_id = ? AND version_number LIKE ?), 
    1.0
  ), 
  'FINAL', 
  form_data, 
  NOW()
FROM forms 
WHERE id = ?;

-- Update main form
UPDATE forms 
SET 
  status = 'FINAL', 
  current_version_number = (
    SELECT version_number 
    FROM form_versions 
    WHERE form_id = ? 
    ORDER BY created_at DESC 
    LIMIT 1
  ), 
  updated_at = NOW() 
WHERE id = ?;
```

### 3.3 Version History Query
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

## 4. Form Deletion Operations

### 4.1 Important Note
**Currently, there is NO delete form mutation implemented in the frontend.** Forms are managed through status changes rather than deletion.

### 4.2 Recommended Deletion Strategy

#### Soft Delete Approach:
```graphql
mutation DeleteForm($departmentId: ID!, $formId: ID!) {
  deleteForm(departmentId: $departmentId, formId: $formId) {
    status
    messages {
      text
      type
    }
    data {
      id
      status
      deletedAt
    }
  }
}
```

#### Backend Implementation Options:

**Option 1: Soft Delete (Recommended)**
```sql
UPDATE forms 
SET 
  status = 'DELETED', 
  deleted_at = NOW(), 
  updated_at = NOW() 
WHERE id = ? AND department_id = ?;
```

**Option 2: Hard Delete (Not Recommended)**
```sql
-- Delete form versions first
DELETE FROM form_versions WHERE form_id = ?;
-- Delete form
DELETE FROM forms WHERE id = ? AND department_id = ?;
```

**Option 3: Archive Approach**
```sql
-- Move to archive table
INSERT INTO forms_archive 
SELECT *, NOW() as archived_at 
FROM forms 
WHERE id = ? AND department_id = ?;

-- Delete from main table
DELETE FROM forms WHERE id = ? AND department_id = ?;
```

## 5. Forms Fetching Queries

### 5.1 Get Forms for Department
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

### 5.2 Get Single Form
```graphql
query GetForm($departmentId: ID!, $formId: ID!) {
  getForm(departmentId: $departmentId, formId: $formId) {
    status
    messages {
      text
      type
    }
    data {
      # Complete form structure
      ...FormBuilderData
    }
  }
}
```

### 5.3 Backend Query Implementation

#### Get Forms List:
```sql
SELECT 
  id, 
  department_id, 
  title, 
  status, 
  current_version_number, 
  created_at, 
  updated_at 
FROM forms 
WHERE department_id = ? 
  AND status != 'DELETED' 
ORDER BY updated_at DESC;
```

#### Get Single Form:
```sql
SELECT 
  id, 
  department_id, 
  title, 
  description, 
  status, 
  current_version_number, 
  current_schema_version, 
  form_data, 
  created_at, 
  updated_at 
FROM forms 
WHERE id = ? AND department_id = ? 
  AND status != 'DELETED';
```

## 6. Form Data Structure

### 6.1 FormField Types
```typescript
enum FieldType {
  text, email, number, date, textarea, 
  select, radio, checkbox, table, 
  signature, file, heading, paragraph
}

interface FormField {
  id: string!
  label: string!
  type: FieldType!
  placeholder?: string
  required: boolean!
  order: number!
  hideLabel: boolean!
  boldLabel: boolean!
  italicLabel: boolean!
  underlineLabel: boolean!
  centerLabel: boolean!
  options?: string[]
  tableConfig?: {
    mode: 'static' | 'dynamic'
    rows?: number
    columns?: number
    headerPlacement?: string
    columnHeaders?: string[]
    rowHeaders?: string[]
  }
  conditionalRendering?: {
    dependsOn: string!
    condition: string!
    value?: string
    itemType?: string
  }
}
```

### 6.2 FormSection Structure
```typescript
interface FormSection {
  id: string!
  title: string!
  boldTitle: boolean!
  italicTitle: boolean!
  underlineTitle: boolean!
  centerTitle: boolean!
  columns: number!
  order: number!
  fields: FormField[]
}
```

### 6.3 FormAction Structure
```typescript
interface FormAction {
  id: string!
  name: string!
  type: string!
  quantity: number!
  price: number!
  isQuantifiable: boolean!
  backendId: string
}
```

## 7. Complete Backend Schema

### 7.1 Forms Table
```sql
CREATE TABLE forms (
  id VARCHAR(255) PRIMARY KEY,
  department_id VARCHAR(255) NOT NULL,
  title VARCHAR(500) NOT NULL,
  description TEXT,
  status ENUM('DRAFT', 'FINAL', 'DELETED') NOT NULL DEFAULT 'DRAFT',
  current_version_number VARCHAR(50) NOT NULL DEFAULT '1.0',
  current_schema_version INT NOT NULL DEFAULT 1,
  form_data JSON NOT NULL,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted_at TIMESTAMP NULL,
  
  INDEX idx_department_id (department_id),
  INDEX idx_status (status),
  INDEX idx_created_at (created_at),
  INDEX idx_updated_at (updated_at),
  INDEX idx_department_status (department_id, status)
);
```

### 7.2 Form Versions Table
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
  INDEX idx_created_at (created_at),
  UNIQUE KEY unique_form_version (form_id, version_number)
);
```

## 8. Error Handling

### 8.1 Response Format
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

### 8.2 Common Error Cases
- **Form not found**: "Form with ID {formId} not found"
- **Permission denied**: "You don't have permission to modify this form"
- **Invalid status**: "Cannot update finalized form without creating new version"
- **Validation error**: "Form title is required"
- **Department mismatch**: "Form does not belong to specified department"

## 9. Implementation Checklist

### 9.1 Required Mutations
- [ ] `createForm` - Create new form
- [ ] `updateForm` - Update existing form
- [ ] `finalizeForm` - Finalize draft form
- [ ] `deleteForm` - Delete form (optional)

### 9.2 Required Queries
- [ ] `getForms` - Get forms for department
- [ ] `getForm` - Get single form
- [ ] `getFormVersionHistory` - Get version history

### 9.3 Database Tables
- [ ] `forms` - Main forms table
- [ ] `form_versions` - Version history table

### 9.4 Validation Rules
- [ ] Form title required
- [ ] Department ID validation
- [ ] Form status validation
- [ ] Version number format validation
- [ ] JSON schema validation

## 10. Testing Requirements

### 10.1 Unit Tests
- Form creation with valid data
- Form creation with invalid data
- Form update for draft forms
- Form update for finalized forms
- Form finalization
- Form version history

### 10.2 Integration Tests
- Complete form lifecycle
- Version management
- Permission checks
- Data consistency

This documentation provides the exact implementation details needed to integrate department forms functionality in the backend system.
