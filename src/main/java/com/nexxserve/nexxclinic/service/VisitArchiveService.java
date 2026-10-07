package com.nexxserve.nexxclinic.service;

import com.nexxserve.nexxclinic.config.SupabaseProperties;
import com.nexxserve.nexxclinic.entity.ClinicProfile;
import com.nexxserve.nexxclinic.entity.Visit;
import com.nexxserve.nexxclinic.entity.VisitDepartment;
import com.nexxserve.nexxclinic.repository.ClinicProfileRepository;
import com.nexxserve.nexxclinic.repository.VisitDepartmentRepository;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Service
public class VisitArchiveService {

    private static final Logger log = LoggerFactory.getLogger(VisitArchiveService.class);

    private final FileStorageService storageService;
    private final SupabaseProperties storageProperties;
    private final ClinicProfileRepository clinicProfileRepository;
    private final VisitArchivePathBuilder pathBuilder;
    private final VisitDepartmentRepository visitDepartmentRepository;

    public VisitArchiveService(
            FileStorageService storageService,
            SupabaseProperties storageProperties,
            ClinicProfileRepository clinicProfileRepository,
            VisitArchivePathBuilder pathBuilder,
            VisitDepartmentRepository visitDepartmentRepository) {
        this.storageService = storageService;
        this.storageProperties = storageProperties;
        this.clinicProfileRepository = clinicProfileRepository;
        this.pathBuilder = pathBuilder;
        this.visitDepartmentRepository = visitDepartmentRepository;
    }

    public void scheduleFolderCreation(Visit visit, List<VisitDepartment> departments) {
        UUID clinicId = clinicId();
        List<String> directories = directoriesFor(visit, visit.getVisitDate(), clinicId, departments);
        runAfterCommit(() -> ensureDirectories(visit.getId(), directories));
    }

    public void scheduleFolderMove(Visit visit, LocalDateTime oldVisitDate, LocalDateTime newVisitDate) {
        if (oldVisitDate.equals(newVisitDate)) {
            return;
        }
        UUID clinicId = clinicId();
        String oldVisitPrefix = pathBuilder.visitPrefix(visit, oldVisitDate, clinicId);
        String newVisitPrefix = pathBuilder.visitPrefix(visit, newVisitDate, clinicId);
        String oldPatientPrefix = patientPrefix(oldVisitPrefix);
        String newPatientPrefix = patientPrefix(newVisitPrefix);
        List<VisitDepartment> departments = visitDepartmentRepository.findByVisitId(visit.getId());
        List<String> directories = directoriesFor(visit, newVisitDate, clinicId, departments);
        runAfterCommit(() -> {
            try {
                storageService.movePrefix(invoiceBucket(), oldPatientPrefix, newPatientPrefix);
                String oldVisitSuffix = oldVisitPrefix.substring(oldPatientPrefix.length());
                storageService.movePrefix(
                    invoiceBucket(),
                    newPatientPrefix + oldVisitSuffix,
                    newVisitPrefix
                );
            } catch (IOException e) {
                log.error(
                    "Failed to move visit archive {} to {}: {}",
                    oldPatientPrefix, newPatientPrefix, e.getMessage(), e
                );
            }
            ensureDirectories(visit.getId(), directories);
        });
    }

    private List<String> directoriesFor(
            Visit visit,
            LocalDateTime visitDate,
            UUID clinicId,
            List<VisitDepartment> departments) {
        String visitPrefix = pathBuilder.visitPrefix(visit, visitDate, clinicId);
        List<String> directories = new ArrayList<>();
        directories.add(visitPrefix);
        for (VisitDepartment department : departments) {
            String departmentPrefix = pathBuilder.departmentPrefix(
                visit, department, visitDate, clinicId
            );
            directories.add(departmentPrefix + "/consultations");
            directories.add(departmentPrefix + "/invoices");
        }
        return directories;
    }

    private void ensureDirectories(UUID visitId, List<String> directories) {
        try {
            for (String directory : directories) {
                storageService.ensureDirectory(invoiceBucket(), directory);
            }
            log.info("Visit archive folders ensured for visit {}", visitId);
        } catch (IOException e) {
            log.error("Failed to create archive folders for visit {}: {}", visitId, e.getMessage(), e);
        }
    }

    private void runAfterCommit(Runnable action) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            action.run();
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                action.run();
            }
        });
    }

    private UUID clinicId() {
        return clinicProfileRepository.findFirstByOrderByCreatedAtAsc()
            .map(ClinicProfile::getId)
            .orElse(null);
    }

    private String invoiceBucket() {
        String bucket = storageProperties.getBucketInvoices();
        return bucket == null || bucket.isBlank() ? "clinic-files" : bucket;
    }

    private String patientPrefix(String visitPrefix) {
        return visitPrefix.substring(0, visitPrefix.lastIndexOf("/visit-"));
    }
}
