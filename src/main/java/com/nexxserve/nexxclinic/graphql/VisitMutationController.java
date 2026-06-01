package com.nexxserve.nexxclinic.graphql;

import com.nexxserve.nexxclinic.auth.AuthenticatedUser;
import com.nexxserve.nexxclinic.graphql.input.ConsultationAnswersInput;
import com.nexxserve.nexxclinic.graphql.input.ChangeVisitDateInput;
import com.nexxserve.nexxclinic.graphql.input.CreateVisitDepartmentProductInput;
import com.nexxserve.nexxclinic.graphql.input.CreateVisitInput;
import com.nexxserve.nexxclinic.graphql.input.UpdateVisitDepartmentProductStatusInput;
import com.nexxserve.nexxclinic.graphql.input.UpdateVisitDepartmentStatusInput;
import com.nexxserve.nexxclinic.graphql.input.UpdateVisitDepartmentProductQuantityInput;
import com.nexxserve.nexxclinic.graphql.input.AddDiagnosisInput;
import com.nexxserve.nexxclinic.graphql.input.AddMedicationInput;
import com.nexxserve.nexxclinic.graphql.input.AddVisitVitalSignsInput;
import com.nexxserve.nexxclinic.graphql.input.AddVisitPreInstructionsInput;
import com.nexxserve.nexxclinic.model.ApiResponse;
import com.nexxserve.nexxclinic.model.RoleName;
import com.nexxserve.nexxclinic.security.HasRole;
import com.nexxserve.nexxclinic.service.VisitService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.ContextValue;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.stereotype.Controller;

@Controller
public class VisitMutationController {

    private final VisitService visitService;

    public VisitMutationController(VisitService visitService) {
        this.visitService = visitService;
    }

    @HasRole({RoleName.ADMIN, RoleName.CLINIC_ADMIN, RoleName.RECEPTION, RoleName.NURSE, RoleName.CLINICIAN})
    @MutationMapping
    public ApiResponse createVisit(
            @Argument @Valid CreateVisitInput input,
            @ContextValue(name = "authUser", required = false) AuthenticatedUser authUser
    ) {
        return visitService.createVisit(input, authUser);
    }

    @HasRole({RoleName.ADMIN, RoleName.CLINIC_ADMIN, RoleName.RECEPTION, RoleName.NURSE, RoleName.CLINICIAN})
    @MutationMapping
    public ApiResponse changeVisitDate(
            @Argument @Valid ChangeVisitDateInput input,
            @ContextValue(name = "authUser", required = false) AuthenticatedUser authUser
    ) {
        return visitService.changeVisitDate(input);
    }

    @HasRole({RoleName.ADMIN, RoleName.CLINIC_ADMIN, RoleName.RECEPTION, RoleName.NURSE, RoleName.CLINICIAN})
    @MutationMapping
    public ApiResponse addVisitDepartment(
            @Argument UUID visitId,
            @Argument UUID departmentId,
            @ContextValue(name = "authUser", required = false) AuthenticatedUser authUser
    ) {
        return visitService.addVisitDepartment(visitId, departmentId, authUser);
    }

    @HasRole({RoleName.ADMIN, RoleName.CLINIC_ADMIN, RoleName.RECEPTION, RoleName.NURSE, RoleName.CLINICIAN})
    @MutationMapping
    public ApiResponse addChildVisitDepartment(
            @Argument UUID parentVisitDepartmentId,
            @Argument UUID departmentId,
            @Argument List<UUID> productIds,
            @Argument UUID processorId,
            @ContextValue(name = "authUser", required = false) AuthenticatedUser authUser
    ) {
        return visitService.addChildVisitDepartment(parentVisitDepartmentId, departmentId, productIds, processorId, authUser);
    }

    @HasRole({RoleName.ADMIN, RoleName.CLINIC_ADMIN, RoleName.RECEPTION, RoleName.NURSE, RoleName.CLINICIAN})
    @MutationMapping
    public ApiResponse removeChildVisitDepartment(
            @Argument UUID visitDepartmentId,
            @ContextValue(name = "authUser", required = false) AuthenticatedUser authUser
    ) {
        return visitService.removeChildVisitDepartment(visitDepartmentId, authUser);
    }

