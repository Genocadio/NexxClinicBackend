package com.nexxserve.nexxclinic.model;

public enum VisitStatus {
    CREATED,
    IN_PROGRESS,
    CANCELLED,
    COMPLETED,
    /**
     * Transitional status: the visit was COMPLETED but billing is being edited.
     * Product/insurance/billing mutations are allowed only in this mode.
     * Transitions back to COMPLETED via completeBillEditing or cancelBillEditing.
     */
    BILL_EDITING,
    /**
     * Terminal status: all departments are FINALISED and no further edits are
     * allowed unless an ADMIN re-opens the visit.
     */
    FINALISED
}
