package com.nexxserve.nexxclinic.service;

import com.nexxserve.nexxclinic.entity.Department;
import com.nexxserve.nexxclinic.entity.Patient;
import com.nexxserve.nexxclinic.entity.Visit;
import com.nexxserve.nexxclinic.entity.VisitDepartment;
import com.nexxserve.nexxclinic.graphql.input.AddVisitVitalSignItemInput;
import com.nexxserve.nexxclinic.graphql.input.AddVisitVitalSignsInput;
import com.nexxserve.nexxclinic.graphql.input.AddDiagnosisInput;
import com.nexxserve.nexxclinic.graphql.input.AddMedicationInput;
import com.nexxserve.nexxclinic.model.ApiResponse;
import com.nexxserve.nexxclinic.model.ResponseStatus;
import com.nexxserve.nexxclinic.model.VisitDepartmentStatus;
import com.nexxserve.nexxclinic.model.VisitStatus;
import com.nexxserve.nexxclinic.repository.DepartmentRepository;
import com.nexxserve.nexxclinic.repository.PatientRepository;
import com.nexxserve.nexxclinic.repository.VisitDepartmentRepository;
import com.nexxserve.nexxclinic.repository.VisitRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class VisitServiceTest {

    @Autowired
    private VisitService visitService;

    @Autowired
    private VisitRepository visitRepository;

    @Autowired
    private DepartmentRepository departmentRepository;

    @Autowired
    private VisitDepartmentRepository visitDepartmentRepository;

    @Autowired
    private PatientRepository patientRepository;

    private VisitDepartment visitDepartment;

    @BeforeEach
    void setUp() {
        Patient patient = new Patient();
        patient.setFirstName("John");
        patient.setLastName("Doe");
        patient.setFullName("John Doe");
        patient.setDateOfBirth(java.time.LocalDate.now().minusYears(30));
        patient.setGender(com.nexxserve.nexxclinic.model.Gender.MALE);
        patient = patientRepository.save(patient);

        Visit visit = new Visit();
        visit.setPatient(patient);
        visit.setStatus(VisitStatus.IN_PROGRESS);
        visit.setVisitDate(java.time.LocalDateTime.now());
        visit = visitRepository.save(visit);

        Department department = new Department();
        department.setName("General Medicine");
        department = departmentRepository.save(department);

        visitDepartment = new VisitDepartment();
        visitDepartment.setVisit(visit);
        visitDepartment.setDepartment(department);
        visitDepartment.setStatus(VisitDepartmentStatus.PENDING);
        visitDepartment = visitDepartmentRepository.save(visitDepartment);
    }

    @Test
    void testAddDiagnosis() {
        AddDiagnosisInput input = new AddDiagnosisInput(
                visitDepartment.getId().toString(),
                "Flu",
                "ICD-10-Flu"
        );

        ApiResponse response = visitService.addDiagnosisToVisitDepartment(input);
        assertEquals(ResponseStatus.SUCCESS, response.status());
        assertNotNull(response.data());

        Map<String, Object> data = (Map<String, Object>) response.data();
        assertNotNull(data.get("id"));
        assertEquals("Flu", data.get("diagnosisName"));
        assertEquals("ICD-10-Flu", data.get("icd11Code"));
    }

    @Test
    void testAddMedication() {
        AddMedicationInput input = new AddMedicationInput(
                visitDepartment.getId().toString(),
                "Paracetamol",
                "Take 500mg twice a day"
        );

        ApiResponse response = visitService.addMedicationToVisitDepartment(input);
        assertEquals(ResponseStatus.SUCCESS, response.status());
        assertNotNull(response.data());

        Map<String, Object> data = (Map<String, Object>) response.data();
        assertNotNull(data.get("id"));
        assertEquals("Paracetamol", data.get("medicationName"));
        assertEquals("Take 500mg twice a day", data.get("instructions"));
    }

    @Test
    void testAddVisitVitalSigns() {
        AddVisitVitalSignItemInput weight = new AddVisitVitalSignItemInput("Weight", "58", "kg");
        AddVisitVitalSignItemInput pulse = new AddVisitVitalSignItemInput("Pulse rate", "72", "bpm");
        AddVisitVitalSignsInput input = new AddVisitVitalSignsInput(
                visitDepartment.getVisit().getId(),
                java.util.List.of(weight, pulse)
        );

        ApiResponse response = visitService.addVisitVitalSigns(input, null);
        assertEquals(ResponseStatus.SUCCESS, response.status());
        assertNotNull(response.data());

        Map<String, Object> data = (Map<String, Object>) response.data();
        assertTrue(data.containsKey("vitalSigns"));
        java.util.List<?> vitalSigns = (java.util.List<?>) data.get("vitalSigns");
        assertEquals(1, vitalSigns.size());

        Map<String, Object> group = (Map<String, Object>) vitalSigns.get(0);
        assertNotNull(group.get("id"));
        assertNotNull(group.get("createdAt"));
        assertNull(group.get("addedBy"));

        java.util.List<?> measurements = (java.util.List<?>) group.get("measurements");
        assertEquals(2, measurements.size());

        Map<String, Object> first = (Map<String, Object>) measurements.get(0);
        assertEquals("Weight", first.get("measurementName"));
        assertEquals("58", first.get("value"));
        assertEquals("kg", first.get("unit"));
    }
}
