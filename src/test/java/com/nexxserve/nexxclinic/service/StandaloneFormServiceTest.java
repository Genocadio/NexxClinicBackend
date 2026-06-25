package com.nexxserve.nexxclinic.service;

import com.nexxserve.nexxclinic.dto.out.ApiResponse;
import com.nexxserve.nexxclinic.dto.out.VisitStandaloneAnswerDto;
import com.nexxserve.nexxclinic.entity.*;
import com.nexxserve.nexxclinic.model.*;
import com.nexxserve.nexxclinic.repository.*;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class StandaloneFormServiceTest {

    @Autowired
    private StandaloneFormService standaloneFormService;

    @Autowired
    private StandaloneFormRepository formRepository;

    @Autowired
    private StandaloneFormVersionRepository versionRepository;

    @Autowired
    private DepartmentRepository departmentRepository;

    @Autowired
    private VisitRepository visitRepository;

    @Autowired
    private VisitDepartmentRepository visitDepartmentRepository;

    @Autowired
    private PatientRepository patientRepository;

    @Test
    void testSaveVisitStandaloneAnswer() {
        // 1. Setup Data
        Department department = new Department();
        department.setName("Test Department " + UUID.randomUUID());
        department = departmentRepository.save(department);

        StandaloneForm form = new StandaloneForm();
        form.setName("Test Form " + UUID.randomUUID());
        form.setType("GENERAL");
        form = formRepository.save(form);

        StandaloneFormVersion version = new StandaloneFormVersion();
        version.setForm(form);
        version.setVersionLabel("1.0.0");
        version.setMajorVersion(1);
        version.setMinorVersion(0);
        version.setBlocks("{}");
        version.setStatus(FormStatus.FINAL);
        version = versionRepository.save(version);

        Patient patient = new Patient();
        patient.setFirstName("John");
        patient.setLastName("Doe");
        patient.setFullName("John Doe");
        patient.setDateOfBirth(LocalDate.of(1990, 1, 1));
        patient.setGender(Gender.MALE);
        patient = patientRepository.save(patient);

        Visit visit = new Visit();
        visit.setPatient(patient);
        visit.setStatus(VisitStatus.CREATED);
        visit.setVisitDate(LocalDateTime.now());
        visit = visitRepository.save(visit);

        VisitDepartment visitDept = new VisitDepartment();
        visitDept.setVisit(visit);
        visitDept.setDepartment(department);
        visitDept.setStatus(VisitDepartmentStatus.ACTIVE);
        visitDept = visitDepartmentRepository.save(visitDept);

        // 2. Execute Mutation logic via service
        Map<String, Object> answers = Map.of("q1", "a1");
        ApiResponse<VisitStandaloneAnswerDto> response = standaloneFormService.saveVisitStandaloneAnswer(
                visit.getId(),
                department.getId(),
                version.getId(),
                answers,
                AnswerStatus.FINAL,
                85.5,
                null,
                null
        );

        // 3. Verify
        assertEquals(ResponseStatus.SUCCESS, response.status());
        assertNotNull(response.data());
        assertNotNull(response.data().answer());
        assertNotNull(response.data().visitDepartment());
        assertEquals(department.getId(), response.data().visitDepartment().department().id());
        
        // Check if answerId is updated in VisitDepartment
        VisitDepartment updatedVisitDept = visitDepartmentRepository.findById(visitDept.getId()).orElseThrow();
        assertNotNull(updatedVisitDept.getAnswerId());
        assertEquals(response.data().answer().id(), updatedVisitDept.getAnswerId());
        
        // Check if answer is saved with correct data
        assertEquals(visit.getId(), response.data().answer().visitId());
        assertEquals(patient.getId(), response.data().answer().patientId());
        assertEquals(0, new BigDecimal("85.5").compareTo(response.data().answer().score()));
        assertEquals(AnswerStatus.FINAL, response.data().answer().status());
    }
}
