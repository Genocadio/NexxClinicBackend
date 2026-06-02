package com.nexxserve.nexxclinic.graphql;

import com.nexxserve.nexxclinic.auth.AuthenticatedUser;
import com.nexxserve.nexxclinic.model.ApiResponse;
import com.nexxserve.nexxclinic.model.RoleName;
import com.nexxserve.nexxclinic.security.HasRole;
import com.nexxserve.nexxclinic.service.ClinicProfileService;
import org.springframework.graphql.data.method.annotation.ContextValue;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

@Controller
public class ClinicQueryController {

    private final ClinicProfileService clinicProfileService;

    public ClinicQueryController(ClinicProfileService clinicProfileService) {
        this.clinicProfileService = clinicProfileService;
    }

    @HasRole({RoleName.ADMIN, RoleName.CLINIC_ADMIN})
    @QueryMapping
    public ApiResponse clinicProfile(@ContextValue(name = "authUser", required = false) AuthenticatedUser authUser) {
        return clinicProfileService.clinicProfile();
    }
}
