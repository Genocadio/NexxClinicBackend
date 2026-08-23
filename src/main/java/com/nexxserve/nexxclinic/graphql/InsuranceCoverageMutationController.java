package com.nexxserve.nexxclinic.graphql;

import com.nexxserve.nexxclinic.auth.AuthenticatedUser;
import com.nexxserve.nexxclinic.dto.out.ApiResponse;
import com.nexxserve.nexxclinic.graphql.input.CreateInsuranceCoverageInput;
import com.nexxserve.nexxclinic.graphql.input.UpdateInsuranceCoverageInput;
import com.nexxserve.nexxclinic.model.RoleName;
import com.nexxserve.nexxclinic.security.HasRole;
import com.nexxserve.nexxclinic.service.InsuranceCoverageService;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.ContextValue;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.stereotype.Controller;

@Controller
public class InsuranceCoverageMutationController {

    private final InsuranceCoverageService ruleService;

    public InsuranceCoverageMutationController(InsuranceCoverageService ruleService) {
        this.ruleService = ruleService;
    }

    @HasRole({RoleName.ADMIN, RoleName.CLINIC_ADMIN})
    @MutationMapping
    public ApiResponse createInsuranceCoverage(
        @Argument @Valid CreateInsuranceCoverageInput input,
        @ContextValue(name = "authUser", required = false) AuthenticatedUser authUser
    ) {
        return ruleService.createRule(input);
    }

    @HasRole({RoleName.ADMIN, RoleName.CLINIC_ADMIN})
    @MutationMapping
    public ApiResponse updateInsuranceCoverage(
        @Argument UUID ruleId,
        @Argument @Valid UpdateInsuranceCoverageInput input,
        @ContextValue(name = "authUser", required = false) AuthenticatedUser authUser
    ) {
        return ruleService.updateRule(ruleId, input);
    }

    @HasRole({RoleName.ADMIN, RoleName.CLINIC_ADMIN})
    @MutationMapping
    public ApiResponse deleteInsuranceCoverage(
        @Argument UUID ruleId,
        @ContextValue(name = "authUser", required = false) AuthenticatedUser authUser
    ) {
        return ruleService.deleteRule(ruleId);
    }
}