    @HasRole({RoleName.ADMIN, RoleName.CLINIC_ADMIN, RoleName.RECEPTION, RoleName.NURSE, RoleName.CLINICIAN})
    @MutationMapping
    public ApiResponse addVisitDepartmentProcessor(
            @Argument UUID visitDepartmentId,
            @Argument UUID processorId,
            @ContextValue(name = "authUser", required = false) AuthenticatedUser authUser
    ) {
        return visitService.addVisitDepartmentProcessor(visitDepartmentId, processorId, authUser);
    }

    @HasRole({RoleName.ADMIN, RoleName.CLINIC_ADMIN, RoleName.RECEPTION, RoleName.NURSE, RoleName.CLINICIAN})
    @MutationMapping
    public ApiResponse removeVisitDepartmentProcessor(
            @Argument UUID visitDepartmentId,
            @Argument UUID processorId,
            @ContextValue(name = "authUser", required = false) AuthenticatedUser authUser
    ) {
        return visitService.removeVisitDepartmentProcessor(visitDepartmentId, processorId, authUser);
    }

    @HasRole({RoleName.ADMIN, RoleName.CLINIC_ADMIN, RoleName.RECEPTION, RoleName.NURSE, RoleName.FINANCE})
    @MutationMapping
    public ApiResponse linkVisitInsurances(
            @Argument UUID visitId,
            @Argument List<UUID> insuranceIds,
            @ContextValue(name = "authUser", required = false) AuthenticatedUser authUser
    ) {
        return visitService.linkVisitInsurances(visitId, insuranceIds, authUser);
    }

    @HasRole({RoleName.ADMIN, RoleName.CLINIC_ADMIN, RoleName.RECEPTION, RoleName.NURSE, RoleName.FINANCE})
    @MutationMapping
    public ApiResponse unlinkVisitInsurances(
            @Argument UUID visitId,
            @Argument List<UUID> insuranceIds,
            @ContextValue(name = "authUser", required = false) AuthenticatedUser authUser
    ) {
        return visitService.unlinkVisitInsurances(visitId, insuranceIds, authUser);
    }

    @HasRole({RoleName.ADMIN, RoleName.CLINIC_ADMIN, RoleName.RECEPTION, RoleName.NURSE, RoleName.CLINICIAN})
    @MutationMapping
    public ApiResponse updateVisitDepartmentStatus(
            @Argument @Valid UpdateVisitDepartmentStatusInput input,
            @ContextValue(name = "authUser", required = false) AuthenticatedUser authUser
    ) {
        return visitService.updateVisitDepartmentStatus(input, authUser);
    }

    @HasRole({RoleName.ADMIN, RoleName.CLINIC_ADMIN, RoleName.RECEPTION, RoleName.NURSE, RoleName.CLINICIAN})
    @MutationMapping
    public ApiResponse saveAnswerAndCompleteVisit(
            @Argument @Valid ConsultationAnswersInput input,
            @Argument("final") boolean finalAnswer,
            @ContextValue(name = "authUser", required = false) AuthenticatedUser authUser
    ) {
        return visitService.saveAnswerAndCompleteVisit(input, finalAnswer, authUser);
    }

    @HasRole({RoleName.ADMIN, RoleName.CLINIC_ADMIN, RoleName.FINANCE, RoleName.NURSE, RoleName.CLINICIAN})
    @MutationMapping
    public ApiResponse addVisitDepartmentProduct(
            @Argument @Valid CreateVisitDepartmentProductInput input,
            @ContextValue(name = "authUser", required = false) AuthenticatedUser authUser
    ) {
        return visitService.addVisitDepartmentProduct(input, authUser);
    }

    @HasRole({RoleName.ADMIN, RoleName.CLINIC_ADMIN, RoleName.RECEPTION, RoleName.NURSE, RoleName.CLINICIAN})
    @MutationMapping
    public ApiResponse updateVisitDepartmentProductStatus(
            @Argument @Valid UpdateVisitDepartmentProductStatusInput input,
            @ContextValue(name = "authUser", required = false) AuthenticatedUser authUser
    ) {
        return visitService.updateVisitDepartmentProductStatus(input, authUser);
    }

