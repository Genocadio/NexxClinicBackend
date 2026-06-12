# GraphQL Schema Corrections for Form Types

## Summary
This document outlines the necessary corrections to the GraphQL schema to ensure full alignment with the form-format-guide.md specification.

## Issue
The GraphQL `FieldType` enum was missing four critical field types that are defined in the form-format-guide:
- `diagnosticRecord`
- `medicationLongForm`
- `medicationMiniForm`
- `actionListener`

This caused the error:
```
[GraphQL]: Variable 'input' has an invalid value: Invalid input for enum 'FieldType'. No value found for name 'diagnosticRecord'
```

## Corrections Applied

### 1. FieldType Enum

**Old (Incorrect):**
```graphql
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
  signature    # Not in form-format-guide
  file         # Not in form-format-guide
  heading      # Not in form-format-guide
  paragraph    # Not in form-format-guide
}
```

**New (Correct):**
```graphql
enum FieldType {
  text
  email
  number
  textarea
  date
  select
  radio
  checkbox
  table
  diagnosticRecord       # Required: diagnosis entry list widget
  medicationLongForm     # Required: full medication entry form
  medicationMiniForm     # Required: lightweight medication entry form
  actionListener         # Required: billable action trigger
}
```

### 2. FormStatus Enum

**Old (Incorrect):**
```graphql
enum FormStatus {
  DRAFT
  FINAL
}
```

**New (Correct):**
```graphql
enum FormStatus {
  draft
  published
  archived
}
```

#### Status Lifecycle Rules (from form-format-guide §8):
- **draft**: Editable. Not visible to clinicians.
- **published**: Locked. Active and visible to clinicians. Only one per department.
- **archived**: Locked. No longer visible. Retained for historical submissions.
- **Publish rule**: Publishing a new draft automatically archives the previous published form in the same transaction.

### 3. TableMode Enum

**Old (Incorrect):**
```graphql
enum TableMode {
  static
  dynamic
}
```

**New (Correct):**
```graphql
enum TableMode {
  fixed
  variableRows
  variableColumns
}
```

#### Mode Definitions (from form-format-guide §6):
- **fixed**: locked grid (clinician cannot modify dimensions)
- **variableRows**: clinician can add rows
- **variableColumns**: clinician can add columns

### 4. ConditionalCondition Enum

**Old (Incorrect):**
```graphql
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
```

**New (Correct):**
```graphql
enum ConditionalCondition {
  notEmpty
  equals
  checked
  includes
  hasItem
}
```

#### Condition Type Definitions (from form-format-guide §5):
- **notEmpty**: Show when the trigger field has any non-empty value.
- **equals**: Show when the trigger field value exactly matches `value`.
- **checked**: Show when the trigger field (checkbox/radio) is checked/selected.
- **includes**: Show when the trigger field value contains the string `value`.
- **hasItem**: Show when an `actionListener` has a specific action/consumable added.

### 5. AnswerStatus Enum

**Old (Incorrect):**
```graphql
enum AnswerStatus {
  DRAFT
  FINAL
}
```

**New (Correct):**
```graphql
enum AnswerStatus {
  draft
  submitted
}
```

## Backend Java Implementation Required

### 1. GraphQL Type Definition File
Update your GraphQL schema definition (typically in `.graphqls` file) with the corrected enums:

```graphql
enum FieldType {
  text
  email
  number
  textarea
  date
  select
  radio
  checkbox
  table
  diagnosticRecord
  medicationLongForm
  medicationMiniForm
  actionListener
}

enum FormStatus {
  draft
  published
  archived
}

enum TableMode {
  fixed
  variableRows
  variableColumns
}

enum ConditionalCondition {
  notEmpty
  equals
  checked
  includes
  hasItem
}

enum AnswerStatus {
  draft
  submitted
}
```

### 2. Java Enum Classes
Ensure Java enum classes match:
- `FieldType.java` - must include all 14 field types
- `FormStatus.java` - must have draft, published, archived (lowercase)
- `TableMode.java` - must have fixed, variableRows, variableColumns
- `ConditionalCondition.java` - must have the 5 condition types

### 3. Database Schema Constraints
Update your database schema enums to match the GraphQL definitions:

```sql
-- Update forms table status enum
ALTER TABLE forms MODIFY status ENUM('draft', 'published', 'archived') NOT NULL DEFAULT 'draft';

-- Update form_versions table status enum
ALTER TABLE form_versions MODIFY status ENUM('draft', 'published', 'archived') NOT NULL;

-- Update consultation_answers table status enum
ALTER TABLE consultation_answers MODIFY status ENUM('draft', 'submitted') NOT NULL DEFAULT 'draft';
```

### 4. Resolver Validation
When validating field types in create/update form mutations, ensure:
- All field types from the FieldType enum are properly handled
- `diagnosticRecord`, `medicationLongForm`, `medicationMiniForm`, `actionListener` don't require `options` or `tableConfig`
- Proper error messages are returned when invalid field types are provided

## Testing

### Test Payload (Should Now Pass)
```json
{
  "departmentId": "1628efbc-fb7e-4790-aeb6-d12075746fd4",
  "formId": "34755036-9e19-431e-bd68-aebc77043f6c",
  "input": {
    "title": "oph test",
    "description": "",
    "fields": [
      {
        "id": "field_1777895234592",
        "label": "dd",
        "type": "text",
        "required": true,
        "order": 1
      },
      {
        "id": "field_1778514831970",
        "label": "products",
        "type": "actionListener",
        "required": true,
        "order": 2
      },
      {
        "id": "field_1779032239159",
        "label": "Diagonastics",
        "type": "diagnosticRecord",
        "required": true,
        "order": 3
      }
    ],
    "sections": [],
    "actions": []
  }
}
```

### Expected Result
GraphQL should accept all field types without enum validation errors.

## Migration Checklist

- [x] Update `.graphqls` schema file with corrected enums — **COMPLETED** in `src/main/resources/graphql/forms.graphqls`
- [x] Update Java enum classes to match — **ALREADY CORRECT** (FieldType, FormStatus, TableMode, ConditionalCondition, AnswerStatus all have correct values)
- [ ] Update database schema enums (if using MySQL ENUM type instead of storing as strings)
- [ ] Update resolvers to handle new field types (verify validation logic)
- [ ] Add validation tests for each field type
- [ ] Test form creation with diagnosticRecord
- [ ] Test form creation with medicationLongForm
- [ ] Test form creation with medicationMiniForm
- [ ] Test form creation with actionListener
- [ ] Test conditional rendering with hasItem condition
- [ ] Test status lifecycle (draft → published → archived)
- [ ] Update any API documentation that references old enum values

## Applied Corrections

### GraphQL Schema File Created
A complete GraphQL schema file has been created at `src/main/resources/graphql/forms.graphqls` with:
- All 14 field types in FieldType enum
- Correct FormStatus enum (draft, published, archived)
- Correct TableMode enum (fixed, variableRows, variableColumns)
- Correct ConditionalCondition enum (notEmpty, equals, checked, includes, hasItem)
- All input types matching the form-format-guide specification
- All output types for forms and consultation answers
- Complete Query and Mutation operations

The Java enum classes (`src/main/java/com/nexxserve/nexxclinic/model/`) are already correctly defined and match the GraphQL schema.
