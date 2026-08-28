package com.nexxserve.nexxclinic.graphql;

import com.nexxserve.nexxclinic.auth.AuthenticatedUser;
import com.nexxserve.nexxclinic.graphql.input.SearchPatientHistoryInput;
import com.nexxserve.nexxclinic.graphql.input.SearchVisitsInput;
import com.nexxserve.nexxclinic.dto.out.ApiResponse;
import com.nexxserve.nexxclinic.model.RoleName;
import com.nexxserve.nexxclinic.security.HasRole;
import com.nexxserve.nexxclinic.service.VisitDepartmentNoteService;
import com.nexxserve.nexxclinic.service.VisitService;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.ContextValue;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

@Controller
public class VisitQueryController {

    private final VisitService visitService;
    private final VisitDepartmentNoteService noteService;
    private final com.nexxserve.nexxclinic.service.VisitPriceEstimateService visitPriceEstimateService;

    public VisitQueryController(
        VisitService visitService,
        VisitDepartmentNoteService noteService,
        com.nexxserve.nexxclinic.service.VisitPriceEstimateService visitPriceEstimateService
    ) {
        this.visitService = visitService;
        this.noteService = noteService;
        this.visitPriceEstimateService = visitPriceEstimateService;
    }

    @HasRole({RoleName.ADMIN, RoleName.CLINIC_ADMIN, RoleName.RECEPTION, RoleName.NURSE, RoleName.CLINICIAN, RoleName.FINANCE, RoleName.MANAGER})
    @QueryMapping
    public ApiResponse visit(
            @Argument UUID visitId,
            @ContextValue(name = "authUser", required = false) AuthenticatedUser authUser
    ) {
        return visitService.visit(visitId, authUser);
    }

    @HasRole({RoleName.ADMIN, RoleName.CLINIC_ADMIN, RoleName.RECEPTION, RoleName.NURSE, RoleName.CLINICIAN, RoleName.FINANCE, RoleName.MANAGER})
    @QueryMapping
    public ApiResponse visitDepartmentNotes(
            @Argument UUID visitId,
            @Argument UUID visitDepartmentId,
            @ContextValue(name = "authUser", required = false) AuthenticatedUser authUser
    ) {
        return noteService.visitDepartmentNotes(visitId, visitDepartmentId, authUser);
    }

    @HasRole({RoleName.ADMIN, RoleName.CLINIC_ADMIN, RoleName.RECEPTION, RoleName.NURSE, RoleName.CLINICIAN, RoleName.FINANCE, RoleName.MANAGER})
    @QueryMapping
    public ApiResponse visits(
            @Argument @Valid SearchVisitsInput input,
            @ContextValue(name = "authUser", required = false) AuthenticatedUser authUser
    ) {
        return visitService.visits(input, authUser);
    }

        @HasRole({RoleName.ADMIN, RoleName.CLINIC_ADMIN, RoleName.RECEPTION, RoleName.NURSE, RoleName.CLINICIAN, RoleName.FINANCE, RoleName.MANAGER})
    @QueryMapping
    public ApiResponse lastPatientDepartmentVisit(
            @Argument UUID visitId,
            @Argument UUID departmentId,
            @ContextValue(name = "authUser", required = false) AuthenticatedUser authUser
    ) {
        return visitService.lastPatientDepartmentVisit(visitId, departmentId, authUser);
    }        @HasRole({RoleName.ADMIN, RoleName.CLINIC_ADMIN, RoleName.RECEPTION, RoleName.NURSE, RoleName.CLINICIAN, RoleName.FINANCE, RoleName.MANAGER})
    @QueryMapping
    public ApiResponse getPatientHistory(
            @Argument UUID patientId,
            @Argument @Valid SearchPatientHistoryInput input,
            @ContextValue(name = "authUser", required = false) AuthenticatedUser authUser
    ) {
        return visitService.getPatientHistory(patientId, input, authUser);
    }

    @HasRole({RoleName.ADMIN, RoleName.CLINIC_ADMIN, RoleName.RECEPTION, RoleName.NURSE, RoleName.CLINICIAN, RoleName.FINANCE, RoleName.MANAGER})
    @QueryMapping
    public ApiResponse visitPriceEstimates(
            @Argument UUID visitId
    ) {
        return visitPriceEstimateService.getEstimatesForVisit(visitId);
    }
}
