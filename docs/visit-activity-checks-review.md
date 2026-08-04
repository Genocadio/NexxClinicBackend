# Visit Activity Checks — Review Status

This file tracks the review recommendations for visit/product/processor/department
behaviour and what has been applied to the backend.

## Already applied (verified in code)

| Recommendation | Where |
| --- | --- |
| **Soft-Deleted Product Restoration (SD1)** | `VisitService.addProductsToVisitDepartment` and `VisitDepartmentService.addVisitDepartmentProduct` both look up `findByVisitDepartmentIdAndProductIdIncludingDeleted` and restore the existing row (`setDeleted(false)`) instead of inserting a new one. The partial unique index (`V2__visit_department_products_partial_unique_index.sql`, `WHERE deleted = false`) stays satisfied because only one active row exists per (visit_department_id, product_id). |
| **Duplicate open visit check (V1)** | `VisitService.createVisit` rejects when the patient already has a `CREATED` or `IN_PROGRESS` visit (`findByPatientIdAndStatusIn`). A `PESSIMISTIC_WRITE` lock on the patient row (`PatientRepository.findByIdForUpdate`) is acquired before the check, serializing concurrent createVisit calls for the same patient so they cannot both pass. A partial unique index (`V7__visits_active_patient_partial_unique_index.sql`, `WHERE status IN ('CREATED','IN_PROGRESS')`) is the database-level backstop. |
| **Processor assignment defaults** | `VisitDepartmentService.assignVisitDepartmentProductProcessor` has an explicit default chain when `processorId` is null: acting user (if a processor) → sole department processor → explicit error when the department has multiple processors or (support-requests) no processors. |
| **Profile / department-type alignment** | `addVisitDepartment`, `applyProfileToVisitDepartment`, and `changeVisitDepartmentProfile` all reject profiles on departments with `supportRequests = true`. Child departments (always support-requests) reject profiles outright. |
| **Manual status overrides (S4)** | Product add and status-update paths reject client-supplied `BILLED` / `EXEMPTED` / `CORRECTION_PENDING`; those are set only by the billing service. |
| **Audit trail** | `addedBy` is stamped from the acting user on every add path; `billedBy` is stamped when a product is created with a non-PENDING status. |

## Applied in this round

- Added `log.debug(...)` to the two remaining `DataIntegrityViolationException`
  catch blocks that lacked it (`applyProfileProducts` and
  `addChildVisitDepartment` in `VisitDepartmentService`), so all four
  product-insertion paths log partial-unique-index races consistently for
  diagnosing concurrency issues in high-traffic environments.
- Added a pessimistic lock on the patient row in `createVisit`
  (`PatientRepository.findByIdForUpdate`, `PESSIMISTIC_WRITE`) so concurrent
  requests for the same patient serialize on the patient row and the second one
  sees the first's committed visit.
- Added `V7__visits_active_patient_partial_unique_index.sql`: a partial unique
  index on `visits (patient_id) WHERE status IN ('CREATED','IN_PROGRESS')` as a
  database-level guarantee against duplicate active visits. The migration
  detects pre-existing duplicates first and fails loudly (no data changed);
  ops resolves them manually and re-runs.

## Deferred — patient state validation

**Recommendation:** `createVisit` should verify the patient is in a valid state
(e.g. not marked as inactive/deceased).

**Current state:** `Patient` has no active/deceased/status concept. `createVisit`
validates only that the patient exists and has no open visit. A patient cannot
currently be flagged inactive or deceased anywhere in the system.

**Required to implement later:**

1. `Patient` entity field, e.g. `boolean deceased` / `LocalDateTime dateOfDeath`,
   or a `PatientStatus` enum (`ACTIVE` / `INACTIVE` / `DECEASED`).
2. Flyway migration (e.g. `V7`) adding the column(s) with a sensible default.
3. `createVisit` check rejecting visits for non-valid patients.
4. Admin UI (frontend repo) support to set/maintain the flag, plus the same
   validation surfaced in the UI.

Until then, the duplicate-open-visit and existence checks remain the only
guards on visit creation.
