package com.nexxserve.nexxclinic.graphql;

import com.nexxserve.nexxclinic.auth.AuthenticatedUser;
import com.nexxserve.nexxclinic.dto.out.ApiResponse;
import com.nexxserve.nexxclinic.graphql.input.CreateInsuranceCoverageRuleInput;
import com.nexxserve.nexxclinic.graphql.input.UpdateInsuranceCoverageRuleInput;
import com.nexxserve.nexxclinic.model.RoleName;
import com.nexxserve.nexxclinic.security.HasRole;
import com.nexxserve.nexxclinic.service.InsuranceCoverageRuleService;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.ContextValue;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.stereotype.Controller;

@Controller
public class InsuranceCoverageRuleMutationController {

    private final InsuranceCoverageRuleService ruleService;

    public InsuranceCoverageRuleMutationController(InsuranceCoverageRuleService ruleService) {
        this.ruleService = ruleService;
    }

    @HasRole({RoleName.ADMIN, RoleName.CLINIC_ADMIN})
    @MutationMapping
    public ApiResponse createInsuranceCoverageRule(
        @Argument @Valid CreateInsuranceCoverageRuleInput input,
        @ContextValue(name = "authUser", required = false) AuthenticatedUser authUser
    ) {
        return ruleService.createRule(input);
    }

    @HasRole({RoleName.ADMIN, RoleName.CLINIC_ADMIN})
    @MutationMapping
    public ApiResponse updateInsuranceCoverageRule(
        @Argument UUID ruleId,
        @Argument @Valid UpdateInsuranceCoverageRuleInput input,
        @ContextValue(name = "authUser", required = false) AuthenticatedUser authUser
    ) {
        return ruleService.updateRule(ruleId, input);
    }

    @HasRole({RoleName.ADMIN, RoleName.CLINIC_ADMIN})
    @MutationMapping
    public ApiResponse deleteInsuranceCoverageRule(
        @Argument UUID ruleId,
        @ContextValue(name = "authUser", required = false) AuthenticatedUser authUser
    ) {
        return ruleService.deleteRule(ruleId);
    }
}
