package com.nexxserve.nexxclinic.service.billing;

import com.nexxserve.nexxclinic.entity.DepartmentInsuranceBilling;
import com.nexxserve.nexxclinic.entity.VisitBilling;
import com.nexxserve.nexxclinic.entity.VisitDepartmentBilling;
import com.nexxserve.nexxclinic.entity.VisitDepartmentProduct;
import com.nexxserve.nexxclinic.entity.billing.VisitBillingVersion;
import com.nexxserve.nexxclinic.graphql.input.BillVisitInput;
import com.nexxserve.nexxclinic.model.CoverageType;
import com.nexxserve.nexxclinic.model.ExemptionType;
import com.nexxserve.nexxclinic.model.VisitProductStatus;
import com.nexxserve.nexxclinic.repository.VisitBillingRepository;
import com.nexxserve.nexxclinic.repository.VisitBillingVersionRepository;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import static com.nexxserve.nexxclinic.service.billing.MoneyUtils.ZERO;
import static com.nexxserve.nexxclinic.service.billing.MoneyUtils.toMoney;

/**
 * Handles carrying forward previously billed departments into a new billing
 * version during incremental {@code billVisit} / {@code editBillVisit}.
 *
 * <p>When a visit is billed incrementally (e.g. Dept A first, Dept B later),
 * the previously billed departments must appear in the new version so the
 * authoritative latest billing view never silently drops them. This component
 * builds the carried-forward department inputs and feeds their payments into
 * the distribution path so the re-billed department keeps its original method
 * and reference.
 *
 * <p>Extracted from {@code VisitBillingService} to isolate the E2
 * carry-forward logic from the billing orchestration.
 */
@Component
public class BillingCarryForwardService {

    private static final Logger log = LoggerFactory.getLogger(BillingCarryForwardService.class);

    private final VisitBillingVersionRepository visitBillingVersionRepository;
    private final VisitBillingRepository visitBillingRepository;

    public BillingCarryForwardService(
        VisitBillingVersionRepository visitBillingVersionRepository,
        VisitBillingRepository visitBillingRepository
    ) {
        this.visitBillingVersionRepository = visitBillingVersionRepository;
        this.visitBillingRepository = visitBillingRepository;
    }

    /**
     * Holds the results of the carry-forward merge.
     *
     * @param departmentsToProcess the merged list of requested + carried-forward departments
     * @param carriedDepartmentIds the IDs of departments that were carried forward (excluded from fresh-balance note rule)
     * @param rootDepartments      map of department ID → VisitDepartment (may be updated with carried-forward entries)
     * @param rootPaymentsByDepartment map of department ID → payments (may be updated with carried-forward payments)
     * @param remainingPaidByDepartment map of department ID → remaining paid amount (may be updated)
     * @param previousBilling      the previous billing version (for snapshot building)
     */
    public record CarryForwardResult(
        List<BillVisitInput.BillVisitDepartmentInput> departmentsToProcess,
        Set<UUID> carriedDepartmentIds,
        Map<UUID, com.nexxserve.nexxclinic.entity.VisitDepartment> rootDepartments,
        Map<UUID, List<BillVisitInput.BillingPaymentInput>> rootPaymentsByDepartment,
        Map<UUID, BigDecimal> remainingPaidByDepartment,
        VisitBilling previousBilling
    ) {}

    /**
     * Resolves the previous billing version for the visit and returns it.
     * Returns {@code null} when no previous billing exists.
     */
    public VisitBilling resolvePreviousBilling(UUID visitId) {
        List<VisitBilling> existingBillings = visitBillingRepository.findByVisitIdOrderByCreatedAtDesc(visitId);
        List<VisitBilling> ordered = existingBillings.stream()
            .sorted(java.util.Comparator
                .comparingInt((VisitBilling billing) ->
                    billing.getBillingVersion() == null ? 0 : billing.getBillingVersion().getVersion())
                .reversed()
                .thenComparing(
                    VisitBilling::getCreatedAt,
                    java.util.Comparator.nullsLast(java.util.Comparator.reverseOrder())))
            .toList();
        return ordered.isEmpty() ? null : ordered.get(0);
    }

