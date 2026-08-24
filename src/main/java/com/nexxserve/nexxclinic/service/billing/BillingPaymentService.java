package com.nexxserve.nexxclinic.service.billing;

import com.nexxserve.nexxclinic.auth.AuthenticatedUser;
import com.nexxserve.nexxclinic.dto.out.ApiResponse;
import com.nexxserve.nexxclinic.entity.DepartmentInsuranceBilling;
import com.nexxserve.nexxclinic.entity.Visit;
import com.nexxserve.nexxclinic.entity.VisitBillingPayment;
import com.nexxserve.nexxclinic.entity.VisitDepartment;
import com.nexxserve.nexxclinic.entity.VisitDepartmentBilling;
import com.nexxserve.nexxclinic.entity.VisitDepartmentNote;
import com.nexxserve.nexxclinic.entity.VisitInsurance;
import com.nexxserve.nexxclinic.entity.Worker;
import com.nexxserve.nexxclinic.graphql.input.BillVisitInput;
import com.nexxserve.nexxclinic.graphql.input.RecordVisitBillingPaymentInput;
import com.nexxserve.nexxclinic.model.NoteType;
import com.nexxserve.nexxclinic.model.VisitStatus;
import com.nexxserve.nexxclinic.repository.DepartmentInsuranceBillingRepository;
import com.nexxserve.nexxclinic.repository.VisitBillingVersionRepository;
import com.nexxserve.nexxclinic.repository.VisitDepartmentBillingRepository;
import com.nexxserve.nexxclinic.repository.VisitDepartmentNoteRepository;
import com.nexxserve.nexxclinic.repository.VisitRepository;
import com.nexxserve.nexxclinic.repository.WorkerRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import static com.nexxserve.nexxclinic.service.billing.MoneyUtils.ZERO;
import static com.nexxserve.nexxclinic.service.billing.MoneyUtils.toMoney;

/**
 * Handles recording post-billing payments against an existing billing version.
 *
 * <p>Extracted from {@code VisitBillingService} to isolate the payment recording
 * flow from the billing orchestration.
 */
@Component
public class BillingPaymentService {

    private static final Logger log = LoggerFactory.getLogger(BillingPaymentService.class);

    private final VisitRepository visitRepository;
    private final VisitDepartmentBillingRepository visitDepartmentBillingRepository;
    private final DepartmentInsuranceBillingRepository departmentInsuranceBillingRepository;
    private final VisitBillingVersionRepository visitBillingVersionRepository;
    private final WorkerRepository workerRepository;
    private final VisitDepartmentNoteRepository visitDepartmentNoteRepository;
    private final BillingValidation billingValidation;
    private final BillingPaymentDistributor paymentDistributor;
    private final BillingDataMapper billingDataMapper;

    public BillingPaymentService(
        VisitRepository visitRepository,
        VisitDepartmentBillingRepository visitDepartmentBillingRepository,
        DepartmentInsuranceBillingRepository departmentInsuranceBillingRepository,
        VisitBillingVersionRepository visitBillingVersionRepository,
        WorkerRepository workerRepository,
        VisitDepartmentNoteRepository visitDepartmentNoteRepository,
        BillingValidation billingValidation,
        BillingPaymentDistributor paymentDistributor,
        BillingDataMapper billingDataMapper
    ) {
        this.visitRepository = visitRepository;
        this.visitDepartmentBillingRepository = visitDepartmentBillingRepository;
        this.departmentInsuranceBillingRepository = departmentInsuranceBillingRepository;
        this.visitBillingVersionRepository = visitBillingVersionRepository;
        this.workerRepository = workerRepository;
        this.visitDepartmentNoteRepository = visitDepartmentNoteRepository;
        this.billingValidation = billingValidation;
        this.paymentDistributor = paymentDistributor;
        this.billingDataMapper = billingDataMapper;
    }

