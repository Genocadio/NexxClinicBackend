package com.nexxserve.nexxclinic.service.billing;

import com.nexxserve.nexxclinic.entity.DepartmentInsuranceBilling;
import com.nexxserve.nexxclinic.entity.VisitBillingPayment;
import com.nexxserve.nexxclinic.entity.VisitDepartmentBilling;
import com.nexxserve.nexxclinic.graphql.input.BillVisitInput;
import com.nexxserve.nexxclinic.model.VisitBillingStatus;
import java.math.BigDecimal;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * Owns how submitted payments are turned into bucket-level payment records:
 * distributing a department's total payment across its insurance buckets while
 * preserving each payment's original method and reference, and deriving billing
 * statuses from paid/payable amounts.
 */
@Component
public class BillingPaymentDistributor {

    /**
     * Builds a per-root-department FIFO queue of submitted payments. A single
     * submitted payment may be split across several buckets, but every
     * bucket-level record keeps that payment's own method and reference.
     */
    public Map<UUID, ArrayDeque<BillVisitInput.BillingPaymentInput>> buildPaymentQueues(
        Map<UUID, List<BillVisitInput.BillingPaymentInput>> rootPaymentsByDepartment
    ) {
        Map<UUID, ArrayDeque<BillVisitInput.BillingPaymentInput>> queues = new HashMap<>();
        for (Map.Entry<UUID, List<BillVisitInput.BillingPaymentInput>> e
                : rootPaymentsByDepartment.entrySet()) {
            if (e.getValue() != null && !e.getValue().isEmpty()) {
                queues.put(e.getKey(), new ArrayDeque<>(e.getValue()));
            }
        }
        return queues;
    }

    /**
     * Sums the submitted payment amounts for a department (already validated).
     */
    public BigDecimal totalPayments(List<BillVisitInput.BillingPaymentInput> payments) {
        BigDecimal total = MoneyUtils.ZERO;
        if (payments != null) {
            for (BillVisitInput.BillingPaymentInput payment : payments) {
                if (payment != null && payment.amount() != null) {
                    total = MoneyUtils.toMoney(total.add(payment.amount()));
                }
            }
        }
        return total;
    }

    /**
     * Distributes {@code paidAmount} for one insurance bucket by consuming the
     * department's submitted payments in order. A single submitted payment may be
     * split across several buckets; every resulting bucket-level record preserves
     * that payment's own method and reference. (Legacy paid amounts recorded
     * without payment rows simply allocate the amount without creating a payment
     * record.)
     */
    public void allocatePaymentsToBucket(
        ArrayDeque<BillVisitInput.BillingPaymentInput> queue,
        VisitDepartmentBilling departmentBilling,
        DepartmentInsuranceBilling insuranceBilling,
        BigDecimal paidAmount
    ) {
        BigDecimal toApply = MoneyUtils.toMoney(paidAmount);
        while (toApply.compareTo(MoneyUtils.ZERO) > 0 && queue != null && !queue.isEmpty()) {
            BillVisitInput.BillingPaymentInput payment = queue.peek();
            // Defensively skip invalid legacy payment rows (missing method or a
            // non-positive amount) instead of persisting a row that would violate the
            // NOT NULL constraint or corrupt the audit trail.
            if (
                payment == null ||
                payment.amount() == null ||
                payment.amount().compareTo(MoneyUtils.ZERO) <= 0 ||
                payment.paymentMethod() == null
            ) {
                queue.poll();
                continue;
            }
            BigDecimal available = MoneyUtils.toMoney(payment.amount());
            BigDecimal take = available.compareTo(toApply) >= 0 ? toApply : available;

            VisitBillingPayment bucketPayment = new VisitBillingPayment();
            bucketPayment.setVisitDepartmentBilling(departmentBilling);
            bucketPayment.setDepartmentInsuranceBilling(insuranceBilling);
            bucketPayment.setAmount(take);
            bucketPayment.setPaymentMethod(payment.paymentMethod());
            bucketPayment.setReference(payment.reference());
            departmentBilling.getPayments().add(bucketPayment);

            toApply = MoneyUtils.toMoney(toApply.subtract(take));
            queue.poll();
            if (available.compareTo(take) > 0) {
                queue.addFirst(new BillVisitInput.BillingPaymentInput(
                    MoneyUtils.toMoney(available.subtract(take)),
                    payment.paymentMethod(),
                    payment.reference()
                ));
            }
        }
    }

    /**
     * Derives the billing status for a bucket or department from how much has been
     * paid versus how much the patient must pay.
     */
    public VisitBillingStatus resolveBillingStatus(
        BigDecimal paidAmount,
        BigDecimal patientPayableAmount
    ) {
        if (patientPayableAmount.compareTo(MoneyUtils.ZERO) == 0) {
            return VisitBillingStatus.PAID;
        }
        if (paidAmount.compareTo(MoneyUtils.ZERO) == 0) {
            return VisitBillingStatus.UNPAID;
        }
        if (paidAmount.compareTo(patientPayableAmount) >= 0) {
            return VisitBillingStatus.PAID;
        }
        return VisitBillingStatus.PARTIALLY_PAID;
    }
}
