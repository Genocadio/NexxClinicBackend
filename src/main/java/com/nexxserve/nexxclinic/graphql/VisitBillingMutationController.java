package com.nexxserve.nexxclinic.graphql;

import com.nexxserve.nexxclinic.auth.AuthenticatedUser;
import com.nexxserve.nexxclinic.graphql.input.BillVisitInput;
import com.nexxserve.nexxclinic.graphql.input.EditBillVisitInput;
import com.nexxserve.nexxclinic.graphql.input.RecordVisitBillingPaymentInput;
import com.nexxserve.nexxclinic.dto.out.ApiResponse;
import com.nexxserve.nexxclinic.model.RoleName;
import com.nexxserve.nexxclinic.security.HasRole;
import com.nexxserve.nexxclinic.service.VisitBillingService;
import com.nexxserve.nexxclinic.service.billing.InvoiceGenerator;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.ContextValue;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.stereotype.Controller;

@Controller
public class VisitBillingMutationController {

    private final VisitBillingService visitBillingService;
    private final InvoiceGenerator invoiceGenerator;

    public VisitBillingMutationController(
        VisitBillingService visitBillingService,
        InvoiceGenerator invoiceGenerator
    ) {
        this.visitBillingService = visitBillingService;
        this.invoiceGenerator = invoiceGenerator;
    }

    @HasRole({RoleName.ADMIN, RoleName.CLINIC_ADMIN, RoleName.RECEPTION, RoleName.NURSE, RoleName.CLINICIAN, RoleName.FINANCE})
    @MutationMapping
    public ApiResponse billVisit(
            @Argument @Valid BillVisitInput input,
            @ContextValue(name = "authUser", required = false) AuthenticatedUser authUser
    ) {
        return visitBillingService.billVisit(input, authUser);
    }

    @HasRole({RoleName.ADMIN, RoleName.CLINIC_ADMIN, RoleName.FINANCE})
    @MutationMapping
    public ApiResponse editBillVisit(
            @Argument @Valid EditBillVisitInput input,
            @ContextValue(name = "authUser", required = false) AuthenticatedUser authUser
    ) {
        return visitBillingService.editBillVisit(input, authUser);
    }

    @HasRole({RoleName.ADMIN, RoleName.CLINIC_ADMIN, RoleName.RECEPTION, RoleName.CLINICIAN, RoleName.FINANCE})
    @MutationMapping
    public ApiResponse recordVisitBillingPayment(
            @Argument @Valid RecordVisitBillingPaymentInput input,
            @ContextValue(name = "authUser", required = false) AuthenticatedUser authUser
    ) {
        return visitBillingService.recordVisitBillingPayment(input, authUser);
    }

    @HasRole({RoleName.ADMIN, RoleName.CLINIC_ADMIN, RoleName.RECEPTION, RoleName.CLINICIAN, RoleName.FINANCE})
    @MutationMapping
    public ApiResponse generateInvoice(
            @Argument("departmentInsuranceBillingId") UUID departmentInsuranceBillingId,
            @ContextValue(name = "authUser", required = false) AuthenticatedUser authUser
    ) {
        return invoiceGenerator.generateInvoice(departmentInsuranceBillingId, authUser);
    }

    @HasRole({RoleName.ADMIN, RoleName.CLINIC_ADMIN, RoleName.FINANCE})
    @MutationMapping
    public ApiResponse flushSoftDeletedVisitProducts(
            @Argument("visitId") UUID visitId,
            @ContextValue(name = "authUser", required = false) AuthenticatedUser authUser
    ) {
        return visitBillingService.flushSoftDeletedVisitProducts(visitId, authUser);
    }
}
