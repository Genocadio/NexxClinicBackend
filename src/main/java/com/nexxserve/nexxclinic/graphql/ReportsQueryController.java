package com.nexxserve.nexxclinic.graphql;

import com.nexxserve.nexxclinic.auth.AuthenticatedUser;
import com.nexxserve.nexxclinic.dto.out.ApiResponse;
import com.nexxserve.nexxclinic.model.RoleName;
import com.nexxserve.nexxclinic.security.HasRole;
import com.nexxserve.nexxclinic.service.ReportsService;
import java.util.List;
import java.util.UUID;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.ContextValue;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

@Controller
public class ReportsQueryController {

    private final ReportsService reportsService;

    public ReportsQueryController(ReportsService reportsService) {
        this.reportsService = reportsService;
    }

    @HasRole({
        RoleName.ADMIN,
        RoleName.CLINIC_ADMIN,
        RoleName.RECEPTION,
        RoleName.NURSE,
        RoleName.CLINICIAN,
        RoleName.FINANCE,
        RoleName.MANAGER
    })
    @QueryMapping
    public ApiResponse getUserReports(
        @Argument("fromDate") String fromDate,
        @Argument("toDate") String toDate,
        @Argument("period") String period,
        @Argument("workerId") UUID workerId,
        @ContextValue(name = "authUser", required = false) AuthenticatedUser authUser
    ) {
        return reportsService.getUserReports(fromDate, toDate, period, workerId, authUser);
    }

    @HasRole({
        RoleName.ADMIN,
        RoleName.CLINIC_ADMIN,
        RoleName.MANAGER
    })
    @QueryMapping
    public ApiResponse getClinicReports(
        @Argument("fromDate") String fromDate,
        @Argument("toDate") String toDate,
        @Argument("period") String period,
        @Argument("departmentId") UUID departmentId,
        @Argument("departmentIds") List<UUID> departmentIds,
        @Argument("clinicianId") UUID clinicianId,
        @Argument("clinicianIds") List<UUID> clinicianIds,
        @Argument("insuranceId") UUID insuranceId,
        @Argument("insuranceIds") List<UUID> insuranceIds,
        @Argument("isPrivateOnly") Boolean isPrivateOnly,
        @ContextValue(name = "authUser", required = false) AuthenticatedUser authUser
    ) {
        return reportsService.getClinicReports(fromDate, toDate, period, departmentId, departmentIds, clinicianId, clinicianIds, insuranceId, insuranceIds, isPrivateOnly, authUser);
    }
}
