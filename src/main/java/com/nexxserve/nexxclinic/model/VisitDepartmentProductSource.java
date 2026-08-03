package com.nexxserve.nexxclinic.model;

/**
 * How a {@link com.nexxserve.nexxclinic.entity.VisitDepartmentProduct} was added
 * to a visit department.
 *
 * <ul>
 *   <li>{@link #USER} — added manually through the normal add-product flow
 *       (addVisitDepartmentProduct, createVisit products, editBillVisit additions).</li>
 *   <li>{@link #PROFILE} — added from a department profile that was explicitly
 *       applied to the visit department (via addVisitDepartment/createVisit with a
 *       profileId, or changeVisitDepartmentProfile). Profiles are never
 *       auto-applied. Profile products are managed by the profile: they cannot be
 *       removed individually — change the visit department's profile instead.</li>
 * </ul>
 */
public enum VisitDepartmentProductSource {
    USER,
    PROFILE
}