    /**
     * Merges the previous billing version's departments into the current billing
     * request. Departments from the previous version that are NOT in the current
     * request and still have billable products are carried forward with their
     * original products, payments, and quantities.
     *
     * <p>Returns a {@link CarryForwardResult} containing the merged department
     * list and updated payment maps.
     */
    public CarryForwardResult mergePreviousDepartments(
        BillVisitInput input,
        VisitBilling previousBilling,
        Map<UUID, VisitDepartmentProduct> allProductsById,
        Map<UUID, com.nexxserve.nexxclinic.entity.VisitDepartment> rootDepartments,
        Map<UUID, List<BillVisitInput.BillingPaymentInput>> rootPaymentsByDepartment,
        Map<UUID, BigDecimal> remainingPaidByDepartment
    ) {
        Set<UUID> requestedDeptIds = input.departments().stream()
            .map(BillVisitInput.BillVisitDepartmentInput::visitDepartmentId)
            .collect(java.util.stream.Collectors.toSet());

        Set<UUID> carriedDepartmentIds = new HashSet<>();
        List<BillVisitInput.BillVisitDepartmentInput> carriedDepartments = new ArrayList<>();
        List<BillVisitInput.BillVisitDepartmentInput> mergedRequestedDepartments = new ArrayList<>();

        for (VisitDepartmentBilling prevDeptBilling : previousBilling.getDepartments()) {
            if (prevDeptBilling == null || prevDeptBilling.getVisitDepartment() == null) {
                continue;
            }
            UUID deptId = prevDeptBilling.getVisitDepartment().getId();

            List<BillVisitInput.BillVisitDepartmentProductInput> products =
                (prevDeptBilling.getInsuranceBillings() == null
                    ? java.util.List.<DepartmentInsuranceBilling>of()
                    : prevDeptBilling.getInsuranceBillings()).stream()
                    .filter(ib -> ib != null)
                    .flatMap(ib -> ib.getItems().stream())
                    .filter(item -> item != null && item.getVisitDepartmentProduct() != null)
                    .filter(item -> allProductsById.containsKey(item.getVisitDepartmentProduct().getId()))
                    .map(item -> {
                        UUID carriedInsuranceId =
                            item.getAppliedPatientInsurance() != null
                                ? item.getAppliedPatientInsurance().getId()
                                : null;
                        return new BillVisitInput.BillVisitDepartmentProductInput(
                            item.getVisitDepartmentProduct().getId(),
                            null,
                            item.getQuantitySnapshot(),
                            carriedInsuranceId != null ? CoverageType.INSURANCE : CoverageType.PRIVATE,
                            carriedInsuranceId,
                            resolveExemptionTypeFromStatus(
                                item.getVisitDepartmentProduct().getStatus()
                            ),
                            null
                        );
                    })
                    .toList();

            if (requestedDeptIds.contains(deptId)) {
                BillVisitInput.BillVisitDepartmentInput requested = input.departments().stream()
                    .filter(department -> deptId.equals(department.visitDepartmentId()))
                    .findFirst()
                    .orElse(null);
                if (requested != null && !products.isEmpty()) {
                    Set<UUID> requestedProductIds = requested.products() == null
                        ? Set.of()
                        : requested.products().stream()
                            .filter(product -> product != null)
                            .map(BillVisitInput.BillVisitDepartmentProductInput::visitDepartmentProductId)
                            .collect(java.util.stream.Collectors.toSet());
                    List<BillVisitInput.BillVisitDepartmentProductInput> mergedProducts =
                        new ArrayList<>(requested.products() == null ? List.of() : requested.products());
                    products.stream()
                        .filter(product -> !requestedProductIds.contains(product.visitDepartmentProductId()))
                        .forEach(mergedProducts::add);
                    mergedRequestedDepartments.add(new BillVisitInput.BillVisitDepartmentInput(
                        requested.visitDepartmentId(),
                        mergedProducts,
                        requested.payments(),
                        requested.note(),
                        requested.outstandingType(),
                        requested.outstandingReason()
                    ));
                }
                continue;
            }

            // Skip departments whose billed products were all removed by a later
            // edit — they have nothing to carry forward.
            if (products.isEmpty()) {
                continue;
            }
            log.info("Carrying forward previously billed department {} to the new billing version.", deptId);

            List<BillVisitInput.BillingPaymentInput> payments =
                prevDeptBilling.getPayments() == null
                    ? List.of()
                    : prevDeptBilling.getPayments().stream()
                        // Keep only rows that are actually recordable, so the total
                        // we distribute never exceeds what the queue can record.
                        .filter(p -> p != null && p.getAmount() != null
                            && p.getAmount().compareTo(ZERO) > 0
                            && p.getPaymentMethod() != null)
                        .map(p -> new BillVisitInput.BillingPaymentInput(
                            p.getAmount(),
                            p.getPaymentMethod(),
                            p.getReference()
                        ))
                        .toList();

            carriedDepartments.add(new BillVisitInput.BillVisitDepartmentInput(
                deptId,
                products,
                payments,
                null,
                null,
                null
            ));
            carriedDepartmentIds.add(deptId);

            // Feed the carried payments into the same distribution path as explicit
            // payments so the re-billed department keeps its original method and
            // reference instead of being re-created as CASH.
            rootPaymentsByDepartment.put(deptId, payments);
            BigDecimal paymentsTotal = ZERO;
            for (BillVisitInput.BillingPaymentInput payment : payments) {
                paymentsTotal = toMoney(paymentsTotal.add(payment.amount()));
            }
            BigDecimal carriedPaid = prevDeptBilling.getPaidAmount() == null
                ? ZERO
                : prevDeptBilling.getPaidAmount();
            remainingPaidByDepartment.put(
                deptId,
                paymentsTotal.compareTo(ZERO) > 0 ? paymentsTotal : carriedPaid
            );
            rootDepartments.put(deptId, prevDeptBilling.getVisitDepartment());
        }

        List<BillVisitInput.BillVisitDepartmentInput> departmentsToProcess;
        if (!mergedRequestedDepartments.isEmpty()) {
            Map<UUID, BillVisitInput.BillVisitDepartmentInput> mergedByDepartment =
                mergedRequestedDepartments.stream()
                    .collect(java.util.stream.Collectors.toMap(
                        BillVisitInput.BillVisitDepartmentInput::visitDepartmentId,
                        department -> department
                    ));
            departmentsToProcess = input.departments().stream()
                .map(department -> mergedByDepartment.getOrDefault(
                    department.visitDepartmentId(),
                    department
                ))
                .collect(java.util.stream.Collectors.toCollection(ArrayList::new));
        } else {
            departmentsToProcess = new ArrayList<>(input.departments());
        }
        if (!carriedDepartments.isEmpty()) {
            departmentsToProcess.addAll(carriedDepartments);
        }

        return new CarryForwardResult(
            departmentsToProcess,
            carriedDepartmentIds,
            rootDepartments,
            rootPaymentsByDepartment,
            remainingPaidByDepartment,
            previousBilling
        );
    }

    private ExemptionType resolveExemptionTypeFromStatus(VisitProductStatus status) {
        if (status == VisitProductStatus.EXEMPTED) {
            return ExemptionType.FULL;
        }
        if (status == VisitProductStatus.PATIENT_SHARE_EXEMPTED) {
            return ExemptionType.PATIENT_SHARE;
        }
        return ExemptionType.NONE;
    }
}
