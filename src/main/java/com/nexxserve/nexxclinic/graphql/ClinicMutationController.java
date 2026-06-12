package com.nexxserve.nexxclinic.graphql;

import com.nexxserve.nexxclinic.auth.AuthenticatedUser;
import com.nexxserve.nexxclinic.graphql.input.UpdateClinicProfileInput;
import com.nexxserve.nexxclinic.dto.out.ApiResponse;
import com.nexxserve.nexxclinic.model.RoleName;
import com.nexxserve.nexxclinic.security.HasRole;
import com.nexxserve.nexxclinic.service.ClinicProfileService;
import jakarta.validation.Valid;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.ContextValue;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.stereotype.Controller;

@Controller
public class ClinicMutationController {

    private final ClinicProfileService clinicProfileService;

    public ClinicMutationController(ClinicProfileService clinicProfileService) {
        this.clinicProfileService = clinicProfileService;
    }

    @HasRole({RoleName.ADMIN, RoleName.CLINIC_ADMIN})
    @MutationMapping
    public ApiResponse updateClinicProfile(
            @Argument @Valid UpdateClinicProfileInput input,
            @ContextValue(name = "authUser", required = false) AuthenticatedUser authUser
    ) {
        return clinicProfileService.updateClinicProfile(input);
    }

    @HasRole({RoleName.ADMIN, RoleName.CLINIC_ADMIN})
    @MutationMapping
    public ApiResponse deleteClinicProfile(@ContextValue(name = "authUser", required = false) AuthenticatedUser authUser) {
        return clinicProfileService.deleteClinicProfile();
    }
}
