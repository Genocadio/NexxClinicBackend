package com.nexxserve.nexxclinic.service;

import com.nexxserve.nexxclinic.entity.ClinicProfile;
import com.nexxserve.nexxclinic.entity.DepartmentInsuranceBilling;
import com.nexxserve.nexxclinic.entity.Visit;
import com.nexxserve.nexxclinic.entity.VisitDepartment;
import com.nexxserve.nexxclinic.repository.VisitRepository;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class VisitArchivePathBuilder {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ISO_LOCAL_DATE;

    private final VisitRepository visitRepository;

    public VisitArchivePathBuilder(VisitRepository visitRepository) {
        this.visitRepository = visitRepository;
    }

    public String visitPrefix(Visit visit, LocalDateTime visitDate, UUID clinicId) {
        var patient = visit.getPatient();
        String date = DATE_FORMAT.format(visitDate);
        long visitNumber = visitRepository.countPatientVisitsCreatedThrough(
            patient.getId(), visit.getCreatedAt(), visit.getId()
        );
        String patientFolder = "patient-" + patient.getId()
            + "_" + safe(patient.getFullName())
            + "_" + safeOrDefault(patient.getPrimaryPhoneNumber(), "no-phone")
            + "_" + date
            + "_" + visitNumber;

        return "clinic-" + (clinicId == null ? "unknown" : clinicId)
            + "/" + patientFolder
            + "/visit-" + date + "_" + visit.getId();
    }

    public String departmentPrefix(
            Visit visit,
            VisitDepartment department,
            LocalDateTime visitDate,
            UUID clinicId) {
        return visitPrefix(visit, visitDate, clinicId)
            + "/departments/"
            + safeOrDefault(department.getDepartment().getName(), "department")
            + "-" + department.getId();
    }

    public String invoicePath(
            DepartmentInsuranceBilling billing,
            ClinicProfile clinicProfile,
            String copyType) {
        var departmentBilling = billing.getVisitDepartmentBilling();
        VisitDepartment visitDepartment = departmentBilling.getVisitDepartment();
        Visit visit = departmentBilling.getVisitBilling().getVisit();
        UUID clinicId = clinicProfile == null ? null : clinicProfile.getId();
        String departmentPrefix = departmentPrefix(
            visit, visitDepartment, visit.getVisitDate(), clinicId
        );
        String copy = switch (copyType.toUpperCase(Locale.ROOT)) {
            case "PATIENT" -> "patient";
            case "INSURANCE" -> "insurance";
            default -> "default";
        };
        return departmentPrefix + "/invoices/billing-" + billing.getId()
            + "/invoice-" + copy + ".pdf";
    }

    private String safeOrDefault(String value, String fallback) {
        String safe = safe(value);
        return safe.isBlank() ? fallback : safe;
    }

    private String safe(String value) {
        if (value == null) {
            return "";
        }
        return value.trim()
            .replaceAll("[^A-Za-z0-9_-]+", "_")
            .replaceAll("^_+|_+$", "");
    }
}
