package com.nexxserve.nexxclinic.graphql;

import com.nexxserve.nexxclinic.auth.AuthenticatedUser;
import com.nexxserve.nexxclinic.model.ApiResponse;
import com.nexxserve.nexxclinic.model.RoleName;
import com.nexxserve.nexxclinic.security.HasRole;
import com.nexxserve.nexxclinic.service.WorkerService;
import org.springframework.graphql.data.method.annotation.ContextValue;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

@Controller
public class UserQueryController {

    private final WorkerService workerService;

    public UserQueryController(WorkerService workerService) {
        this.workerService = workerService;
    }

    @QueryMapping
    public ApiResponse me(@ContextValue(name = "authUser", required = false) AuthenticatedUser authUser) {
        if (authUser == null) {
            return ApiResponse.unauthenticated("Authentication is required for this operation.");
        }
        return workerService.me(authUser);
    }

    @HasRole(RoleName.ADMIN)
    @QueryMapping
    public ApiResponse listUsers(@ContextValue(name = "authUser", required = false) AuthenticatedUser authUser) {
        return workerService.listUsers();
    }

    @HasRole(RoleName.ADMIN)
    @QueryMapping
    public ApiResponse adminAuditLogs(@ContextValue(name = "authUser", required = false) AuthenticatedUser authUser) {
        return workerService.adminAuditLogs();
    }
}
