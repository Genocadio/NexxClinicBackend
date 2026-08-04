package com.nexxserve.nexxclinic.service.billing;

import com.nexxserve.nexxclinic.entity.VisitDepartment;
import com.nexxserve.nexxclinic.entity.Worker;
import com.nexxserve.nexxclinic.graphql.input.BillVisitInput;
import com.nexxserve.nexxclinic.model.RoleName;
import com.nexxserve.nexxclinic.model.VisitDepartmentStatus;
import com.nexxserve.nexxclinic.repository.VisitDepartmentNoteRepository;
import com.nexxserve.nexxclinic.repository.VisitDepartmentRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * Shared billing validations: the unread-notes gate and submitted-payment
 * structural validation. Keeping these in one place means every entry point
 * (bill, edit, payment, invoice) enforces identical rules.
 */
@Component
public class BillingValidation {

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
}
