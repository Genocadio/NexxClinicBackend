package com.nexxserve.nexxclinic.graphql;

import com.nexxserve.nexxclinic.auth.AuthenticatedUser;
import com.nexxserve.nexxclinic.dto.out.ApiResponse;
import com.nexxserve.nexxclinic.model.RoleName;
import com.nexxserve.nexxclinic.security.HasRole;
import com.nexxserve.nexxclinic.service.VisitBillingService;
import java.util.UUID;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.ContextValue;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

@Controller
public class VisitBillingQueryController {

    private final VisitBillingService visitBillingService;

    public VisitBillingQueryController(VisitBillingService visitBillingService) {
        this.visitBillingService = visitBillingService;
    }

    @HasRole({RoleName.ADMIN, RoleName.CLINIC_ADMIN, RoleName.RECEPTION, RoleName.NURSE, RoleName.CLINICIAN, RoleName.FINANCE})
    @QueryMapping
    public ApiResponse visitBilling(
            @Argument("visitId") UUID visitId,
            @ContextValue(name = "authUser", required = false) AuthenticatedUser authUser
    ) {
        return visitBillingService.visitBilling(visitId);
    }

    @HasRole({RoleName.ADMIN, RoleName.CLINIC_ADMIN, RoleName.RECEPTION, RoleName.NURSE, RoleName.CLINICIAN, RoleName.FINANCE})
    @QueryMapping
    public ApiResponse getInvoice(
            @Argument("departmentInsuranceBillingId") UUID departmentInsuranceBillingId,
            @ContextValue(name = "authUser", required = false) AuthenticatedUser authUser
    ) {
        return visitBillingService.getInvoice(departmentInsuranceBillingId);
    }
}
