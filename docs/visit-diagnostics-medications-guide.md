# Visit Department Diagnostics & Medications - Implementation Guide

## Overview

Visit departments can now have **diagnostics** and **medications** recorded alongside products. This allows clinicians to track:
- **Diagnostics**: Medical diagnoses with optional ICD-11 codes
- **Medications**: Prescribed medications with detailed instructions

Multiple diagnostics and medications can be added to each visit department.

---

## Database Schema

### New Tables

#### `visit_department_diagnostics`
```sql
CREATE TABLE visit_department_diagnostics (
  id UUID PRIMARY KEY,
  visit_department_id UUID NOT NULL REFERENCES visit_departments(id),
  diagnosis_name VARCHAR(255) NOT NULL,
  icd11_code VARCHAR(50),
  created_at TIMESTAMP NOT NULL,
  updated_at TIMESTAMP NOT NULL,
  INDEX idx_visit_department (visit_department_id)
);
```

#### `visit_department_medications`
```sql
CREATE TABLE visit_department_medications (
  id UUID PRIMARY KEY,
  visit_department_id UUID NOT NULL REFERENCES visit_departments(id),
  medication_name VARCHAR(255) NOT NULL,
  instructions TEXT NOT NULL,
  created_at TIMESTAMP NOT NULL,
  updated_at TIMESTAMP NOT NULL,
  INDEX idx_visit_department (visit_department_id)
);
```

---

## Entity Classes

### VisitDepartmentDiagnosis
```java
@Entity
@Table(name = "visit_department_diagnostics")
public class VisitDepartmentDiagnosis {
    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "visit_department_id", nullable = false)
    private VisitDepartment visitDepartment;

    @Column(nullable = false)
    private String diagnosisName;        // Mandatory

    @Column(name = "icd11_code")
    private String icd11Code;            // Optional

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;
    // ... getters/setters
}
```

### VisitDepartmentMedication
```java
@Entity
@Table(name = "visit_department_medications")
public class VisitDepartmentMedication {
    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "visit_department_id", nullable = false)
    private VisitDepartment visitDepartment;

    @Column(nullable = false)
    private String medicationName;       // Mandatory

    @Column(nullable = false)
    private String instructions;         // Mandatory

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;
    // ... getters/setters
}
```

### VisitDepartment (Updated)
```java
@Entity
public class VisitDepartment {
    // ... existing fields ...

    @OneToMany(mappedBy = "visitDepartment", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<VisitDepartmentDiagnosis> diagnostics = new ArrayList<>();

    @OneToMany(mappedBy = "visitDepartment", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<VisitDepartmentMedication> medications = new ArrayList<>();

    // ... getters/setters ...
}
```

---

## GraphQL Schema

### Types

#### VisitDepartmentDiagnosis
```graphql
type VisitDepartmentDiagnosis {
  id: UUID!
  diagnosisName: String!
  icd11Code: String        # Optional
  createdAt: String!
}
```

#### VisitDepartmentMedication
```graphql
type VisitDepartmentMedication {
  id: UUID!
  medicationName: String!
  instructions: String!
  createdAt: String!
}
```

### Input Types

#### AddDiagnosisInput
```graphql
input AddDiagnosisInput {
  visitDepartmentId: String!    # UUID of the VisitDepartment
  diagnosisName: String!        # Diagnosis name (mandatory)
  icd11Code: String             # ICD-11 code (optional)
}
```

#### AddMedicationInput
```graphql
input AddMedicationInput {
  visitDepartmentId: String!    # UUID of the VisitDepartment
  medicationName: String!       # Medication name (mandatory)
  instructions: String!         # Medication instructions (mandatory)
}
```

### Mutations

#### addDiagnosis
```graphql
mutation AddDiagnosis($input: AddDiagnosisInput!) {
  addDiagnosis(input: $input) {
    status
    messages {
      text
      type
    }
    data
  }
}
```

Example:
```json
{
  "input": {
    "visitDepartmentId": "3f2a1bc4-0000-0000-0000-000000000001",
    "diagnosisName": "Malaria, Uncomplicated",
    "icd11Code": "B50.9"
  }
}
```

#### addMedication
```graphql
mutation AddMedication($input: AddMedicationInput!) {
  addMedication(input: $input) {
    status
    messages {
      text
      type
    }
    data
  }
}
```

