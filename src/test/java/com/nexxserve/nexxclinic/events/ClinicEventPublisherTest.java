package com.nexxserve.nexxclinic.events;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.nexxserve.nexxclinic.entity.Department;
import com.nexxserve.nexxclinic.entity.Patient;
import com.nexxserve.nexxclinic.entity.Visit;
import com.nexxserve.nexxclinic.entity.VisitDepartment;
import com.nexxserve.nexxclinic.model.RoleName;
import com.nexxserve.nexxclinic.model.VisitDepartmentStatus;
import com.nexxserve.nexxclinic.model.VisitStatus;
import com.nexxserve.nexxclinic.repository.VisitDepartmentRepository;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

class ClinicEventPublisherTest {

    @Test
    void departmentStateEventIsBroadcastAfterCommitAndTargetsFinance() {
        ClinicSseRegistry registry = mock(ClinicSseRegistry.class);
        VisitDepartmentRepository visitDepartmentRepository = mock(VisitDepartmentRepository.class);
        ClinicEventPublisher publisher = new ClinicEventPublisher(registry, visitDepartmentRepository);

        UUID visitId = UUID.randomUUID();
        UUID departmentId = UUID.randomUUID();
        Visit visit = new Visit();
        visit.setId(visitId);
        visit.setStatus(VisitStatus.IN_PROGRESS);
        Patient patient = new Patient();
        patient.setId(UUID.randomUUID());
        patient.setFirstName("Test");
        patient.setLastName("Patient");
        visit.setPatient(patient);

        Department department = new Department();
        department.setId(departmentId);
        department.setName("Imaging");
        VisitDepartment visitDepartment = new VisitDepartment();
        visitDepartment.setId(UUID.randomUUID());
        visitDepartment.setVisit(visit);
        visitDepartment.setDepartment(department);
        visitDepartment.setStatus(VisitDepartmentStatus.BILLING);

        TransactionSynchronizationManager.setActualTransactionActive(true);
        TransactionSynchronizationManager.initSynchronization();
        try {
            publisher.publishDepartmentStateChanged(visitDepartment, null);
            verifyNoInteractions(registry);

            List<TransactionSynchronization> synchronizations =
                    TransactionSynchronizationManager.getSynchronizations();
            synchronizations.forEach(TransactionSynchronization::afterCommit);

            var eventCaptor = org.mockito.ArgumentCaptor.forClass(ClinicSseEvent.class);
            verify(registry).broadcast(eventCaptor.capture());
            ClinicSseEvent event = eventCaptor.getValue();
            assertEquals("VISIT_DEPARTMENT_STATE_CHANGED", event.type());
            assertEquals(VisitDepartmentStatus.BILLING, event.departmentStatus());
            assertEquals(Set.of(departmentId), event.targetDepartmentIds());
            assertTrue(event.targetRoles().contains(RoleName.FINANCE));
        } finally {
            if (TransactionSynchronizationManager.isSynchronizationActive()) {
                TransactionSynchronizationManager.clearSynchronization();
            }
            TransactionSynchronizationManager.setActualTransactionActive(false);
        }
    }

    @Test
    void departmentStateEventIsNotBroadcastWhenTransactionRollsBack() {
        ClinicSseRegistry registry = mock(ClinicSseRegistry.class);
        ClinicEventPublisher publisher = new ClinicEventPublisher(
                registry, mock(VisitDepartmentRepository.class));
        Visit visit = new Visit();
        visit.setId(UUID.randomUUID());
        VisitDepartment visitDepartment = new VisitDepartment();
        visitDepartment.setId(UUID.randomUUID());
        visitDepartment.setVisit(visit);
        visitDepartment.setStatus(VisitDepartmentStatus.BILLING);

        TransactionSynchronizationManager.setActualTransactionActive(true);
        TransactionSynchronizationManager.initSynchronization();
        try {
            publisher.publishDepartmentStateChanged(visitDepartment, null);
            TransactionSynchronizationManager.getSynchronizations().forEach(
                    synchronization -> synchronization.afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK));
            verifyNoInteractions(registry);
        } finally {
            if (TransactionSynchronizationManager.isSynchronizationActive()) {
                TransactionSynchronizationManager.clearSynchronization();
            }
            TransactionSynchronizationManager.setActualTransactionActive(false);
        }
    }

    @Test
    void eventBroadcastsImmediatelyWhenNoTransactionIsActive() {
        ClinicSseRegistry registry = mock(ClinicSseRegistry.class);
        VisitDepartmentRepository visitDepartmentRepository = mock(VisitDepartmentRepository.class);

        ClinicEventPublisher publisher = new ClinicEventPublisher(registry, visitDepartmentRepository);

        Visit visit = new Visit();
        visit.setId(UUID.randomUUID());
        visit.setStatus(VisitStatus.IN_PROGRESS);
        VisitDepartment visitDepartment = new VisitDepartment();
        visitDepartment.setId(UUID.randomUUID());
        visitDepartment.setVisit(visit);
        visitDepartment.setStatus(VisitDepartmentStatus.ACTIVE);

        publisher.publishDepartmentStateChanged(visitDepartment, null);

        verify(registry).broadcast(any(ClinicSseEvent.class));
    }
}
