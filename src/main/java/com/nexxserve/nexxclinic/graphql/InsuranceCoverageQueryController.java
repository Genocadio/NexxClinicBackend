package com.nexxserve.nexxclinic.graphql;

import com.nexxserve.nexxclinic.auth.AuthenticatedUser;
import com.nexxserve.nexxclinic.dto.out.ApiResponse;
import com.nexxserve.nexxclinic.graphql.input.SearchInsuranceCoveragesInput;
import com.nexxserve.nexxclinic.model.RoleName;
import com.nexxserve.nexxclinic.security.HasRole;
import com.nexxserve.nexxclinic.service.InsuranceCoverageService;
import java.util.UUID;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.ContextValue;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

@Controller
public class InsuranceCoverageQueryController {

    private final InsuranceCoverageService ruleService;

    public InsuranceCoverageQueryController(InsuranceCoverageService ruleService) {
        this.ruleService = ruleService;
    }

    @HasRole({RoleName.ADMIN, RoleName.CLINIC_ADMIN, RoleName.RECEPTION, RoleName.FINANCE, RoleName.MANAGER})
    @QueryMapping
    public ApiResponse insuranceCoverage(
        @Argument UUID ruleId,
        @ContextValue(name = "authUser", required = false) AuthenticatedUser authUser
    ) {
        return ruleService.getRule(ruleId);
    }

    @HasRole({RoleName.ADMIN, RoleName.CLINIC_ADMIN, RoleName.RECEPTION, RoleName.FINANCE, RoleName.MANAGER})
    @QueryMapping
    public ApiResponse insuranceCoverages(
        @Argument SearchInsuranceCoveragesInput input,
        @ContextValue(name = "authUser", required = false) AuthenticatedUser authUser
    ) {
        return ruleService.listRules(input);
    }
}
