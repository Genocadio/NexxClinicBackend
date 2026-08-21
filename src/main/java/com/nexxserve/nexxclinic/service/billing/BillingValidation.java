package com.nexxserve.nexxclinic.service.billing;

import com.nexxserve.nexxclinic.entity.VisitDepartment;
import com.nexxserve.nexxclinic.entity.VisitDepartmentBilling;
import com.nexxserve.nexxclinic.entity.Worker;
import com.nexxserve.nexxclinic.graphql.input.BillVisitInput;
import com.nexxserve.nexxclinic.model.RoleName;
import com.nexxserve.nexxclinic.model.VisitDepartmentStatus;
import com.nexxserve.nexxclinic.repository.VisitDepartmentNoteRepository;
import com.nexxserve.nexxclinic.repository.VisitDepartmentRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Shared billing validations: the unread-notes gate, submitted-payment
 * structural validation, overpayment rejection, and billing-note requirement.
 * Keeping these in one place means every entry point (bill, edit, payment, invoice)
 * enforces identical rules.
 */
@Component
public class BillingValidation {

    private static final Logger log = LoggerFactory.getLogger(BillingValidation.class);

    private final VisitDepartmentRepository visitDepartmentRepository;
    private final VisitDepartmentNoteRepository visitDepartmentNoteRepository;

    public BillingValidation(
        VisitDepartmentRepository visitDepartmentRepository,
        VisitDepartmentNoteRepository visitDepartmentNoteRepository
    ) {
        this.visitDepartmentRepository = visitDepartmentRepository;
        this.visitDepartmentNoteRepository = visitDepartmentNoteRepository;
    }

    /**
     * Counts unread notes on any non-CANCELLED department of the visit for the
     * given viewer. ADMIN and CLINIC_ADMIN bypass the gate. Returns 0 for a null
     * viewer so callers can fail closed on top of this.
     */
    public long countUnreadNotesForVisit(UUID visitId, Worker viewer) {
        if (visitId == null || viewer == null || viewer.getId() == null) {
            return 0;
        }

        // Business rule: ADMIN and CLINIC_ADMIN can bypass unread notes check for billing.
        if (viewer.getRoles() != null && (
            viewer.getRoles().contains(RoleName.ADMIN) ||
            viewer.getRoles().contains(RoleName.CLINIC_ADMIN)
        )) {
            return 0;
        }

        List<VisitDepartment> departments = visitDepartmentRepository.findByVisitId(visitId);
        long total = 0;
        for (VisitDepartment vd : departments) {
            // F1 fix: notes on CANCELLED departments must not block billing of the
            // departments that are actually being billed.
            if (vd.getStatus() == VisitDepartmentStatus.CANCELLED) {
                continue;
            }
            total += visitDepartmentNoteRepository.countNewNotesForViewer(vd.getId(), viewer.getId());
        }
        return total;
    }

    /**
     * Validates a department's submitted payment list. Returns an error message,
     * or {@code null} when every payment row is structurally valid.
     */
    public String validatePayments(List<BillVisitInput.BillingPaymentInput> payments) {
        if (payments == null) {
            return null;
        }
        for (BillVisitInput.BillingPaymentInput payment : payments) {
            if (payment == null || payment.amount() == null || payment.paymentMethod() == null) {
                return "Each payment requires amount and paymentMethod.";
            }
            if (payment.amount().compareTo(MoneyUtils.ZERO) <= 0) {
                return "Payment amount must be greater than 0.";
            }
        }
        return null;
    }

