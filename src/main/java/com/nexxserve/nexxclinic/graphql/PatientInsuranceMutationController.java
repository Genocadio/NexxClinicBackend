package com.nexxserve.nexxclinic.graphql;

import com.nexxserve.nexxclinic.auth.AuthenticatedUser;
import com.nexxserve.nexxclinic.graphql.input.CreateInsuranceProviderInput;
import com.nexxserve.nexxclinic.graphql.input.CreatePatientInput;
import com.nexxserve.nexxclinic.graphql.input.CreatePatientInsuranceInput;
import com.nexxserve.nexxclinic.graphql.input.UpdateInsuranceProviderInput;
import com.nexxserve.nexxclinic.graphql.input.UpdatePatientInput;
import com.nexxserve.nexxclinic.graphql.input.UpdatePatientInsuranceInput;
import com.nexxserve.nexxclinic.model.ApiResponse;
import com.nexxserve.nexxclinic.model.RoleName;
import com.nexxserve.nexxclinic.security.HasRole;
import com.nexxserve.nexxclinic.service.InsuranceProviderService;
import com.nexxserve.nexxclinic.service.PatientService;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.ContextValue;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.stereotype.Controller;

@Controller
public class PatientInsuranceMutationController {

    private final InsuranceProviderService insuranceProviderService;
    private final PatientService patientService;

    public PatientInsuranceMutationController(
            InsuranceProviderService insuranceProviderService,
            PatientService patientService
    ) {
        this.insuranceProviderService = insuranceProviderService;
        this.patientService = patientService;
    }

    @HasRole({RoleName.ADMIN, RoleName.CLINIC_ADMIN, RoleName.RECEPTION})
    @MutationMapping
    public ApiResponse createInsuranceProvider(
            @Argument @Valid CreateInsuranceProviderInput input,
            @ContextValue(name = "authUser", required = false) AuthenticatedUser authUser
    ) {
        return insuranceProviderService.createInsuranceProvider(input);
    }

    @HasRole({RoleName.ADMIN, RoleName.CLINIC_ADMIN, RoleName.RECEPTION})
    @MutationMapping
    public ApiResponse updateInsuranceProvider(
            @Argument UUID insuranceProviderId,
            @Argument @Valid UpdateInsuranceProviderInput input,
            @ContextValue(name = "authUser", required = false) AuthenticatedUser authUser
    ) {
        return insuranceProviderService.updateInsuranceProvider(insuranceProviderId, input);
    }

    @HasRole({RoleName.ADMIN, RoleName.CLINIC_ADMIN})
    @MutationMapping
    public ApiResponse deleteInsuranceProvider(
            @Argument UUID insuranceProviderId,
            @ContextValue(name = "authUser", required = false) AuthenticatedUser authUser
    ) {
        return insuranceProviderService.deleteInsuranceProvider(insuranceProviderId);
    }

    @HasRole({RoleName.ADMIN, RoleName.CLINIC_ADMIN, RoleName.RECEPTION})
    @MutationMapping
    public ApiResponse createPatient(
            @Argument @Valid CreatePatientInput input,
            @ContextValue(name = "authUser", required = false) AuthenticatedUser authUser
    ) {
        return patientService.createPatient(input);
    }

    @HasRole({RoleName.ADMIN, RoleName.CLINIC_ADMIN, RoleName.RECEPTION})
    @MutationMapping
    public ApiResponse updatePatient(
            @Argument UUID patientId,
            @Argument @Valid UpdatePatientInput input,
            @ContextValue(name = "authUser", required = false) AuthenticatedUser authUser
    ) {
        return patientService.updatePatient(patientId, input);
    }

    @HasRole({RoleName.ADMIN, RoleName.CLINIC_ADMIN, RoleName.RECEPTION})
    @MutationMapping
    public ApiResponse deletePatient(
            @Argument UUID patientId,
            @ContextValue(name = "authUser", required = false) AuthenticatedUser authUser
    ) {
        return patientService.deletePatient(patientId);
    }

    @HasRole({RoleName.ADMIN, RoleName.CLINIC_ADMIN, RoleName.RECEPTION})
    @MutationMapping
    public ApiResponse createPatientInsurance(
            @Argument @Valid CreatePatientInsuranceInput input,
            @ContextValue(name = "authUser", required = false) AuthenticatedUser authUser
    ) {
        return patientService.createPatientInsurance(input);
    }

    @HasRole({RoleName.ADMIN, RoleName.CLINIC_ADMIN, RoleName.RECEPTION})
    @MutationMapping
    public ApiResponse updatePatientInsurance(
            @Argument UUID patientInsuranceId,
            @Argument @Valid UpdatePatientInsuranceInput input,
            @ContextValue(name = "authUser", required = false) AuthenticatedUser authUser
    ) {
        return patientService.updatePatientInsurance(patientInsuranceId, input);
    }

    @HasRole({RoleName.ADMIN, RoleName.CLINIC_ADMIN, RoleName.RECEPTION})
    @MutationMapping
    public ApiResponse deletePatientInsurance(
            @Argument UUID patientInsuranceId,
            @ContextValue(name = "authUser", required = false) AuthenticatedUser authUser
    ) {
        return patientService.deletePatientInsurance(patientInsuranceId);
    }
}
