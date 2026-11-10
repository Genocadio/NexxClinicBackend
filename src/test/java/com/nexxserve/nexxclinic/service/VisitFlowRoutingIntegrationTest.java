package com.nexxserve.nexxclinic.service;

import com.nexxserve.nexxclinic.auth.AuthenticatedUser;
import com.nexxserve.nexxclinic.dto.out.ApiResponse;
import com.nexxserve.nexxclinic.dto.out.VisitDto;
import com.nexxserve.nexxclinic.entity.*;
import com.nexxserve.nexxclinic.model.*;
import com.nexxserve.nexxclinic.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class VisitFlowRoutingIntegrationTest {

    @Autowired
    private VisitFlowService visitFlowService;

    @Autowired
    private VisitRepository visitRepository;

    @Autowired
    private VisitDepartmentRepository visitDepartmentRepository;

    @Autowired
    private DepartmentRepository departmentRepository;

    @Autowired
    private PatientRepository patientRepository;

    @Autowired
    private WorkerRepository workerRepository;

    @Autowired
    private VisitDepartmentTransitionRepository transitionRepository;

    private Worker testWorker;
    private Patient testPatient;
    private Department consultationDept;
    private Department labDept;
    private Department imagingDept;
    private AuthenticatedUser authUser;

    @BeforeEach
    void setUp() {
        testWorker = new Worker();
        testWorker.setEmail("flow_worker_" + UUID.randomUUID() + "@test.com");
        testWorker.setFirstName("Dr. Flow");
        testWorker.setLastName("Tester");
        testWorker.setAccountStatus(AccountStatus.ACTIVE);
        testWorker.setPasswordHash("hashed");
        testWorker = workerRepository.save(testWorker);

        authUser = new AuthenticatedUser(
                testWorker.getId(),
                testWorker.getEmail(),
                Set.of(RoleName.CLINICIAN, RoleName.FINANCE),
                "token-123",
                Instant.now().plusSeconds(3600)
        );

        testPatient = new Patient();
        testPatient.setPatientIdentifier("PT-" + UUID.randomUUID().toString().substring(0, 8));
        testPatient.setFirstName("Patient");
        testPatient.setLastName("Flow");
        testPatient.setFullName("Patient Flow");
        testPatient.setGender(Gender.MALE);
        testPatient.setDateOfBirth(LocalDate.of(1990, 1, 1));
        testPatient = patientRepository.save(testPatient);

        consultationDept = new Department();
        consultationDept.setName("General Consultation " + UUID.randomUUID());
        consultationDept.setKind(DepartmentKind.CONSULTATION);
        consultationDept.setInsurancePolicyMode(DepartmentInsurancePolicyMode.ALL);
        consultationDept = departmentRepository.save(consultationDept);

        labDept = new Department();
        labDept.setName("Clinical Laboratory " + UUID.randomUUID());
        labDept.setKind(DepartmentKind.LABORATORY);
        labDept.setInsurancePolicyMode(DepartmentInsurancePolicyMode.ALL);
        labDept = departmentRepository.save(labDept);

        imagingDept = new Department();
        imagingDept.setName("Radiology / Imaging " + UUID.randomUUID());
        imagingDept.setKind(DepartmentKind.IMAGING);
        imagingDept.setInsurancePolicyMode(DepartmentInsurancePolicyMode.ALL);
        imagingDept = departmentRepository.save(imagingDept);
    }

    @Test
    void testDischargeVisitCompletesDepartmentsAndMarksDischarged() {
        Visit visit = new Visit();
        visit.setPatient(testPatient);
        visit.setStatus(VisitStatus.IN_PROGRESS);
        visit.setVisitDate(LocalDateTime.now());
        visit = visitRepository.save(visit);

        VisitDepartment vd = new VisitDepartment();
        vd.setVisit(visit);
        vd.setDepartment(consultationDept);
        vd.setStatus(VisitDepartmentStatus.BILLING);
        vd = visitDepartmentRepository.save(vd);

        ApiResponse<VisitDto> response = visitFlowService.dischargeVisit(
                visit.getId(),
                vd.getId(),
                "All items paid, discharging patient",
                authUser
        );

        assertThat(response.status()).isEqualTo(ResponseStatus.SUCCESS);
        Visit updatedVisit = visitRepository.findById(visit.getId()).orElseThrow();
        assertThat(updatedVisit.getStatus()).isEqualTo(VisitStatus.COMPLETED);
        assertThat(updatedVisit.getIsDischarged()).isTrue();
        assertThat(updatedVisit.getActiveEpisodeCount()).isEqualTo(0);

        VisitDepartment updatedVd = visitDepartmentRepository.findById(vd.getId()).orElseThrow();
        assertThat(updatedVd.getStatus()).isEqualTo(VisitDepartmentStatus.COMPLETED);
        assertThat(updatedVd.getLastRoutingAction()).isEqualTo("DISCHARGED");

        List<VisitDepartmentTransition> transitions = transitionRepository.findByVisitIdOrderByOccurredAtAsc(visit.getId());
        assertThat(transitions).isNotEmpty();
        assertThat(transitions.get(transitions.size() - 1).getActionType()).isEqualTo("DISCHARGE");
    }

    @Test
    void testSendBackFromBillingToDoctor() {
        Visit visit = new Visit();
        visit.setPatient(testPatient);
        visit.setStatus(VisitStatus.IN_PROGRESS);
        visit.setVisitDate(LocalDateTime.now());
        visit = visitRepository.save(visit);

        VisitDepartment consultVd = new VisitDepartment();
        consultVd.setVisit(visit);
        consultVd.setDepartment(consultationDept);
        consultVd.setStatus(VisitDepartmentStatus.COMPLETED);
        consultVd = visitDepartmentRepository.save(consultVd);

        ApiResponse<VisitDto> response = visitFlowService.sendBackVisit(
                visit.getId(),
                consultVd.getId(),
                consultVd.getId(),
                "Need doctor revision on prescription",
                authUser
        );

        assertThat(response.status()).isEqualTo(ResponseStatus.SUCCESS);
        Visit updatedVisit = visitRepository.findById(visit.getId()).orElseThrow();
        assertThat(updatedVisit.getStatus()).isEqualTo(VisitStatus.IN_PROGRESS);
        assertThat(updatedVisit.getHasReturnedFromBilling()).isTrue();

        VisitDepartment updatedVd = visitDepartmentRepository.findById(consultVd.getId()).orElseThrow();
        assertThat(updatedVd.getStatus()).isEqualTo(VisitDepartmentStatus.ACTIVE);
        assertThat(updatedVd.getAwaitingRole()).isEqualTo("CLINICIAN");
        assertThat(updatedVd.getLastRoutingAction()).isEqualTo("SENT_BACK");

        List<VisitDepartmentTransition> transitions = transitionRepository.findByVisitIdOrderByOccurredAtAsc(visit.getId());
        assertThat(transitions).isNotEmpty();
        assertThat(transitions.get(transitions.size() - 1).getActionType()).isEqualTo("SEND_BACK");
    }

    @Test
    void testSendBackWithoutTargetDepartmentIdFallbackToConsultation() {
        Visit visit = new Visit();
        visit.setPatient(testPatient);
        visit.setStatus(VisitStatus.IN_PROGRESS);
        visit.setVisitDate(LocalDateTime.now());
        visit = visitRepository.save(visit);

        VisitDepartment consultVd = new VisitDepartment();
        consultVd.setVisit(visit);
        consultVd.setDepartment(consultationDept);
        consultVd.setStatus(VisitDepartmentStatus.BILLING);
        consultVd = visitDepartmentRepository.save(consultVd);

        ApiResponse<VisitDto> response = visitFlowService.sendBackVisit(
                visit.getId(),
                consultVd.getId(),
                null,
                "Returned with null targetDepartmentId",
                authUser
        );

        assertThat(response.status()).isEqualTo(ResponseStatus.SUCCESS);
        Visit updatedVisit = visitRepository.findById(visit.getId()).orElseThrow();
        assertThat(updatedVisit.getStatus()).isEqualTo(VisitStatus.IN_PROGRESS);
        assertThat(updatedVisit.getHasReturnedFromBilling()).isTrue();

        VisitDepartment updatedVd = visitDepartmentRepository.findById(consultVd.getId()).orElseThrow();
        assertThat(updatedVd.getStatus()).isEqualTo(VisitDepartmentStatus.ACTIVE);
        assertThat(updatedVd.getAwaitingRole()).isEqualTo("CLINICIAN");
    }

    @Test
    void testSendVisitToNextActivatesLaboratory() {
        Visit visit = new Visit();
        visit.setPatient(testPatient);
        visit.setStatus(VisitStatus.IN_PROGRESS);
        visit.setVisitDate(LocalDateTime.now());
        visit = visitRepository.save(visit);

        VisitDepartment consultVd = new VisitDepartment();
        consultVd.setVisit(visit);
        consultVd.setDepartment(consultationDept);
        consultVd.setStatus(VisitDepartmentStatus.BILLING);
        consultVd = visitDepartmentRepository.save(consultVd);

        ApiResponse<VisitDto> response = visitFlowService.sendVisitToNext(
                visit.getId(),
                consultVd.getId(),
                labDept.getId(),
                "Routing to lab for blood draw",
                authUser
        );

        assertThat(response.status()).isEqualTo(ResponseStatus.SUCCESS);
        Visit updatedVisit = visitRepository.findById(visit.getId()).orElseThrow();
        assertThat(updatedVisit.getStatus()).isEqualTo(VisitStatus.IN_PROGRESS);
        assertThat(updatedVisit.getActiveEpisodeCount()).isGreaterThanOrEqualTo(1);
        assertThat(updatedVisit.getCurrentLocations()).contains("Laboratory");

        VisitDepartment labVd = visitDepartmentRepository.findByVisitIdAndDepartmentId(visit.getId(), labDept.getId())
                .orElseThrow();
        assertThat(labVd.getStatus()).isEqualTo(VisitDepartmentStatus.ACTIVE);
        assertThat(labVd.getAwaitingRole()).isEqualTo("LAB");
        assertThat(labVd.getLastRoutingAction()).isEqualTo("SENT_TO_NEXT");
        assertThat(labVd.getPreviousDepartment().getId()).isEqualTo(consultationDept.getId());

        List<VisitDepartmentTransition> transitions = transitionRepository.findByVisitIdOrderByOccurredAtAsc(visit.getId());
        assertThat(transitions).isNotEmpty();
        assertThat(transitions.get(transitions.size() - 1).getActionType()).isEqualTo("SEND_TO_NEXT");
    }
}
