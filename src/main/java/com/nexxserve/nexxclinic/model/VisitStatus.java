package com.nexxserve.nexxclinic.model;

public enum VisitStatus {
    CREATED,
    IN_PROGRESS,
    CANCELLED,
    COMPLETED,
    /**
     * Terminal status: all departments are FINALISED and no further edits are
     * allowed unless an ADMIN re-opens the visit.
     */
    FINALISED
}
