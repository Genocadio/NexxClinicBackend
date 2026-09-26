package com.nexxserve.nexxclinic.graphql;

import com.nexxserve.nexxclinic.auth.AuthenticatedUser;
import com.nexxserve.nexxclinic.dto.out.ApiResponse;
import com.nexxserve.nexxclinic.model.RoleName;
import com.nexxserve.nexxclinic.security.HasRole;
import com.nexxserve.nexxclinic.service.VisitBillingService;
import com.nexxserve.nexxclinic.dto.out.VisitDepartmentDto;
import com.nexxserve.nexxclinic.dto.out.VisitDepartmentProductDto;
import com.nexxserve.nexxclinic.service.billing.BillingQueryService;
import java.util.Map;
import java.util.UUID;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.ContextValue;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.graphql.data.method.annotation.SchemaMapping;
import org.springframework.stereotype.Controller;

@Controller
public class VisitBillingQueryController {

    private final VisitBillingService visitBillingService;
    private final BillingQueryService billingQueryService;

    public VisitBillingQueryController(
        VisitBillingService visitBillingService,
        BillingQueryService billingQueryService
    ) {
        this.visitBillingService = visitBillingService;
        this.billingQueryService = billingQueryService;
    }

    @HasRole({
        RoleName.ADMIN,
        RoleName.CLINIC_ADMIN,
        RoleName.RECEPTION,
        RoleName.NURSE,
        RoleName.CLINICIAN,
        RoleName.FINANCE,
    })
    @QueryMapping
    public ApiResponse visitBilling(
        @Argument("visitId") UUID visitId,
        @ContextValue(
            name = "authUser",
            required = false
        ) AuthenticatedUser authUser
    ) {
        return visitBillingService.visitBilling(visitId);
    }

    @HasRole({
        RoleName.ADMIN,
        RoleName.CLINIC_ADMIN,
        RoleName.RECEPTION,
        RoleName.NURSE,
        RoleName.CLINICIAN,
        RoleName.FINANCE,
    })
    @QueryMapping
    public ApiResponse getVisitDepartmentBilling(
        @Argument("visitDepartmentId") UUID visitDepartmentId,
        @ContextValue(
            name = "authUser",
            required = false
        ) AuthenticatedUser authUser
    ) {
        return billingQueryService.getVisitDepartmentBilling(visitDepartmentId);
    }

    @SchemaMapping(typeName = "VisitDepartment", field = "billing")
    public Map<String, Object> departmentBilling(VisitDepartmentDto visitDepartment) {
        if (visitDepartment == null || visitDepartment.id() == null) {
            return null;
        }
        return billingQueryService.departmentBilling(visitDepartment.id());
    }

    @SchemaMapping(typeName = "VisitDepartmentProduct", field = "billingItem")
    public Map<String, Object> productBillingItem(VisitDepartmentProductDto product) {
        if (product == null || product.id() == null) {
            return null;
        }
        return billingQueryService.productBillingItem(product.id());
    }
}
