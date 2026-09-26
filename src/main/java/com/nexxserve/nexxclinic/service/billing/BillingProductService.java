package com.nexxserve.nexxclinic.service.billing;

import com.nexxserve.nexxclinic.auth.AuthenticatedUser;
import com.nexxserve.nexxclinic.dto.out.ApiResponse;
import com.nexxserve.nexxclinic.entity.DepartmentInsuranceBilling;
import com.nexxserve.nexxclinic.entity.PatientInsurance;
import com.nexxserve.nexxclinic.entity.Product;
import com.nexxserve.nexxclinic.entity.Visit;
import com.nexxserve.nexxclinic.entity.VisitBilling;
import com.nexxserve.nexxclinic.entity.VisitBillingItem;
import com.nexxserve.nexxclinic.entity.VisitDepartment;
import com.nexxserve.nexxclinic.entity.VisitDepartmentBilling;
import com.nexxserve.nexxclinic.entity.VisitDepartmentProduct;
import com.nexxserve.nexxclinic.entity.VisitInsurance;
import com.nexxserve.nexxclinic.entity.Worker;
import com.nexxserve.nexxclinic.model.BillingConfirmationStatus;
import com.nexxserve.nexxclinic.model.CoverageType;
import com.nexxserve.nexxclinic.model.ExemptionType;
import com.nexxserve.nexxclinic.model.PatientShareSource;
import com.nexxserve.nexxclinic.model.VisitBillingStatus;
import com.nexxserve.nexxclinic.model.VisitDepartmentProductSource;
import com.nexxserve.nexxclinic.model.VisitProductStatus;
import com.nexxserve.nexxclinic.model.VisitStatus;
import com.nexxserve.nexxclinic.repository.DepartmentInsuranceBillingRepository;
import com.nexxserve.nexxclinic.repository.ProductRepository;
import com.nexxserve.nexxclinic.repository.VisitBillingItemRepository;
import com.nexxserve.nexxclinic.repository.VisitBillingRepository;
import com.nexxserve.nexxclinic.repository.VisitDepartmentBillingRepository;
import com.nexxserve.nexxclinic.repository.VisitDepartmentProductRepository;
import com.nexxserve.nexxclinic.repository.VisitDepartmentRepository;
import com.nexxserve.nexxclinic.repository.VisitInsuranceRepository;
import com.nexxserve.nexxclinic.repository.VisitRepository;
import com.nexxserve.nexxclinic.repository.WorkerRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BillingProductService {

    private static final Logger log = LoggerFactory.getLogger(BillingProductService.class);

    private final VisitRepository visitRepository;
    private final VisitDepartmentRepository visitDepartmentRepository;
    private final ProductRepository productRepository;
    private final WorkerRepository workerRepository;
    private final VisitBillingRepository visitBillingRepository;
    private final VisitDepartmentBillingRepository visitDepartmentBillingRepository;
    private final DepartmentInsuranceBillingRepository departmentInsuranceBillingRepository;
    private final VisitBillingItemRepository visitBillingItemRepository;
    private final VisitDepartmentProductRepository visitDepartmentProductRepository;
    private final VisitInsuranceRepository visitInsuranceRepository;
    private final BillingPricingCalculator pricingCalculator;
    private final BillingDataMapper billingDataMapper;

    public BillingProductService(
            VisitRepository visitRepository,
            VisitDepartmentRepository visitDepartmentRepository,
            ProductRepository productRepository,
            WorkerRepository workerRepository,
            VisitBillingRepository visitBillingRepository,
            VisitDepartmentBillingRepository visitDepartmentBillingRepository,
            DepartmentInsuranceBillingRepository departmentInsuranceBillingRepository,
            VisitBillingItemRepository visitBillingItemRepository,
            VisitDepartmentProductRepository visitDepartmentProductRepository,
            VisitInsuranceRepository visitInsuranceRepository,
            BillingPricingCalculator pricingCalculator,
            BillingDataMapper billingDataMapper
    ) {
        this.visitRepository = visitRepository;
        this.visitDepartmentRepository = visitDepartmentRepository;
        this.productRepository = productRepository;
        this.workerRepository = workerRepository;
        this.visitBillingRepository = visitBillingRepository;
        this.visitDepartmentBillingRepository = visitDepartmentBillingRepository;
        this.departmentInsuranceBillingRepository = departmentInsuranceBillingRepository;
        this.visitBillingItemRepository = visitBillingItemRepository;
        this.visitDepartmentProductRepository = visitDepartmentProductRepository;
        this.visitInsuranceRepository = visitInsuranceRepository;
        this.pricingCalculator = pricingCalculator;
        this.billingDataMapper = billingDataMapper;
    }

    @Transactional
    public void syncClinicalProductChangeToBilling(VisitDepartmentProduct vdp) {
        if (vdp == null || vdp.getVisitDepartment() == null) {
            return;
        }

        VisitDepartment rootDept = vdp.getVisitDepartment().getParentVisitDepartment() != null
                ? vdp.getVisitDepartment().getParentVisitDepartment()
                : vdp.getVisitDepartment();

        List<VisitDepartmentBilling> deptBillings = visitDepartmentBillingRepository.findByVisitDepartmentId(rootDept.getId());
        if (deptBillings.isEmpty()) {
            return; // Billing has not been created yet for this department
        }

        VisitDepartmentBilling deptBilling = deptBillings.get(0);
        List<VisitBillingItem> items = visitBillingItemRepository.findByVisitDepartmentProductId(vdp.getId());

        if (!items.isEmpty()) {
            VisitBillingItem item = items.get(0);
            if (vdp.isDeleted()) {
                item.setQuantitySnapshot(MoneyUtils.ZERO);
                item.setLineTotal(MoneyUtils.ZERO);
                item.setInsuranceCoveredAmount(MoneyUtils.ZERO);
                item.setPatientPayableAmount(MoneyUtils.ZERO);
            } else {
                BigDecimal qty = MoneyUtils.toQuantity(vdp.getQuantity());
                item.setQuantitySnapshot(qty);
                BigDecimal lineTotal = MoneyUtils.toMoney(item.getUnitPriceSnapshot().multiply(qty));
                item.setLineTotal(lineTotal);

                if (item.getAppliedPatientInsurance() != null && item.getAppliedPatientSharePct() != null) {
                    int pct = item.getAppliedPatientSharePct();
                    BigDecimal covered = MoneyUtils.ZERO;
                    if (pct < 100) {
                        BigDecimal insPct = BigDecimal.valueOf(100 - pct);
                        covered = MoneyUtils.toMoney(lineTotal.multiply(insPct).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP));
                    }
                    item.setInsuranceCoveredAmount(covered);
                    item.setPatientPayableAmount(MoneyUtils.toMoney(lineTotal.subtract(covered)));
                } else {
                    item.setPatientPayableAmount(lineTotal);
                }
            }
            visitBillingItemRepository.saveAndFlush(item);
            recalculateDepartmentBilling(deptBilling);
        } else if (!vdp.isDeleted()) {
            // New clinical product added while billing exists -> auto-bill it into default bucket
            List<DepartmentInsuranceBilling> buckets = departmentInsuranceBillingRepository.findByVisitDepartmentBillingId(deptBilling.getId());
            DepartmentInsuranceBilling bucket = buckets.isEmpty() ? null : buckets.get(0);

            if (bucket != null) {
                BigDecimal unitPrice = pricingCalculator.resolveDefaultUnitPrice(vdp, bucket.getPatientInsurance());
                BigDecimal qty = MoneyUtils.toQuantity(vdp.getQuantity());
                BigDecimal lineTotal = MoneyUtils.toMoney(unitPrice.multiply(qty));

                VisitBillingItem newItem = new VisitBillingItem();
                newItem.setDepartmentInsuranceBilling(bucket);
                newItem.setVisitDepartmentProduct(vdp);
                newItem.setAppliedPatientInsurance(bucket.getPatientInsurance());
                newItem.setUnitPriceSnapshot(unitPrice);
                newItem.setQuantitySnapshot(qty);
                newItem.setLineTotal(lineTotal);
                newItem.setInsuranceCoveredAmount(MoneyUtils.ZERO);
                newItem.setPatientPayableAmount(lineTotal);
                newItem.setAppliedPatientSharePct(100);
                newItem.setPatientShareSource(PatientShareSource.PROVIDER_DEFAULT);

                visitBillingItemRepository.saveAndFlush(newItem);
                recalculateDepartmentBilling(deptBilling);
            }
        }
    }

    @Transactional
    public void syncBilledProductsToVisitDepartments(UUID visitId) {
        if (visitId == null) {
            return;
        }

        List<VisitBilling> billings = visitBillingRepository.findByVisitIdOrderByCreatedAtDesc(visitId);
        if (billings.isEmpty()) {
            return;
        }

        VisitBilling latestBilling = billings.get(0);
        if (latestBilling.getDepartments() == null || latestBilling.getDepartments().isEmpty()) {
            return;
        }

        for (VisitDepartmentBilling deptBilling : latestBilling.getDepartments()) {
            if (deptBilling == null || deptBilling.getInsuranceBillings() == null) {
                continue;
            }

            for (DepartmentInsuranceBilling insBilling : deptBilling.getInsuranceBillings()) {
                if (insBilling == null || insBilling.getItems() == null) {
                    continue;
                }

                for (VisitBillingItem item : insBilling.getItems()) {
                    if (item == null) {
                        continue;
                    }

                    VisitDepartmentProduct vdp = item.getVisitDepartmentProduct();
                    BigDecimal qtySnapshot = item.getQuantitySnapshot() != null ? item.getQuantitySnapshot() : MoneyUtils.ZERO;

                    if (vdp != null) {
                        boolean changed = false;
                        if (qtySnapshot.compareTo(BigDecimal.ZERO) > 0) {
                            if (vdp.isDeleted()) {
                                vdp.setDeleted(false);
                                changed = true;
                            }
                            if (vdp.getQuantity() == null || vdp.getQuantity().compareTo(qtySnapshot) != 0) {
                                vdp.setQuantity(qtySnapshot);
                                changed = true;
                            }
                            VisitProductStatus expectedStatus = resolveBilledProductStatus(item);
                            if (vdp.getStatus() == null || vdp.getStatus() == VisitProductStatus.PENDING) {
                                vdp.setStatus(expectedStatus);
                                changed = true;
                            }
                            if (vdp.getBillingConfirmationStatus() == null) {
                                vdp.setBillingConfirmationStatus(BillingConfirmationStatus.PENDING_OPERATOR_CONFIRMATION);
                                changed = true;
                            }
                        } else {
                            if (!vdp.isDeleted()) {
                                vdp.setDeleted(true);
                                changed = true;
                            }
                        }

                        if (changed) {
                            visitDepartmentProductRepository.saveAndFlush(vdp);
                        }
                    }
                }
            }
        }
    }

    private VisitProductStatus resolveBilledProductStatus(VisitBillingItem item) {
        if (item == null) {
            return VisitProductStatus.BILLED;
        }
        if (item.getPatientShareSource() == PatientShareSource.EXEMPTED
                || (item.getLineTotal() != null && item.getLineTotal().compareTo(BigDecimal.ZERO) == 0)) {
            return VisitProductStatus.EXEMPTED;
        }
        if (item.getPatientPayableAmount() != null && item.getPatientPayableAmount().compareTo(BigDecimal.ZERO) == 0
                && item.getInsuranceCoveredAmount() != null && item.getInsuranceCoveredAmount().compareTo(BigDecimal.ZERO) > 0) {
            return VisitProductStatus.PATIENT_SHARE_EXEMPTED;
        }
        return VisitProductStatus.BILLED;
    }

    private void recalculateDepartmentBilling(VisitDepartmentBilling deptBilling) {
        BigDecimal deptTotal = MoneyUtils.ZERO;
        BigDecimal deptInsurance = MoneyUtils.ZERO;
        BigDecimal deptPatient = MoneyUtils.ZERO;

        List<DepartmentInsuranceBilling> buckets = departmentInsuranceBillingRepository.findByVisitDepartmentBillingId(deptBilling.getId());
        for (DepartmentInsuranceBilling bucket : buckets) {
            BigDecimal bucketTotal = MoneyUtils.ZERO;
            BigDecimal bucketInsurance = MoneyUtils.ZERO;
            BigDecimal bucketPatient = MoneyUtils.ZERO;

            List<VisitBillingItem> items = visitBillingItemRepository.findByDepartmentInsuranceBillingId(bucket.getId());
            for (VisitBillingItem it : items) {
                bucketTotal = MoneyUtils.toMoney(bucketTotal.add(it.getLineTotal()));
                bucketInsurance = MoneyUtils.toMoney(bucketInsurance.add(it.getInsuranceCoveredAmount()));
                bucketPatient = MoneyUtils.toMoney(bucketPatient.add(it.getPatientPayableAmount()));
            }

            bucket.setTotalAmount(bucketTotal);
            bucket.setInsuranceCoveredAmount(bucketInsurance);
            bucket.setPatientPayableAmount(bucketPatient);
            BigDecimal bucketPaid = bucket.getPaidAmount() != null ? bucket.getPaidAmount() : MoneyUtils.ZERO;
            bucket.setOutstandingAmount(MoneyUtils.toMoney(bucketPatient.subtract(bucketPaid)));

            if (bucketPaid.compareTo(MoneyUtils.ZERO) > 0 && bucketPaid.compareTo(bucketPatient) >= 0) {
                bucket.setStatus(VisitBillingStatus.PAID);
            } else if (bucketPaid.compareTo(MoneyUtils.ZERO) > 0) {
                bucket.setStatus(VisitBillingStatus.PARTIALLY_PAID);
            } else {
                bucket.setStatus(VisitBillingStatus.UNPAID);
            }
            departmentInsuranceBillingRepository.saveAndFlush(bucket);

            deptTotal = MoneyUtils.toMoney(deptTotal.add(bucketTotal));
            deptInsurance = MoneyUtils.toMoney(deptInsurance.add(bucketInsurance));
            deptPatient = MoneyUtils.toMoney(deptPatient.add(bucketPatient));
        }

        deptBilling.setTotalAmount(deptTotal);
        deptBilling.setInsuranceCoveredAmount(deptInsurance);
        deptBilling.setPatientPayableAmount(deptPatient);
        BigDecimal deptPaid = deptBilling.getPaidAmount() != null ? deptBilling.getPaidAmount() : MoneyUtils.ZERO;
        deptBilling.setOutstandingAmount(MoneyUtils.toMoney(deptPatient.subtract(deptPaid)));

        if (deptPaid.compareTo(MoneyUtils.ZERO) > 0 && deptPaid.compareTo(deptPatient) >= 0) {
            deptBilling.setStatus(VisitBillingStatus.PAID);
        } else if (deptPaid.compareTo(MoneyUtils.ZERO) > 0) {
            deptBilling.setStatus(VisitBillingStatus.PARTIALLY_PAID);
        } else {
            deptBilling.setStatus(VisitBillingStatus.UNPAID);
        }
        visitDepartmentBillingRepository.saveAndFlush(deptBilling);
    }
}
