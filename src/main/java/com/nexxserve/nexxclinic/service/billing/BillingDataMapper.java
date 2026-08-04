package com.nexxserve.nexxclinic.service.billing;

import com.nexxserve.nexxclinic.entity.Department;
import com.nexxserve.nexxclinic.entity.DepartmentInsuranceBilling;
import com.nexxserve.nexxclinic.entity.PatientInsurance;
import com.nexxserve.nexxclinic.entity.VisitBilling;
import com.nexxserve.nexxclinic.entity.VisitBillingItem;
import com.nexxserve.nexxclinic.entity.VisitBillingPayment;
import com.nexxserve.nexxclinic.entity.VisitDepartment;
import com.nexxserve.nexxclinic.entity.VisitDepartmentBilling;
import java.util.HashMap;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * Converts billing entities into the map-shaped responses returned to the
 * GraphQL client. Shared by the billing orchestration and the invoice generator
 * so one mapping definition drives every response.
 */
@Component
public class BillingDataMapper {

    public Map<String, Object> visitBillingToMap(VisitBilling billing) {
        Map<String, Object> data = new HashMap<>();
        data.put("id", billing.getId());
        data.put("visitId", billing.getVisit().getId());
        if (billing.getBillingVersion() != null) {
            Map<String, Object> versionData = new HashMap<>();
            versionData.put("id", billing.getBillingVersion().getId());
            versionData.put("version", billing.getBillingVersion().getVersion());
            data.put("version", versionData);
        }
        data.put("createdAt", billing.getCreatedAt());
        data.put("updatedAt", billing.getUpdatedAt());
        data.put(
            "departments",
            billing
                .getDepartments()
                .stream()
                .map(this::visitDepartmentBillingToMap)
                .toList()
        );
        return data;
    }

    private Map<String, Object> visitDepartmentBillingToMap(
        VisitDepartmentBilling billing
    ) {
        Map<String, Object> data = new HashMap<>();
        data.put("id", billing.getId());
        data.put(
            "visitDepartment",
            visitDepartmentToMap(billing.getVisitDepartment())
        );
        data.put("status", billing.getStatus());
        data.put("totalAmount", billing.getTotalAmount());
        data.put("insuranceCoveredAmount", billing.getInsuranceCoveredAmount());
        data.put("patientPayableAmount", billing.getPatientPayableAmount());
        data.put("paidAmount", billing.getPaidAmount());
        data.put("outstandingAmount", billing.getOutstandingAmount());
        data.put(
            "payments",
            billing
                .getPayments()
                .stream()
                .map(this::visitBillingPaymentToMap)
                .toList()
        );
        data.put(
            "insuranceBillings",
            billing
                .getInsuranceBillings()
                .stream()
                .map(this::departmentInsuranceBillingToMap)
                .toList()
        );
        data.put("createdAt", billing.getCreatedAt());
        data.put("updatedAt", billing.getUpdatedAt());
        return data;
    }

    private Map<String, Object> departmentInsuranceBillingToMap(
        DepartmentInsuranceBilling billing
    ) {
        Map<String, Object> data = new HashMap<>();
        data.put("id", billing.getId());
        data.put(
            "patientInsurance",
            billing.getPatientInsurance() == null
                ? null
                : patientInsuranceToMap(billing.getPatientInsurance())
        );
        data.put("status", billing.getStatus());
        data.put("totalAmount", billing.getTotalAmount());
        data.put("insuranceCoveredAmount", billing.getInsuranceCoveredAmount());
        data.put("patientPayableAmount", billing.getPatientPayableAmount());
        data.put("paidAmount", billing.getPaidAmount());
        data.put("outstandingAmount", billing.getOutstandingAmount());
        data.put(
            "items",
            billing
                .getItems()
                .stream()
                .map(this::visitBillingItemToMap)
                .toList()
        );
        data.put("createdAt", billing.getCreatedAt());
        data.put("updatedAt", billing.getUpdatedAt());
        return data;
    }

    private Map<String, Object> visitDepartmentToMap(
        VisitDepartment visitDepartment
    ) {
        if (visitDepartment == null) {
            return null;
        }
        Map<String, Object> data = new HashMap<>();
        data.put("id", visitDepartment.getId());
        data.put(
            "department",
            departmentToMap(visitDepartment.getDepartment())
        );
        data.put("status", visitDepartment.getStatus());
        data.put("createdAt", visitDepartment.getCreatedAt());
        data.put("updatedAt", visitDepartment.getUpdatedAt());
        return data;
    }

    private Map<String, Object> departmentToMap(Department department) {
        if (department == null) {
            return null;
        }
        Map<String, Object> data = new HashMap<>();
        data.put("id", department.getId());
        data.put("name", department.getName());
        return data;
    }

    private Map<String, Object> patientInsuranceToMap(
        PatientInsurance insurance
    ) {
        if (insurance == null) {
            return null;
        }
        Map<String, Object> data = new HashMap<>();
        data.put("id", insurance.getId());
        data.put(
            "insuranceProviderId",
            insurance.getInsuranceProvider() == null
                ? null
                : insurance.getInsuranceProvider().getId()
        );
        data.put("insuranceCardNumber", insurance.getInsuranceCardNumber());
        data.put("principalMemberName", insurance.getPrincipalMemberName());
        return data;
    }

    /**
     * Public because the invoice generator also builds item lists for the PDF
     * renderer from the same projection.
     */
    public Map<String, Object> visitBillingItemToMap(VisitBillingItem item) {
        Map<String, Object> data = new HashMap<>();
        data.put("id", item.getId());
        data.put(
            "visitDepartmentProductId",
            item.getVisitDepartmentProduct().getId()
        );
        data.put(
            "productId",
            item.getVisitDepartmentProduct().getProduct().getId()
        );
        data.put(
            "productName",
            item.getVisitDepartmentProduct().getProduct().getName()
        );
        data.put("unitPriceSnapshot", item.getUnitPriceSnapshot());
        data.put("quantitySnapshot", item.getQuantitySnapshot());
        data.put("lineTotal", item.getLineTotal());
        data.put("insuranceCoveredAmount", item.getInsuranceCoveredAmount());
        data.put("patientPayableAmount", item.getPatientPayableAmount());
        data.put("createdAt", item.getCreatedAt());
        data.put("updatedAt", item.getUpdatedAt());
        return data;
    }

    private Map<String, Object> visitBillingPaymentToMap(
        VisitBillingPayment payment
    ) {
        Map<String, Object> data = new HashMap<>();
        data.put("id", payment.getId());
        data.put("amount", payment.getAmount());
        data.put("paymentMethod", payment.getPaymentMethod());
        data.put("reference", payment.getReference());
        data.put("createdAt", payment.getCreatedAt());
        data.put("updatedAt", payment.getUpdatedAt());
        return data;
    }
}
