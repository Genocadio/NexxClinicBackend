package com.nexxserve.nexxclinic.graphql;

import com.nexxserve.nexxclinic.auth.AuthenticatedUser;
import com.nexxserve.nexxclinic.graphql.input.SearchInsuranceProvidersInput;
import com.nexxserve.nexxclinic.graphql.input.SearchPatientsInput;
import com.nexxserve.nexxclinic.dto.out.ApiResponse;
import com.nexxserve.nexxclinic.model.RoleName;
import com.nexxserve.nexxclinic.security.HasRole;
import com.nexxserve.nexxclinic.service.InsuranceProviderService;
import com.nexxserve.nexxclinic.service.PatientService;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.ContextValue;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

@Controller
public class PatientInsuranceQueryController {

    private final InsuranceProviderService insuranceProviderService;
    private final PatientService patientService;

    public PatientInsuranceQueryController(
            InsuranceProviderService insuranceProviderService,
            PatientService patientService
    ) {
        this.insuranceProviderService = insuranceProviderService;
        this.patientService = patientService;
    }

    @HasRole({RoleName.ADMIN, RoleName.CLINIC_ADMIN, RoleName.RECEPTION, RoleName.FINANCE, RoleName.MANAGER})
    @QueryMapping
    public ApiResponse insuranceProvider(
            @Argument UUID insuranceProviderId,
            @ContextValue(name = "authUser", required = false) AuthenticatedUser authUser
    ) {
        return insuranceProviderService.insuranceProvider(insuranceProviderId);
    }

    @HasRole({RoleName.ADMIN, RoleName.CLINIC_ADMIN, RoleName.RECEPTION, RoleName.FINANCE, RoleName.MANAGER})
    @QueryMapping
    public ApiResponse insuranceProviders(
            @Argument @Valid SearchInsuranceProvidersInput input,
            @ContextValue(name = "authUser", required = false) AuthenticatedUser authUser
    ) {
        return insuranceProviderService.insuranceProviders(input);
    }

    @HasRole({RoleName.ADMIN, RoleName.CLINIC_ADMIN, RoleName.RECEPTION, RoleName.FINANCE, RoleName.MANAGER})
    @QueryMapping
    public ApiResponse patient(
            @Argument UUID patientId,
            @ContextValue(name = "authUser", required = false) AuthenticatedUser authUser
    ) {
        return patientService.patient(patientId);
    }

    @HasRole({RoleName.ADMIN, RoleName.CLINIC_ADMIN, RoleName.RECEPTION, RoleName.FINANCE, RoleName.MANAGER})
    @QueryMapping
    public ApiResponse patients(
            @ContextValue(name = "authUser", required = false) AuthenticatedUser authUser
    ) {
        return patientService.patients();
    }

    @HasRole({RoleName.ADMIN, RoleName.CLINIC_ADMIN, RoleName.RECEPTION, RoleName.FINANCE, RoleName.MANAGER})
    @QueryMapping
    public ApiResponse patientInsurances(
            @Argument UUID patientId,
            @ContextValue(name = "authUser", required = false) AuthenticatedUser authUser
    ) {
        return patientService.patientInsurances(patientId);
    }

    @HasRole({RoleName.ADMIN, RoleName.CLINIC_ADMIN, RoleName.RECEPTION, RoleName.FINANCE, RoleName.MANAGER})
    @QueryMapping
    public ApiResponse searchPatients(
            @Argument @Valid SearchPatientsInput input,
            @ContextValue(name = "authUser", required = false) AuthenticatedUser authUser
    ) {
        return patientService.searchPatients(input);
    }
}