Example:
```json
{
  "input": {
    "visitDepartmentId": "3f2a1bc4-0000-0000-0000-000000000001",
    "medicationName": "Artemether-Lumefantrine",
    "instructions": "Take one tablet twice daily for 3 days with food"
  }
}
```

---

## API Responses

### Add Diagnosis Response
```json
{
  "status": "SUCCESS",
  "messages": [
    {
      "text": "Diagnosis added successfully.",
      "type": "INFO"
    }
  ],
  "data": {
    "id": "d1234567-89ab-cdef-0123-456789abcdef",
    "diagnosisName": "Malaria, Uncomplicated",
    "icd11Code": "B50.9",
    "createdAt": "2026-05-17T14:30:00"
  }
}
```

### Add Medication Response
```json
{
  "status": "SUCCESS",
  "messages": [
    {
      "text": "Medication added successfully.",
      "type": "INFO"
    }
  ],
  "data": {
    "id": "m1234567-89ab-cdef-0123-456789abcdef",
    "medicationName": "Artemether-Lumefantrine",
    "instructions": "Take one tablet twice daily for 3 days with food",
    "createdAt": "2026-05-17T14:30:00"
  }
}
```

---

## Service Methods

### VisitService.addDiagnosisToVisitDepartment()

```java
@Transactional
public ApiResponse addDiagnosisToVisitDepartment(AddDiagnosisInput input) {
    // Validates input
    // Finds VisitDepartment by ID
    // Creates new VisitDepartmentDiagnosis
    // Sets diagnosis name (mandatory)
    // Sets ICD-11 code (optional)
    // Saves and returns response with diagnosis data
}
```

**Parameters:**
- `diagnosisName` (String, required): Name of the diagnosis
- `icd11Code` (String, optional): ICD-11 code for the diagnosis
- `visitDepartmentId` (String, required): UUID of the visit department

**Returns:** ApiResponse with diagnosis data

### VisitService.addMedicationToVisitDepartment()

```java
@Transactional
public ApiResponse addMedicationToVisitDepartment(AddMedicationInput input) {
    // Validates input
    // Finds VisitDepartment by ID
    // Creates new VisitDepartmentMedication
    // Sets medication name (mandatory)
    // Sets instructions (mandatory)
    // Saves and returns response with medication data
}
```

**Parameters:**
- `medicationName` (String, required): Name of the medication
- `instructions` (String, required): Medication instructions/dosage
- `visitDepartmentId` (String, required): UUID of the visit department

**Returns:** ApiResponse with medication data

---

## Usage Examples

### Example 1: Add Multiple Diagnostics to a Visit Department

```graphql
mutation {
  diagnosis1: addDiagnosis(input: {
    visitDepartmentId: "3f2a1bc4-0000-0000-0000-000000000001"
    diagnosisName: "Malaria, Uncomplicated"
    icd11Code: "B50.9"
  }) {
    status
    data
  }
  
  diagnosis2: addDiagnosis(input: {
    visitDepartmentId: "3f2a1bc4-0000-0000-0000-000000000001"
    diagnosisName: "Anaemia"
    icd11Code: "D50.9"
  }) {
    status
    data
  }
}
```

### Example 2: Add Multiple Medications

```graphql
mutation {
  medication1: addMedication(input: {
    visitDepartmentId: "3f2a1bc4-0000-0000-0000-000000000001"
    medicationName: "Artemether-Lumefantrine"
    instructions: "Take one tablet twice daily for 3 days with food"
  }) {
    status
    data
  }
  
  medication2: addMedication(input: {
    visitDepartmentId: "3f2a1bc4-0000-0000-0000-000000000001"
    medicationName: "Ferrous Sulfate"
    instructions: "Take one tablet once daily for 30 days"
  }) {
    status
    data
  }
}
```

### Example 3: Add Diagnosis Without ICD-11 Code

```graphql
mutation {
  addDiagnosis(input: {
    visitDepartmentId: "3f2a1bc4-0000-0000-0000-000000000001"
    diagnosisName: "Suspected Dengue Fever"
  }) {
    status
    messages { text type }
    data
  }
}
```

---

## Database Migration

If using Flyway or Liquibase, create migration scripts:

