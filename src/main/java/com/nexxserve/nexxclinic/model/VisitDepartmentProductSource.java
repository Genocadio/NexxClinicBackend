package com.nexxserve.nexxclinic.model;

/**
 * How a {@link com.nexxserve.nexxclinic.entity.VisitDepartmentProduct} was added
 * to a visit department.
 *
 * <ul>
 *   <li>{@link #USER} — added manually through the normal add-product flow
 *       (addVisitDepartmentProduct, createVisit products, editBillVisit additions).</li>
 *   <li>{@link #PROFILE} — added automatically from a department profile when the
 *       department was added to the visit (or the visit department's profile was
 *       changed). Profile products are managed by the profile: they cannot be
 *       removed individually — change the visit department's profile instead.</li>
 * </ul>
 */
public enum VisitDepartmentProductSource {
    USER,
    PROFILE
}
