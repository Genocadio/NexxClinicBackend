package com.nexxserve.nexxclinic.service;

import com.nexxserve.nexxclinic.auth.AuthenticatedUser;
import com.nexxserve.nexxclinic.dto.out.ApiResponse;
import com.nexxserve.nexxclinic.entity.Worker;
import com.nexxserve.nexxclinic.model.AccountStatus;
import com.nexxserve.nexxclinic.model.ResponseStatus;
import com.nexxserve.nexxclinic.model.RoleName;
import com.nexxserve.nexxclinic.repository.WorkerRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class ReportsServiceIntegrationTest {

    @Autowired
    private ReportsService reportsService;

    @Autowired
    private WorkerRepository workerRepository;

    @Test
    void getUserReportsReturnsExpectedReportStructure() {
        Worker admin = new Worker();
        admin.setUsername("testadmin");
        admin.setFirstName("Admin");
        admin.setLastName("User");
        admin.setEmail("admin@clinic.com");
        admin.setPhoneNumber("+250788000001");
        admin.setPasswordHash("hashed");
        admin.setAccountStatus(AccountStatus.ACTIVE);
        admin.setRoles(Set.of(RoleName.ADMIN, RoleName.CLINICIAN, RoleName.FINANCE));
        admin = workerRepository.save(admin);

        AuthenticatedUser authUser = new AuthenticatedUser(
                admin.getId(),
                "testadmin",
                Set.of(RoleName.ADMIN, RoleName.CLINICIAN, RoleName.FINANCE),
                "tok-1",
                Instant.now().plusSeconds(3600)
        );

        String today = LocalDate.now().toString();
        ApiResponse response = reportsService.getUserReports(today, today, "today", admin.getId(), authUser);

        assertEquals(ResponseStatus.SUCCESS, response.status());
        assertNotNull(response.data());

        assertTrue(response.data() instanceof Map);
        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) response.data();

        assertEquals(admin.getId().toString(), data.get("workerId"));
        assertTrue((Boolean) data.get("hasClinician"));
        assertTrue((Boolean) data.get("hasFinance"));
        assertTrue((Boolean) data.get("canViewFinance"));
        assertTrue((Boolean) data.get("isAdminOrManager"));

        assertNotNull(data.get("reception"));
        assertNotNull(data.get("clinician"));
        assertNotNull(data.get("nurse"));
        assertNotNull(data.get("finance"));
        assertNotNull(data.get("timeline"));
        assertNotNull(data.get("allActivities"));

        @SuppressWarnings("unchecked")
        Map<String, Object> finance = (Map<String, Object>) data.get("finance");
        assertNotNull(finance.get("money"));
        assertNotNull(finance.get("general"));

        @SuppressWarnings("unchecked")
        Map<String, Object> clinician = (Map<String, Object>) data.get("clinician");
        assertNotNull(clinician.get("money"));
        assertNotNull(clinician.get("demographics"));
    }

    @Test
    void receptionistReportDoesNotExposeFinanceData() {
        Worker receptionist = new Worker();
        receptionist.setUsername("testreceptionist");
        receptionist.setFirstName("Reception");
        receptionist.setLastName("User");
        receptionist.setEmail("receptionist@clinic.com");
        receptionist.setPhoneNumber("+250788000002");
        receptionist.setPasswordHash("hashed");
        receptionist.setAccountStatus(AccountStatus.ACTIVE);
        receptionist.setRoles(Set.of(RoleName.RECEPTION));
        receptionist = workerRepository.save(receptionist);

        AuthenticatedUser authUser = new AuthenticatedUser(
                receptionist.getId(),
                "testreceptionist",
                Set.of(RoleName.RECEPTION),
                "tok-2",
                Instant.now().plusSeconds(3600)
        );

        String today = LocalDate.now().toString();
        ApiResponse response = reportsService.getUserReports(today, today, "today", null, authUser);

        assertEquals(ResponseStatus.SUCCESS, response.status());
        assertTrue(response.data() instanceof Map);
        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) response.data();

        assertEquals(Boolean.FALSE, data.get("canViewFinance"));
        assertNull(data.get("finance"));

        @SuppressWarnings("unchecked")
        Map<String, Object> clinician = (Map<String, Object>) data.get("clinician");
        assertNull(clinician.get("money"));
        assertTrue(((java.util.List<?>) clinician.get("encountersList")).isEmpty());

        @SuppressWarnings("unchecked")
        java.util.List<Map<String, Object>> timeline = (java.util.List<Map<String, Object>>) data.get("timeline");
        assertTrue(timeline.stream().allMatch(bucket -> ((Number) bucket.get("finance")).intValue() == 0));
    }
}
