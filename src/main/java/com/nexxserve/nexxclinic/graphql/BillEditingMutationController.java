package com.nexxserve.nexxclinic.graphql;

import com.nexxserve.nexxclinic.auth.AuthenticatedUser;
import com.nexxserve.nexxclinic.dto.out.ApiResponse;
import com.nexxserve.nexxclinic.model.RoleName;
import com.nexxserve.nexxclinic.security.HasRole;
import com.nexxserve.nexxclinic.service.BillEditingService;
import java.util.List;
import java.util.UUID;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.ContextValue;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.stereotype.Controller;

@Controller
public class BillEditingMutationController {

    private final BillEditingService billEditingService;

    public BillEditingMutationController(BillEditingService billEditingService) {
        this.billEditingService = billEditingService;
    }

    @HasRole({RoleName.ADMIN, RoleName.CLINIC_ADMIN, RoleName.FINANCE})
    @MutationMapping
    public ApiResponse startBillEditing(
            @Argument("visitDepartmentId") UUID visitDepartmentId,
            @ContextValue(name = "authUser", required = false) AuthenticatedUser authUser
    ) {
        return billEditingService.startBillEditing(visitDepartmentId, authUser);
    }

    @HasRole({RoleName.ADMIN, RoleName.CLINIC_ADMIN, RoleName.FINANCE})
    @MutationMapping
    public ApiResponse completeBillEditing(
            @Argument("visitDepartmentId") UUID visitDepartmentId,
            @ContextValue(name = "authUser", required = false) AuthenticatedUser authUser
    ) {
        return billEditingService.completeBillEditing(visitDepartmentId, authUser);
    }

    @HasRole({RoleName.ADMIN, RoleName.CLINIC_ADMIN, RoleName.FINANCE})
    @MutationMapping
    public ApiResponse cancelBillEditing(
            @Argument("visitDepartmentId") UUID visitDepartmentId,
            @Argument("addedProductIds") List<UUID> addedProductIds,
            @ContextValue(name = "authUser", required = false) AuthenticatedUser authUser
    ) {
        return billEditingService.cancelBillEditing(visitDepartmentId, addedProductIds, authUser);
    }
}
