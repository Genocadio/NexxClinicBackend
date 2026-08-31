package com.nexxserve.nexxclinic.model;

public enum VisitDepartmentStatus {
    ACTIVE,
    PENDING,
    ON_HOLD,
    BILLING,
    COMPLETED,
    FINALISED,
    CANCELLED,
    /**
     * Transitional status: the department was COMPLETED/FINALISED but billing
     * is being edited at the department level. Product mutations are allowed
     * only in this mode. Transitions back to the remembered pre-edit status
     * via completeBillEditing or cancelBillEditing.
     */
    DEPARTMENT_EDITING
}
