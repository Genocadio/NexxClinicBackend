package com.nexxserve.nexxclinic.graphql;

import com.nexxserve.nexxclinic.auth.AuthenticatedUser;
import com.nexxserve.nexxclinic.auth.AccessTokenInfo;
import com.nexxserve.nexxclinic.graphql.input.ActivateUserInput;
import com.nexxserve.nexxclinic.graphql.input.AdminCreateUserInput;
import com.nexxserve.nexxclinic.graphql.input.AdminSetUserSessionLimitInput;
import com.nexxserve.nexxclinic.graphql.input.AdminUpdateUserInput;
import com.nexxserve.nexxclinic.graphql.input.AdminTriggerPasswordResetInput;
import com.nexxserve.nexxclinic.graphql.input.ChangeMyPasswordInput;
import com.nexxserve.nexxclinic.graphql.input.DeactivateUserInput;
import com.nexxserve.nexxclinic.graphql.input.LoginInput;
import com.nexxserve.nexxclinic.graphql.input.LogoutInput;
import com.nexxserve.nexxclinic.graphql.input.RefreshTokenInput;
import com.nexxserve.nexxclinic.graphql.input.RefreshSessionInput;
import com.nexxserve.nexxclinic.graphql.input.SelfRegisterInput;
import com.nexxserve.nexxclinic.graphql.input.SetInitialPasswordInput;
import com.nexxserve.nexxclinic.graphql.input.UpdateMyProfileInput;
import com.nexxserve.nexxclinic.dto.out.ApiResponse;
import com.nexxserve.nexxclinic.model.RoleName;
import com.nexxserve.nexxclinic.security.HasRole;
import com.nexxserve.nexxclinic.service.WorkerService;
import jakarta.validation.Valid;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.ContextValue;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.stereotype.Controller;
import java.util.UUID;

@Controller
public class UserMutationController {

    private final WorkerService workerService;

    public UserMutationController(WorkerService workerService) {
        this.workerService = workerService;
    }

    @MutationMapping
    public ApiResponse selfRegister(@Argument @Valid SelfRegisterInput input) {
        return workerService.selfRegister(input);
    }

    @MutationMapping
    public ApiResponse login(@Argument @Valid LoginInput input) {
        return workerService.login(input);
    }

    @MutationMapping
    public ApiResponse setInitialPassword(@Argument @Valid SetInitialPasswordInput input) {
        return workerService.setInitialPassword(input);
    }

    @MutationMapping
    public ApiResponse refreshSession(@Argument @Valid RefreshSessionInput input) {
        return workerService.refreshSession(input);
    }

    @MutationMapping
    public ApiResponse refreshToken(@Argument @Valid RefreshTokenInput input) {
        return workerService.refreshToken(input);
    }

    @MutationMapping
    public ApiResponse logout(
            @Argument @Valid LogoutInput input,
            @ContextValue(name = "authUser", required = false) AuthenticatedUser authUser,
            @ContextValue(name = "accessTokenInfo", required = false) AccessTokenInfo accessTokenInfo
    ) {
        return workerService.logout(input, authUser, accessTokenInfo);
    }

    @MutationMapping
    public ApiResponse updateMyProfile(
            @Argument @Valid UpdateMyProfileInput input,
            @ContextValue(name = "authUser", required = false) AuthenticatedUser authUser
    ) {
        return workerService.updateMyProfile(input, authUser);
    }

    @MutationMapping
    public ApiResponse changeMyPassword(
            @Argument @Valid ChangeMyPasswordInput input,
            @ContextValue(name = "authUser", required = false) AuthenticatedUser authUser,
            @ContextValue(name = "accessTokenInfo", required = false) AccessTokenInfo accessTokenInfo
    ) {
        return workerService.changeMyPassword(input, authUser, accessTokenInfo);
    }

    @HasRole(RoleName.ADMIN)
    @MutationMapping
    public ApiResponse adminCreateUser(
            @Argument @Valid AdminCreateUserInput input,
            @ContextValue(name = "authUser", required = false) AuthenticatedUser authUser
    ) {
        return workerService.adminCreateUser(input, authUser);
    }

    @HasRole(RoleName.ADMIN)
    @MutationMapping
    public ApiResponse adminUpdateUser(
            @Argument UUID userId,
            @Argument @Valid AdminUpdateUserInput input,
            @ContextValue(name = "authUser", required = false) AuthenticatedUser authUser
    ) {
        return workerService.adminUpdateUser(userId, input, authUser);
    }

    @HasRole(RoleName.ADMIN)
    @MutationMapping
    public ApiResponse activateUser(
            @Argument @Valid ActivateUserInput input,
            @ContextValue(name = "authUser", required = false) AuthenticatedUser authUser
    ) {
        return workerService.activateUser(input, authUser);
    }

    @HasRole(RoleName.ADMIN)
    @MutationMapping
    public ApiResponse deactivateUser(
            @Argument @Valid DeactivateUserInput input,
            @ContextValue(name = "authUser", required = false) AuthenticatedUser authUser
    ) {
        return workerService.deactivateUser(input, authUser);
    }

    @HasRole(RoleName.ADMIN)
    @MutationMapping
    public ApiResponse adminTriggerPasswordReset(
            @Argument @Valid AdminTriggerPasswordResetInput input,
            @ContextValue(name = "authUser", required = false) AuthenticatedUser authUser
    ) {
        return workerService.adminTriggerPasswordReset(input, authUser);
    }

    @HasRole(RoleName.ADMIN)
    @MutationMapping
    public ApiResponse adminSetUserSessionLimit(
            @Argument @Valid AdminSetUserSessionLimitInput input,
            @ContextValue(name = "authUser", required = false) AuthenticatedUser authUser
    ) {
        return workerService.adminSetUserSessionLimit(input, authUser);
    }
}