    /**
     * Validates that no department has overpayment after distribution across
     * insurance buckets. Returns an error message, or {@code null} when all
     * departments are within bounds.
     *
     * @param remainingPaidByDepartment unallocated remaining payment per department (after distribution)
     * @param rootDepartments           department ID → VisitDepartment (for error messages)
     * @param rootPaymentsByDepartment  department ID → submitted payments (for error messages)
     * @param departmentBillingByRoot   department ID → VisitDepartmentBilling (for diagnostic logging)
     * @param effectiveIsEdit           whether this is an edit or incremental billing
     * @param carriedPaidByDepartment   department ID → previously paid amount (for edit overpayment check)
     * @param paymentDistributor        for totalPayments diagnostic
     * @param visitId                  visit ID (for diagnostic logging)
     */
    public String validateOverpayment(
        Map<UUID, BigDecimal> remainingPaidByDepartment,
        Map<UUID, VisitDepartment> rootDepartments,
        Map<UUID, List<BillVisitInput.BillingPaymentInput>> rootPaymentsByDepartment,
        Map<UUID, VisitDepartmentBilling> departmentBillingByRoot,
        boolean effectiveIsEdit,
        Map<UUID, BigDecimal> carriedPaidByDepartment,
        BillingPaymentDistributor paymentDistributor,
        UUID visitId
    ) {
        for (Map.Entry<UUID, BigDecimal> entry : remainingPaidByDepartment.entrySet()) {
            if (entry.getValue().compareTo(MoneyUtils.ZERO) > 0) {
                VisitDepartment dept = rootDepartments.get(entry.getKey());
                String deptName = dept != null && dept.getDepartment() != null
                    ? dept.getDepartment().getName()
                    : "department";
                // DIAG: dump the overpayment context before rejecting.
                if (log.isDebugEnabled()) {
                    VisitDepartmentBilling deptBilling = departmentBillingByRoot.get(entry.getKey());
                    log.debug(
                        "[BILL-DIAG] visit={} REJECT overpayment dept={} ({}): unallocatedRemaining={} effectiveIsEdit={} carriedPaid={} submittedTotal={} deptTotal={} deptInsuranceCovered={} deptPatientPayable={} deptPaid={} deptOutstanding={} deptStatus={}",
                        visitId, entry.getKey(), deptName,
                        entry.getValue(), effectiveIsEdit,
                        carriedPaidByDepartment.getOrDefault(entry.getKey(), MoneyUtils.ZERO),
                        paymentDistributor.totalPayments(
                            rootPaymentsByDepartment.get(entry.getKey())
                        ),
                        deptBilling == null ? null : deptBilling.getTotalAmount(),
                        deptBilling == null ? null : deptBilling.getInsuranceCoveredAmount(),
                        deptBilling == null ? null : deptBilling.getPatientPayableAmount(),
                        deptBilling == null ? null : deptBilling.getPaidAmount(),
                        deptBilling == null ? null : deptBilling.getOutstandingAmount(),
                        deptBilling == null ? null : deptBilling.getStatus()
                    );
                }
                if (effectiveIsEdit && carriedPaidByDepartment.containsKey(entry.getKey())) {
                    // N2: the corrected bill is smaller than the amount already paid
                    // (e.g. a paid product was erased). Surface this clearly instead of
                    // silently dropping the patient's money.
                    return "The corrected bill for " + deptName + " (" +
                        entry.getValue().toPlainString() +
                        ") is smaller than the amount already paid. Keep the paid product or " +
                        "adjust the payments before correcting the billing.";
                }
                return "Payment amount would exceed the patient payable amount for " + deptName + ".";
            }
        }
        return null;
    }

    /**
     * Validates that billing notes are present when required: mandatory when any
     * product is exempted or when the patient payment is less than the full payable
     * amount. Carried-forward departments are excluded (their notes were already
     * recorded in a previous version).
     */
    public String validateBillingNotes(
        Map<UUID, VisitDepartmentBilling> departmentBillingByRoot,
        Set<UUID> exemptedRootDepartmentIds,
        Set<UUID> carriedDepartmentIds,
        Map<UUID, String> noteByDepartmentId
    ) {
        for (Map.Entry<UUID, VisitDepartmentBilling> noteEntry : departmentBillingByRoot.entrySet()) {
            UUID rootDeptId = noteEntry.getKey();
            if (carriedDepartmentIds.contains(rootDeptId)) {
                // A carried-forward department was already billed (with its note) in a
                // previous version and is being re-billed with identical amounts. The
                // fresh-balance note rule does not apply to it.
                continue;
            }
            VisitDepartmentBilling deptBilling = noteEntry.getValue();
            boolean requiresNote =
                exemptedRootDepartmentIds.contains(rootDeptId) ||
                deptBilling.getOutstandingAmount().compareTo(MoneyUtils.ZERO) > 0;
            if (requiresNote && !hasText(noteByDepartmentId.get(rootDeptId))) {
                return "A billing note is required when items are exempted or the patient payment is less than the payable amount.";
            }
        }
        return null;
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
