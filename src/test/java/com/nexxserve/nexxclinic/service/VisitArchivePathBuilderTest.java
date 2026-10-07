package com.nexxserve.nexxclinic.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.nexxserve.nexxclinic.entity.Department;
import com.nexxserve.nexxclinic.entity.Patient;
import com.nexxserve.nexxclinic.entity.Visit;
import com.nexxserve.nexxclinic.entity.VisitDepartment;
import com.nexxserve.nexxclinic.repository.VisitRepository;
import java.time.LocalDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class VisitArchivePathBuilderTest {

    @Test
    void buildsClinicPatientVisitAndDepartmentPrefixesWithReadableNamesAndStableIds() {
        VisitRepository visits = mock(VisitRepository.class);
        VisitArchivePathBuilder builder = new VisitArchivePathBuilder(visits);
        UUID clinicId = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
        UUID patientId = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");
        UUID visitId = UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc");
        UUID visitDepartmentId = UUID.fromString("dddddddd-dddd-dddd-dddd-dddddddddddd");
        LocalDateTime createdAt = LocalDateTime.of(2025, 1, 2, 10, 0);
        LocalDateTime visitDate = LocalDateTime.of(2025, 2, 3, 11, 12);

        Patient patient = new Patient();
        patient.setId(patientId);
        patient.setFullName("Jane Doe");
        patient.setPrimaryPhoneNumber("+250 788 123 456");

        Visit visit = new Visit();
        visit.setId(visitId);
        visit.setPatient(patient);
        visit.setCreatedAt(createdAt);
        visit.setVisitDate(visitDate);
        when(visits.countPatientVisitsCreatedThrough(patientId, createdAt, visitId)).thenReturn(2L);

        Department department = new Department();
        department.setName("Cardiology & Imaging");
        VisitDepartment visitDepartment = new VisitDepartment();
        visitDepartment.setId(visitDepartmentId);
        visitDepartment.setVisit(visit);
        visitDepartment.setDepartment(department);

        String visitPrefix = builder.visitPrefix(visit, visitDate, clinicId);
        assertEquals(
            "clinic-" + clinicId
                + "/patient-" + patientId + "_Jane_Doe_250_788_123_456_2025-02-03_2"
                + "/visit-2025-02-03_" + visitId,
            visitPrefix
        );
        assertEquals(
            visitPrefix + "/departments/Cardiology_Imaging-" + visitDepartmentId,
            builder.departmentPrefix(visit, visitDepartment, visitDate, clinicId)
        );
    }
}