    /**
     * Records a payment against a department insurance billing bucket.
     *
     * <p>A3/A4 fix: acquires the per-visit lock BEFORE reading paidAmount so the
     * read-modify-write is serialized against concurrent payments/edits.
     *
     * <p>H1/H2 fix: payments must target the LATEST billing version.
     */
    public ApiResponse recordVisitBillingPayment(
        RecordVisitBillingPaymentInput input,
        AuthenticatedUser authUser
    ) {
        if (input == null || input.departmentInsuranceBillingId() == null) {
            return ApiResponse.error("departmentInsuranceBillingId is required.");
        }

        Worker actingUser = resolveWorker(authUser);
        if (actingUser == null) {
            // F3/F4 fix: fail closed (see billOrEditVisitInternal).
            return ApiResponse.error("Authentication is required to record a payment.");
        }
        // For consistency with billing/editing rules: block payments when user has unread notes.
        // A3 fix: use the LIGHTWEIGHT visitId lookup here (it does not hydrate the billing
        // entity), so the billing row is not cached in the persistence context with a
        // pre-lock snapshot before we acquire the per-visit lock below.
        UUID visitId = departmentInsuranceBillingRepository
            .findVisitIdById(input.departmentInsuranceBillingId())
            .orElse(null);
        if (visitId == null) {
            return ApiResponse.error("Associated visit not found for this billing.");
        }
        long unreadNotes = billingValidation.countUnreadNotesForVisit(visitId, actingUser);
        if (unreadNotes > 0) {
            return ApiResponse.error("You have unread notes. Please read them before recording payments.");
        }
        String paymentError = billingValidation.validatePayments(List.of(
            new BillVisitInput.BillingPaymentInput(
                input.amount(),
                input.paymentMethod(),
                input.reference()
            )
        ));
        if (paymentError != null) {
            return ApiResponse.error(paymentError);
        }

        // A3/A4 fix: acquire the per-visit lock BEFORE reading paidAmount so the
        // read-modify-write is serialized against concurrent payments/edits (otherwise two
        // payments could both pass the <= patientPayable check and overpay or lose one).
        visitRepository.findByIdForUpdate(visitId);

        // A3 fix: load the billing row with a PESSIMISTIC_WRITE lock AFTER the per-visit
        // lock. The FOR UPDATE re-reads committed state into the persistence context, so
        // paidAmount below reflects any payment a concurrent transaction already committed
        // (a plain findById here would return the stale pre-lock snapshot).
        Optional<DepartmentInsuranceBilling> billingOptional =
            departmentInsuranceBillingRepository.findByIdWithDepartmentBillingAndVisitForUpdate(
                input.departmentInsuranceBillingId()
            );
        if (billingOptional.isEmpty()) {
            return ApiResponse.error("Department insurance billing not found.");
        }

        DepartmentInsuranceBilling insuranceBilling = billingOptional.get();
        // Null-safe: the chain getVisitDepartmentBilling() -> getVisitBilling() -> getVisit()
        // may break on legacy data where an intermediate FK is null.
        Visit visit = null;
        try {
            if (insuranceBilling.getVisitDepartmentBilling() != null
                && insuranceBilling.getVisitDepartmentBilling().getVisitBilling() != null) {
                visit = insuranceBilling.getVisitDepartmentBilling().getVisitBilling().getVisit();
            }
        } catch (Exception e) {
            log.warn("Failed to resolve visit from billing {}: {}",
                insuranceBilling.getId(), e.getMessage());
        }
        if (visit == null) {
            return ApiResponse.error("Cannot resolve visit from this billing. Data may be corrupt.");
        }
        if (visit.getStatus() == VisitStatus.CANCELLED) {
            return ApiResponse.error(
                "Cancelled visits cannot accept billing payments."
            );
        }

        // H1/H2 fix: a payment must always target the LATEST billing version. Paying
        // against an old version would silently mutate stale data the finance team no
        // longer sees as authoritative. Legacy rows with no version are accepted only
        // when the visit has no version rows at all (pre-version-system data).
        Optional<com.nexxserve.nexxclinic.entity.billing.VisitBillingVersion> latestVersionOpt =
            visitBillingVersionRepository.findFirstByVisitIdOrderByVersionDesc(visit.getId());
        UUID latestVersionId = latestVersionOpt
            .map(com.nexxserve.nexxclinic.entity.billing.VisitBillingVersion::getId)
            .orElse(null);

        UUID paymentVersionId = insuranceBilling.getBillingVersion() == null
            ? null
            : insuranceBilling.getBillingVersion().getId();
        boolean isLatestVersion =
            latestVersionId == null
                ? paymentVersionId == null
                : latestVersionId.equals(paymentVersionId);
        if (!isLatestVersion) {
            return ApiResponse.error(
                "Payment must be recorded against the latest billing version. Refresh the billing data and try again."
            );
        }

        // Validate note requirement: mandatory when payment leaves an outstanding balance
        // (null-safe: legacy rows may have a NULL paid_amount -> treat as zero).
        BigDecimal currentPaid = insuranceBilling.getPaidAmount() == null
            ? ZERO
            : insuranceBilling.getPaidAmount();
        BigDecimal candidatePaid = toMoney(
            currentPaid.add(input.amount())
        );
        if (
            candidatePaid.compareTo(
                insuranceBilling.getPatientPayableAmount()
            ) > 0
        ) {
            return ApiResponse.error(
                "Payment amount would exceed the patient payable amount."
            );
        }
        if (
            candidatePaid.compareTo(
                insuranceBilling.getPatientPayableAmount()
            ) < 0 &&
            !hasText(input.note())
        ) {
            return ApiResponse.error(
                "A billing note is required when the payment does not cover the full outstanding amount."
            );
        }

        BigDecimal nextPaid = toMoney(
            currentPaid.add(input.amount())
        );

        insuranceBilling.setPaidAmount(nextPaid);
        insuranceBilling.setOutstandingAmount(
            toMoney(
                insuranceBilling.getPatientPayableAmount().subtract(nextPaid)
            )
        );
        insuranceBilling.setStatus(
            paymentDistributor.resolveBillingStatus(
                nextPaid,
                insuranceBilling.getPatientPayableAmount()
            )
        );

        VisitDepartmentBilling departmentBilling =
            insuranceBilling.getVisitDepartmentBilling();
        VisitBillingPayment billingPayment = new VisitBillingPayment();
        billingPayment.setVisitDepartmentBilling(departmentBilling);
        billingPayment.setDepartmentInsuranceBilling(insuranceBilling);
        // S6 fix: populate the billing version on payments recorded outside the
        // initial billing flow so the payment audit trail is complete.
        billingPayment.setBillingVersion(insuranceBilling.getBillingVersion());
        billingPayment.setAmount(toMoney(input.amount()));
        billingPayment.setPaymentMethod(input.paymentMethod());
        billingPayment.setReference(input.reference());
        departmentBilling.getPayments().add(billingPayment);

        departmentInsuranceBillingRepository.save(insuranceBilling);
        BigDecimal totalAmount = ZERO;
        BigDecimal insuranceCoveredAmount = ZERO;
        BigDecimal patientPayableAmount = ZERO;
        BigDecimal paidAmount = ZERO;
        BigDecimal outstandingAmount = ZERO;

        for (DepartmentInsuranceBilling childBilling : departmentBilling.getInsuranceBillings()) {
            totalAmount = toMoney(
                totalAmount.add(childBilling.getTotalAmount())
            );
            insuranceCoveredAmount = toMoney(
                insuranceCoveredAmount.add(
                    childBilling.getInsuranceCoveredAmount()
                )
            );
            patientPayableAmount = toMoney(
                patientPayableAmount.add(childBilling.getPatientPayableAmount())
            );
            paidAmount = toMoney(paidAmount.add(childBilling.getPaidAmount()));
            outstandingAmount = toMoney(outstandingAmount.add(childBilling.getOutstandingAmount()));
        }

        departmentBilling.setTotalAmount(totalAmount);
        departmentBilling.setInsuranceCoveredAmount(insuranceCoveredAmount);
        departmentBilling.setPatientPayableAmount(patientPayableAmount);
        departmentBilling.setPaidAmount(paidAmount);
        departmentBilling.setOutstandingAmount(outstandingAmount);
        departmentBilling.setStatus(
            paymentDistributor.resolveBillingStatus(paidAmount, patientPayableAmount)
        );
        visitDepartmentBillingRepository.save(departmentBilling);

        // Persist billing note if provided
        if (hasText(input.note())) {
            VisitDepartment noteDept = departmentBilling.getVisitDepartment();
            VisitDepartmentNote billingNote = new VisitDepartmentNote();
            billingNote.setVisitDepartment(noteDept);
            billingNote.setContent(input.note().trim());
            billingNote.setCreatedBy(actingUser);
            billingNote.setNoteType(NoteType.BILLING);
            visitDepartmentNoteRepository.save(billingNote);
        }

        return ApiResponse.success(
            "Payment recorded.",
            billingDataMapper.visitBillingToMap(departmentBilling.getVisitBilling())
        );
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private Worker resolveWorker(AuthenticatedUser authUser) {
        if (authUser == null || authUser.userId() == null) {
            return null;
        }
        return workerRepository.findById(authUser.userId()).orElse(null);
    }
}
