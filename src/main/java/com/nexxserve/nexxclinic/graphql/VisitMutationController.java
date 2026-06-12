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
import com.nexxserve.nexxclinic.graphql.input.AddVisitDepartmentNoteInput;
import com.nexxserve.nexxclinic.graphql.input.AddChildVisitDepartmentInput;
import com.nexxserve.nexxclinic.dto.out.ApiResponse;
import com.nexxserve.nexxclinic.model.RoleName;
import com.nexxserve.nexxclinic.security.HasRole;
import com.nexxserve.nexxclinic.service.VisitDepartmentNoteService;
import com.nexxserve.nexxclinic.service.VisitDepartmentService;
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
    private final VisitDepartmentService visitDepartmentService;
    private final VisitDepartmentNoteService noteService;

    public VisitMutationController(VisitService visitService, VisitDepartmentService visitDepartmentService,  VisitDepartmentNoteService noteService) {
        this.visitService = visitService;
        this.visitDepartmentService = visitDepartmentService;
        this.noteService = noteService;
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
            @Argument com.nexxserve.nexxclinic.model.EncounterType encounterType,
            @ContextValue(name = "authUser", required = false) AuthenticatedUser authUser
    ) {
        return visitDepartmentService.addVisitDepartment(visitId, departmentId, encounterType, authUser);
    }

    @HasRole({RoleName.ADMIN, RoleName.CLINIC_ADMIN, RoleName.RECEPTION, RoleName.NURSE, RoleName.CLINICIAN})
    @MutationMapping
    public ApiResponse updateVisitDepartmentEncounterType(
            @Argument UUID visitDepartmentId,
            @Argument com.nexxserve.nexxclinic.model.EncounterType encounterType,
            @ContextValue(name = "authUser", required = false) AuthenticatedUser authUser
    ) {
        return visitDepartmentService.updateVisitDepartmentEncounterType(visitDepartmentId, encounterType, authUser);
    }

    @HasRole({RoleName.ADMIN, RoleName.CLINIC_ADMIN, RoleName.RECEPTION, RoleName.NURSE, RoleName.CLINICIAN})
    @MutationMapping
    public ApiResponse addChildVisitDepartment(
            @Argument @Valid AddChildVisitDepartmentInput input,
            @ContextValue(name = "authUser", required = false) AuthenticatedUser authUser
    ) {
        return visitDepartmentService.addChildVisitDepartment(input, authUser);
    }

    @HasRole({RoleName.ADMIN, RoleName.CLINIC_ADMIN, RoleName.RECEPTION, RoleName.NURSE, RoleName.CLINICIAN})
    @MutationMapping
    public ApiResponse removeChildVisitDepartment(
            @Argument UUID visitDepartmentId,
            @ContextValue(name = "authUser", required = false) AuthenticatedUser authUser
    ) {
        return visitDepartmentService.removeChildVisitDepartment(visitDepartmentId, authUser);
    }

    @HasRole({RoleName.ADMIN, RoleName.CLINIC_ADMIN, RoleName.RECEPTION, RoleName.NURSE, RoleName.CLINICIAN})
    @MutationMapping
    public ApiResponse addVisitDepartmentProcessor(
            @Argument UUID visitDepartmentId,
            @Argument UUID processorId,
            @ContextValue(name = "authUser", required = false) AuthenticatedUser authUser
    ) {
        return visitDepartmentService.addVisitDepartmentProcessor(visitDepartmentId, processorId, authUser);
    }

    @HasRole({RoleName.ADMIN, RoleName.CLINIC_ADMIN, RoleName.RECEPTION, RoleName.NURSE, RoleName.CLINICIAN})
    @MutationMapping
    public ApiResponse removeVisitDepartmentProcessor(
            @Argument UUID visitDepartmentId,
            @Argument UUID processorId,
            @ContextValue(name = "authUser", required = false) AuthenticatedUser authUser
    ) {
        return visitDepartmentService.removeVisitDepartmentProcessor(visitDepartmentId, processorId, authUser);
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
        return visitDepartmentService.updateVisitDepartmentStatus(input, authUser);
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
        return visitDepartmentService.addVisitDepartmentProduct(input, authUser);
    }

    @HasRole({RoleName.ADMIN, RoleName.CLINIC_ADMIN, RoleName.RECEPTION, RoleName.NURSE, RoleName.CLINICIAN})
    @MutationMapping
    public ApiResponse updateVisitDepartmentProductStatus(
            @Argument @Valid UpdateVisitDepartmentProductStatusInput input,
            @ContextValue(name = "authUser", required = false) AuthenticatedUser authUser
    ) {
        return visitDepartmentService.updateVisitDepartmentProductStatus(input, authUser);
    }

    @HasRole({RoleName.ADMIN, RoleName.CLINIC_ADMIN, RoleName.FINANCE, RoleName.NURSE, RoleName.CLINICIAN})
    @MutationMapping
    public ApiResponse updateVisitDepartmentProductQuantity(
            @Argument @Valid UpdateVisitDepartmentProductQuantityInput input,
            @ContextValue(name = "authUser", required = false) AuthenticatedUser authUser
    ) {
        return visitDepartmentService.updateVisitDepartmentProductQuantity(input);
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
        return visitDepartmentService.removeVisitDepartmentProduct(visitDepartmentProductId);
    }

    @HasRole({RoleName.ADMIN, RoleName.CLINIC_ADMIN, RoleName.RECEPTION, RoleName.NURSE, RoleName.CLINICIAN})
    @MutationMapping
    public ApiResponse addDiagnosis(
            @Argument @Valid AddDiagnosisInput input,
            @ContextValue(name = "authUser", required = false) AuthenticatedUser authUser
    ) {
        return visitDepartmentService.addDiagnosisToVisitDepartment(input);
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
        return visitDepartmentService.addVisitPreInstructions(input, authUser);
    }

    @HasRole({RoleName.ADMIN, RoleName.CLINIC_ADMIN, RoleName.RECEPTION, RoleName.NURSE, RoleName.CLINICIAN})
    @MutationMapping
    public ApiResponse addMedication(
            @Argument @Valid AddMedicationInput input,
            @ContextValue(name = "authUser", required = false) AuthenticatedUser authUser
    ) {
        return visitDepartmentService.addMedicationToVisitDepartment(input);
    }

    @HasRole({RoleName.ADMIN, RoleName.CLINIC_ADMIN, RoleName.RECEPTION, RoleName.NURSE, RoleName.CLINICIAN})
    @MutationMapping
    public ApiResponse addVisitDepartmentNote(
            @Argument @Valid AddVisitDepartmentNoteInput input,
            @ContextValue(name = "authUser", required = false) AuthenticatedUser authUser
    ) {
        return noteService.addVisitDepartmentNote(input, authUser);
    }

    @HasRole({RoleName.ADMIN, RoleName.CLINIC_ADMIN, RoleName.RECEPTION, RoleName.NURSE, RoleName.CLINICIAN})
    @MutationMapping
    public ApiResponse markVisitDepartmentNoteViewed(
            @Argument UUID noteId,
            @ContextValue(name = "authUser", required = false) AuthenticatedUser authUser
    ) {
        return noteService.markVisitDepartmentNoteViewed(noteId, authUser);
    }

    @HasRole({RoleName.ADMIN, RoleName.CLINIC_ADMIN, RoleName.RECEPTION, RoleName.NURSE, RoleName.CLINICIAN})
    @MutationMapping
    public ApiResponse markVisitDepartmentNotesViewed(
            @Argument UUID visitDepartmentId,
            @ContextValue(name = "authUser", required = false) AuthenticatedUser authUser
    ) {
        return noteService.markVisitDepartmentNotesViewed(visitDepartmentId, authUser);
    }
}