    @HasRole({RoleName.ADMIN, RoleName.CLINIC_ADMIN, RoleName.FINANCE, RoleName.NURSE, RoleName.CLINICIAN})
    @MutationMapping
    public ApiResponse updateVisitDepartmentProductQuantity(
            @Argument @Valid UpdateVisitDepartmentProductQuantityInput input,
            @ContextValue(name = "authUser", required = false) AuthenticatedUser authUser
    ) {
        return visitService.updateVisitDepartmentProductQuantity(input);
    }

    @HasRole({RoleName.ADMIN, RoleName.CLINIC_ADMIN, RoleName.RECEPTION, RoleName.NURSE, RoleName.CLINICIAN})
    @MutationMapping
    public ApiResponse completeConsultationVisit(
            @Argument @Valid ConsultationAnswersInput input,
            @Argument("final") boolean finalAnswer,
            @ContextValue(name = "authUser", required = false) AuthenticatedUser authUser
    ) {
        return visitService.completeVisit(input, finalAnswer, authUser);
    }

    @HasRole({RoleName.ADMIN, RoleName.CLINIC_ADMIN, RoleName.RECEPTION, RoleName.NURSE, RoleName.CLINICIAN})
    @MutationMapping
    public ApiResponse completeVisit(
            @Argument UUID visitId,
            @ContextValue(name = "authUser", required = false) AuthenticatedUser authUser
    ) {
        return visitService.completeVisit(visitId);
    }

    @HasRole({RoleName.ADMIN, RoleName.CLINIC_ADMIN, RoleName.RECEPTION, RoleName.NURSE, RoleName.CLINICIAN})
    @MutationMapping
    public ApiResponse cancelVisit(
            @Argument UUID visitId,
            @ContextValue(name = "authUser", required = false) AuthenticatedUser authUser
    ) {
        return visitService.cancelVisit(visitId);
    }

    @HasRole({RoleName.ADMIN, RoleName.CLINIC_ADMIN, RoleName.FINANCE, RoleName.NURSE, RoleName.CLINICIAN})
    @MutationMapping
    public ApiResponse removeVisitDepartmentProduct(
            @Argument UUID visitDepartmentProductId,
            @ContextValue(name = "authUser", required = false) AuthenticatedUser authUser
    ) {
        return visitService.removeVisitDepartmentProduct(visitDepartmentProductId);
    }

    @HasRole({RoleName.ADMIN, RoleName.CLINIC_ADMIN, RoleName.RECEPTION, RoleName.NURSE, RoleName.CLINICIAN})
    @MutationMapping
    public ApiResponse addDiagnosis(
            @Argument @Valid AddDiagnosisInput input,
            @ContextValue(name = "authUser", required = false) AuthenticatedUser authUser
    ) {
        return visitService.addDiagnosisToVisitDepartment(input);
    }

    @HasRole({RoleName.ADMIN, RoleName.CLINIC_ADMIN, RoleName.RECEPTION, RoleName.NURSE, RoleName.CLINICIAN})
    @MutationMapping
    public ApiResponse addVisitVitalSigns(
            @Argument @Valid AddVisitVitalSignsInput input,
            @ContextValue(name = "authUser", required = false) AuthenticatedUser authUser
    ) {
        return visitService.addVisitVitalSigns(input, authUser);
    }

    @HasRole({RoleName.ADMIN, RoleName.CLINIC_ADMIN, RoleName.RECEPTION, RoleName.NURSE, RoleName.CLINICIAN})
    @MutationMapping
    public ApiResponse addVisitPreInstructions(
            @Argument @Valid AddVisitPreInstructionsInput input,
            @ContextValue(name = "authUser", required = false) AuthenticatedUser authUser
    ) {
        return visitService.addVisitPreInstructions(input, authUser);
    }

    @HasRole({RoleName.ADMIN, RoleName.CLINIC_ADMIN, RoleName.RECEPTION, RoleName.NURSE, RoleName.CLINICIAN})
    @MutationMapping
    public ApiResponse addMedication(
            @Argument @Valid AddMedicationInput input,
            @ContextValue(name = "authUser", required = false) AuthenticatedUser authUser
    ) {
        return visitService.addMedicationToVisitDepartment(input);
    }
}