### V1__Add_Visit_Department_Diagnostics_And_Medications.sql
```sql
-- Create diagnostics table
CREATE TABLE visit_department_diagnostics (
    id CHAR(36) NOT NULL PRIMARY KEY,
    visit_department_id CHAR(36) NOT NULL,
    diagnosis_name VARCHAR(255) NOT NULL,
    icd11_code VARCHAR(50),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_vdd_visit_department 
        FOREIGN KEY (visit_department_id) 
        REFERENCES visit_departments(id) ON DELETE CASCADE,
    INDEX idx_visit_department_id (visit_department_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Create medications table
CREATE TABLE visit_department_medications (
    id CHAR(36) NOT NULL PRIMARY KEY,
    visit_department_id CHAR(36) NOT NULL,
    medication_name VARCHAR(255) NOT NULL,
    instructions TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_vdm_visit_department 
        FOREIGN KEY (visit_department_id) 
        REFERENCES visit_departments(id) ON DELETE CASCADE,
    INDEX idx_visit_department_id (visit_department_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

---

## Files Modified/Created

### Created:
- `src/main/java/com/nexxserve/nexxclinic/entity/VisitDepartmentDiagnosis.java`
- `src/main/java/com/nexxserve/nexxclinic/entity/VisitDepartmentMedication.java`
- `src/main/java/com/nexxserve/nexxclinic/repository/VisitDepartmentDiagnosisRepository.java`
- `src/main/java/com/nexxserve/nexxclinic/repository/VisitDepartmentMedicationRepository.java`
- `src/main/java/com/nexxserve/nexxclinic/graphql/input/AddDiagnosisInput.java`
- `src/main/java/com/nexxserve/nexxclinic/graphql/input/AddMedicationInput.java`
- `src/main/resources/graphql/visits.graphqls`

### Updated:
- `src/main/java/com/nexxserve/nexxclinic/entity/VisitDepartment.java` - Added relationships
- `src/main/java/com/nexxserve/nexxclinic/service/VisitService.java` - Added service methods + repositories
- `src/main/java/com/nexxserve/nexxclinic/graphql/VisitMutationController.java` - Added mutations

---

## Build Status

✅ **Build Successful** - All changes compile without errors

---

## Testing Checklist

- [ ] Database migration creates tables
- [ ] Add diagnosis with ICD-11 code
- [ ] Add diagnosis without ICD-11 code
- [ ] Add multiple diagnostics to same visit department
- [ ] Add medication with instructions
- [ ] Add multiple medications to same visit department
- [ ] Verify diagnostics cascade delete when visit department deleted
- [ ] Verify medications cascade delete when visit department deleted
- [ ] Query visit department with nested diagnostics
- [ ] Query visit department with nested medications
- [ ] Permission validation for addDiagnosis mutation
- [ ] Permission validation for addMedication mutation

---

## Relationships & Cascade Behavior

- **VisitDepartment ↔ VisitDepartmentDiagnosis**: One-to-Many (CASCADE DELETE)
  - Deleting a VisitDepartment deletes all associated diagnostics
  
- **VisitDepartment ↔ VisitDepartmentMedication**: One-to-Many (CASCADE DELETE)
  - Deleting a VisitDepartment deletes all associated medications

---

## Role-Based Access Control

Both mutations (`addDiagnosis` and `addMedication`) require one of these roles:
- `ADMIN`
- `CLINIC_ADMIN`
- `RECEPTION`
- `NURSE`
- `CLINICIAN`

---

## Notes

1. **Mandatory vs Optional Fields:**
   - Diagnosis: `diagnosisName` is mandatory, `icd11Code` is optional
   - Medication: Both `medicationName` and `instructions` are mandatory

2. **Multiple Records:**
   - Multiple diagnostics can be added to a single visit department
   - Multiple medications can be added to a single visit department
   - No limit enforced at API level

3. **Data Integrity:**
   - All records are timestamped (createdAt, updatedAt)
   - Cascade delete ensures referential integrity
   - UUID used for all IDs

4. **Future Enhancement Opportunities:**
   - Add dosage/frequency/duration fields to medications
   - Add severity/status fields to diagnostics
   - Add medication interaction checking
   - Add allergy/contraindication warnings
