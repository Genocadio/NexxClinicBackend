package com.nexxserve.nexxclinic.service;

import com.nexxserve.nexxclinic.auth.AuthenticatedUser;
import com.nexxserve.nexxclinic.dto.out.ApiResponse;
import com.nexxserve.nexxclinic.dto.out.PatientInsuranceDto;
import com.nexxserve.nexxclinic.dto.out.VisitDepartmentDiagnosisDto;
import com.nexxserve.nexxclinic.dto.out.VisitDepartmentDto;
import com.nexxserve.nexxclinic.dto.out.VisitDepartmentMedicationDto;
import com.nexxserve.nexxclinic.dto.out.VisitDepartmentProductDto;
import com.nexxserve.nexxclinic.dto.out.VisitDepartmentSymptomDto;
import com.nexxserve.nexxclinic.dto.out.VisitDto;
import com.nexxserve.nexxclinic.dto.out.VisitVitalSignsGroupDto;
import com.nexxserve.nexxclinic.dto.out.VitalMeasurementDto;
import com.nexxserve.nexxclinic.entity.Visit;
import com.nexxserve.nexxclinic.entity.Worker;
import com.nexxserve.nexxclinic.model.RoleName;
import com.nexxserve.nexxclinic.repository.VisitRepository;
import com.nexxserve.nexxclinic.repository.WorkerRepository;
import com.nexxserve.nexxclinic.service.billing.BillingQueryService;
import jakarta.persistence.criteria.Predicate;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReportsService {

    private static final Logger log = LoggerFactory.getLogger(ReportsService.class);

    private final VisitRepository visitRepository;
    private final WorkerRepository workerRepository;
    private final VisitService visitService;
    private final BillingQueryService billingQueryService;

    public ReportsService(
            VisitRepository visitRepository,
            WorkerRepository workerRepository,
            VisitService visitService,
            BillingQueryService billingQueryService
    ) {
        this.visitRepository = visitRepository;
        this.workerRepository = workerRepository;
        this.visitService = visitService;
        this.billingQueryService = billingQueryService;
    }

    @Transactional(readOnly = true)
    public ApiResponse getUserReports(
            String fromDate,
            String toDate,
            String periodInput,
            UUID targetWorkerId,
            AuthenticatedUser authUser
    ) {
        if (authUser == null || authUser.userId() == null) {
            return ApiResponse.unauthenticated("Authentication is required to view reports.");
        }

        LocalDate today = LocalDate.now();
        LocalDate startDate;
        LocalDate endDate;

        try {
            if (fromDate != null && !fromDate.isBlank()) {
                startDate = parseLocalDate(fromDate);
            } else {
                startDate = today;
            }
        } catch (Exception e) {
            startDate = today;
        }

        try {
            if (toDate != null && !toDate.isBlank()) {
                endDate = parseLocalDate(toDate);
            } else {
                endDate = startDate;
            }
        } catch (Exception e) {
            endDate = startDate;
        }

        if (endDate.isBefore(startDate)) {
            LocalDate tmp = startDate;
            startDate = endDate;
            endDate = tmp;
        }

        String fromDateStr = startDate.format(DateTimeFormatter.ISO_LOCAL_DATE);
        String toDateStr = endDate.format(DateTimeFormatter.ISO_LOCAL_DATE);

        long diffDays = java.time.temporal.ChronoUnit.DAYS.between(startDate, endDate);
        String period = (periodInput != null && !periodInput.isBlank())
                ? periodInput.toLowerCase().trim()
                : (diffDays == 0 ? "today" : (diffDays <= 7 ? "week" : (diffDays <= 31 ? "month" : "custom")));

        // Determine worker scope
        boolean isAdminOrManager = authUser.roles() != null && authUser.roles().stream().anyMatch(r ->
                r == RoleName.ADMIN || r == RoleName.MANAGER || r == RoleName.CLINIC_ADMIN
        );

        UUID effectiveWorkerId = authUser.userId();
        if (targetWorkerId != null && isAdminOrManager) {
            effectiveWorkerId = targetWorkerId;
        }

        Worker worker = workerRepository.findById(effectiveWorkerId).orElse(null);
        String workerIdStr = effectiveWorkerId.toString();
        String workerName = "User";
        Set<String> workerRoleNames = new HashSet<>();

        if (worker != null) {
            String fName = worker.getFirstName() != null ? worker.getFirstName().trim() : "";
            String lName = worker.getLastName() != null ? worker.getLastName().trim() : "";
            workerName = (!fName.isEmpty() || !lName.isEmpty()) ? (fName + " " + lName).trim() : (worker.getUsername() != null ? worker.getUsername() : "User");
            if (worker.getRoles() != null) {
                workerRoleNames = worker.getRoles().stream().map(Enum::name).collect(Collectors.toSet());
            }
        } else if (authUser.roles() != null) {
            workerRoleNames = authUser.roles().stream().map(Enum::name).collect(Collectors.toSet());
        }

        boolean hasReception = workerRoleNames.contains("RECEPTION");
        boolean hasClinician = workerRoleNames.contains("CLINICIAN");
        boolean hasNurse = workerRoleNames.contains("NURSE");
        boolean hasFinance = workerRoleNames.contains("FINANCE");

        List<String> allowedTabs = new ArrayList<>();
        if (hasClinician) allowedTabs.add("clinician");
        if (hasFinance) allowedTabs.add("finance");
        if (hasNurse) allowedTabs.add("nurse");
        if (hasReception) allowedTabs.add("reception");

        if (allowedTabs.isEmpty()) {
            if (isAdminOrManager) {
                allowedTabs.add("clinician");
                allowedTabs.add("finance");
            } else {
                return ApiResponse.error("You do not have permission to view reports.");
            }
        }

        boolean canViewFinance = authUser.roles() != null && authUser.roles().stream().anyMatch(r ->
                r == RoleName.FINANCE || r == RoleName.CLINICIAN || r == RoleName.ADMIN || r == RoleName.MANAGER
        );

        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(LocalTime.MAX);

        // Query all visits in range
        Specification<Visit> spec = (root, queryDef, builder) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(builder.or(
                    builder.and(
                            builder.greaterThanOrEqualTo(root.get("visitDate"), startDateTime),
                            builder.lessThanOrEqualTo(root.get("visitDate"), endDateTime)
                    ),
                    builder.and(
                            builder.greaterThanOrEqualTo(root.get("createdAt"), startDateTime),
                            builder.lessThanOrEqualTo(root.get("createdAt"), endDateTime)
                    )
            ));
            return builder.and(predicates.toArray(new Predicate[0]));
        };

        List<Visit> rawVisits = visitRepository.findAll(spec, Sort.by(Sort.Direction.DESC, "visitDate"));

        // Convert to full VisitDtos
        List<VisitDto> visits = rawVisits.stream()
                .map(v -> visitService.visitToDto(v, Set.of(), authUser))
                .toList();

        // Calculate report
        Map<String, Object> reportData = calculateReportsData(
                visits,
                workerIdStr,
                workerName,
                workerRoleNames,
                period,
                fromDateStr,
                toDateStr,
                hasReception,
                hasClinician,
                hasNurse,
                hasFinance,
                isAdminOrManager,
                canViewFinance,
                allowedTabs,
                startDate,
                endDate,
                diffDays
        );

        return ApiResponse.success("User reports generated successfully.", reportData);
    }

    private LocalDate parseLocalDate(String raw) {
        String trimmed = raw.trim();
        if (trimmed.contains("T")) {
            return LocalDateTime.parse(trimmed.endsWith("Z") ? trimmed.substring(0, trimmed.length() - 1) : trimmed).toLocalDate();
        }
        return LocalDate.parse(trimmed, DateTimeFormatter.ISO_LOCAL_DATE);
    }

    private String toDateKey(String isoOrDateString) {
        if (isoOrDateString == null || isoOrDateString.isBlank()) {
            return LocalDate.now().toString();
        }
        return isoOrDateString.length() >= 10 ? isoOrDateString.substring(0, 10) : isoOrDateString;
    }

    private boolean isWithinDateRange(String timestamp, String fromDate, String toDate) {
        if (timestamp == null || timestamp.isBlank()) return false;
        String key = toDateKey(timestamp);
        return key.compareTo(fromDate) >= 0 && key.compareTo(toDate) <= 0;
    }

    private Integer calculatePatientAge(String dateOfBirth, Integer fallbackAge) {
        if (fallbackAge != null && fallbackAge > 0) return fallbackAge;
        if (dateOfBirth == null || dateOfBirth.isBlank()) return null;
        try {
            LocalDate dob = parseLocalDate(dateOfBirth);
            int age = Period.between(dob, LocalDate.now()).getYears();
            return (age >= 0 && age <= 130) ? age : null;
        } catch (Exception e) {
            return null;
        }
    }

    private Integer calculateEncounterDurationMinutes(String startedAt, String completedAt) {
        if (startedAt == null || completedAt == null) return null;
        try {
            LocalDateTime start = parseDateTime(startedAt);
            LocalDateTime end = parseDateTime(completedAt);
            if (start == null || end == null || end.isBefore(start)) return null;
            long diffMins = Duration.between(start, end).toMinutes();
            return (diffMins >= 0 && diffMins <= 720) ? (int) diffMins : null;
        } catch (Exception e) {
            return null;
        }
    }

    private LocalDateTime parseDateTime(String raw) {
        if (raw == null || raw.isBlank()) return null;
        try {
            String trimmed = raw.trim();
            if (trimmed.contains("T")) {
                String clean = trimmed.endsWith("Z") ? trimmed.substring(0, trimmed.length() - 1) : trimmed;
                return LocalDateTime.parse(clean, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            }
            return LocalDate.parse(trimmed, DateTimeFormatter.ISO_LOCAL_DATE).atStartOfDay();
        } catch (Exception e) {
            return null;
        }
    }

    private String formatHourRange(int hour) {
        int displayH = (hour % 12 == 0) ? 12 : (hour % 12);
        String period = (hour >= 12) ? "PM" : "AM";
        int nextH = (hour + 1) % 24;
        int nextDisplayH = (nextH % 12 == 0) ? 12 : (nextH % 12);
        String nextPeriod = (nextH >= 12) ? "PM" : "AM";
        return String.format("%02d:00 %s - %02d:00 %s", displayH, period, nextDisplayH, nextPeriod);
    }

    private String getInsuranceAcronym(String name, String acronym) {
        if (acronym != null && !acronym.trim().isEmpty()) {
            return acronym.trim();
        }
        if (name == null || name.trim().isEmpty()) {
            return null;
        }
        String fullName = name.trim();
        Matcher m = Pattern.compile("\\(([^)]+)\\)").matcher(fullName);
        if (m.find() && m.group(1) != null) {
            return m.group(1).trim();
        }
        if (fullName.length() <= 8) return fullName;
        String lower = fullName.toLowerCase();
        if (lower.contains("rwanda social security") || lower.contains("rssb")) return "RSSB";
        if (lower.contains("military medical") || lower.contains("mmi")) return "MMI";
        if (lower.contains("radiant")) return "RADIANT";
        if (lower.contains("prime")) return "PRIME";
        if (lower.contains("britam")) return "BRITAM";
        if (lower.contains("old mutual") || lower.contains("uap")) return "UAP";
        if (lower.contains("sanlam")) return "SANLAM";
        if (lower.contains("eden care")) return "EDEN CARE";
        if (lower.contains("mutuelle") || lower.contains("cbhi") || lower.contains("community based") || lower.contains("mituelle")) return "CBHI";
        if (lower.contains("sonarwa")) return "SONARWA";
        if (lower.contains("mayfair")) return "MAYFAIR";
        if (lower.contains("soras")) return "SORAS";
        return fullName;
    }

    private String resolveVisitInsuranceAcronym(VisitDepartmentDto dept, VisitDto visit, Map<String, Object> deptBilling, boolean hasInsuranceFallback) {
        if (deptBilling != null) {
            Object insBillingsObj = deptBilling.get("insuranceBillings");
            if (insBillingsObj instanceof List<?> list && !list.isEmpty()) {
                Object first = list.get(0);
                if (first instanceof Map<?, ?> map) {
                    Object pIns = map.get("patientInsurance");
                    if (pIns instanceof Map<?, ?> pMap) {
                        Object prov = pMap.get("insuranceProvider");
                        if (prov instanceof Map<?, ?> provMap) {
                            String acr = getInsuranceAcronym((String) provMap.get("name"), (String) provMap.get("acronym"));
                            if (acr != null) return acr;
                        }
                    }
                }
            }
        }

        if (visit != null && visit.linkedInsurances() != null && !visit.linkedInsurances().isEmpty()) {
            for (PatientInsuranceDto lIns : visit.linkedInsurances()) {
                if (lIns.insuranceProvider() != null) {
                    String acr = getInsuranceAcronym(lIns.insuranceProvider().insuranceName(), lIns.insuranceProvider().acronym());
                    if (acr != null) return acr;
                }
            }
        }

        return hasInsuranceFallback ? "Insured" : "Private / Cash";
    }

    private boolean isUserMatch(UUID userId, String workerId) {
        if (userId == null || workerId == null || workerId.isBlank()) return false;
        return userId.toString().equals(workerId);
    }

    private boolean isProcessorMatch(List<com.nexxserve.nexxclinic.dto.out.WorkerDto> processors, String workerId) {
        if (processors == null || processors.isEmpty() || workerId == null || workerId.isBlank()) return false;
        return processors.stream().anyMatch(p -> p != null && p.id() != null && p.id().toString().equals(workerId));
    }

    private boolean isUserInSet(UUID userId, Set<String> allowedIds) {
        if (userId == null || allowedIds == null || allowedIds.isEmpty()) return false;
        return allowedIds.contains(userId.toString());
    }

    private boolean isProcessorInSet(List<com.nexxserve.nexxclinic.dto.out.WorkerDto> processors, Set<String> allowedIds) {
        if (processors == null || processors.isEmpty() || allowedIds == null || allowedIds.isEmpty()) return false;
        return processors.stream().anyMatch(p -> p != null && p.id() != null && allowedIds.contains(p.id().toString()));
    }

    private Map<String, Object> calculateReportsData(
            List<VisitDto> visits,
            String workerId,
            String workerName,
            Set<String> workerRoleNames,
            String period,
            String fromDate,
            String toDate,
            boolean hasReception,
            boolean hasClinician,
            boolean hasNurse,
            boolean hasFinance,
            boolean isAdminOrManager,
            boolean canViewFinance,
            List<String> allowedTabs,
            LocalDate startDate,
            LocalDate endDate,
            long diffDays
    ) {
        List<Map<String, Object>> receptionActivities = new ArrayList<>();
        List<Map<String, Object>> clinicianActivities = new ArrayList<>();
        List<Map<String, Object>> nurseActivities = new ArrayList<>();
        List<Map<String, Object>> financeActivities = new ArrayList<>();

        Set<String> uniquePatientsSet = new HashSet<>();
        Set<String> receptionPatientsSet = new HashSet<>();

        int insuredVisitsCount = 0;
        int privateVisitsCount = 0;
        int visitsInitiatedCount = 0;
        int departmentsDispatchedCount = 0;

        int consultationsCount = 0;
        int consultationsCompletedCount = 0;
        int consultationsInProgressCount = 0;
        int prescriptionsCount = 0;
        int referralsCount = 0;
        Map<String, Integer> clinicianProductCategoryBreakdown = new HashMap<>();

        List<Map<String, Object>> clinicianEncountersList = new ArrayList<>();
        Set<String> clinicianPatientsSet = new HashSet<>();
        int clinicianMaleCount = 0;
        int clinicianFemaleCount = 0;
        int clinicianOtherGenderCount = 0;
        double clinicianTotalAgeSum = 0;
        int clinicianAgeCount = 0;
        int clinicianUnder18Count = 0;
        int clinicianAdults18to50Count = 0;
        int clinicianSeniors50plusCount = 0;
        long clinicianTotalDurationMinutes = 0;
        int clinicianDurationCount = 0;
        Map<String, Integer> clinicianEncountersByHour = new LinkedHashMap<>();

        Set<String> overallPatientsSet = new HashSet<>();
        int overallMaleCount = 0;
        int overallFemaleCount = 0;
        int overallOtherGenderCount = 0;
        double overallTotalAgeSum = 0;
        int overallAgeCount = 0;
        int overallUnder18Count = 0;
        int overallAdults18to50Count = 0;
        int overallSeniors50plusCount = 0;

        int vitalsRecordedCount = 0;
        int triageEncountersCount = 0;
        int nursingActsCount = 0;

        int billedItemsCount = 0;
        Set<String> billedDepartmentsSet = new HashSet<>();
        Map<String, Map<String, Object>> financeCategoryBreakdown = new HashMap<>();

        double totalGrossBilled = 0;
        double insuranceCoveredAmount = 0;
        double patientShareAmount = 0;
        double patientCashCollected = 0;
        double patientLoanAmount = 0;
        double giveawayAmount = 0;
        int loanCount = 0;
        int giveawayCount = 0;

        double momoCollected = 0;
        double cashCollected = 0;
        double cardCollected = 0;
        double bankTransferCollected = 0;
        double otherCollected = 0;
        Map<String, Map<String, Object>> paymentModesBreakdownMap = new HashMap<>();
        Map<String, Map<String, Object>> insuranceBreakdownMap = new HashMap<>();

        double clinicianTotalGrossBilled = 0;
        double clinicianInsuranceCoveredAmount = 0;
        double clinicianPatientShareAmount = 0;
        double clinicianPatientCashCollected = 0;
        double clinicianPatientLoanAmount = 0;
        double clinicianGiveawayAmount = 0;
        int clinicianLoanCount = 0;
        int clinicianGiveawayCount = 0;
        double clinicianPendingBillingAmount = 0;
        int clinicianPendingBillingCount = 0;

        double clinicianProductTurnoverTotal = 0;
        double clinicianProductItemsApprovedCount = 0;
        Map<String, Map<String, Object>> clinicianProductTurnoverByCategory = new HashMap<>();
        List<Map<String, Object>> clinicianProductTurnoverList = new ArrayList<>();

        Map<String, Map<String, Object>> clinicianDepartmentBreakdownMap = new HashMap<>();
        Map<String, Map<String, Object>> clinicianInsuranceBreakdownMap = new HashMap<>();

        boolean isDailyView = "today".equals(period) || fromDate.equals(toDate) || diffDays == 0;
        boolean isWeeklyView = !isDailyView && ("week".equals(period) || diffDays <= 7);

        // Timeline maps
        Map<String, Map<String, Integer>> timelineMap = new LinkedHashMap<>();
        Map<String, Map<String, Double>> moneyTimelineMap = new LinkedHashMap<>();
        Map<String, Map<String, Double>> clinicianMoneyTimelineMap = new LinkedHashMap<>();

        if (isDailyView) {
            for (int h = 6; h <= 20; h++) {
                String hKey = String.format("%02d:00", h);
                timelineMap.put(hKey, createActivityTimelineBucket());
                moneyTimelineMap.put(hKey, createMoneyTimelineBucket());
                clinicianMoneyTimelineMap.put(hKey, createMoneyTimelineBucket());
            }
        } else if (diffDays <= 31) {
            LocalDate cur = startDate;
            while (!cur.isAfter(endDate)) {
                String dKey = cur.format(DateTimeFormatter.ISO_LOCAL_DATE);
                timelineMap.put(dKey, createActivityTimelineBucket());
                moneyTimelineMap.put(dKey, createMoneyTimelineBucket());
                clinicianMoneyTimelineMap.put(dKey, createMoneyTimelineBucket());
                cur = cur.plusDays(1);
            }
        }

        // Process visits
        for (VisitDto visit : visits) {
            String visitDate = visit.visitDate() != null ? visit.visitDate().toString() : "";
            String patientName = (visit.patient() != null && visit.patient().fullName() != null && !visit.patient().fullName().isBlank())
                    ? visit.patient().fullName()
                    : "Unknown Patient";
            String patientId = visit.patient() != null && visit.patient().id() != null ? visit.patient().id().toString() : "";
            String patientIdentifier = visit.patient() != null ? visit.patient().patientIdentifier() : null;
            boolean hasInsurance = visit.linkedInsurances() != null && !visit.linkedInsurances().isEmpty();
            boolean patientTouchedByWorker = false;

            // 1. Vital Signs
            if (visit.vitalSigns() != null) {
                for (VisitVitalSignsGroupDto vs : visit.vitalSigns()) {
                    String vsTime = vs.createdAt() != null ? vs.createdAt().toString() : visitDate;
                    boolean isAddedByWorker = vs.addedBy() != null && isUserMatch(vs.addedBy().id(), workerId);
                    if (isAddedByWorker && isWithinDateRange(vsTime, fromDate, toDate)) {
                        vitalsRecordedCount++;
                        patientTouchedByWorker = true;
                        bumpActivityTimeline(timelineMap, vsTime, "nurse", isDailyView);

                        String measurementSummary = (vs.measurements() != null)
                                ? vs.measurements().stream()
                                        .map(m -> m.measurementName() + ": " + m.value() + (m.unit() != null ? " " + m.unit() : ""))
                                        .collect(Collectors.joining(", "))
                                : "Recorded vital signs";

                        Map<String, Object> act = new HashMap<>();
                        act.put("id", "vs-" + vs.id());
                        act.put("timestamp", vsTime);
                        act.put("role", "NURSE");
                        act.put("actionType", "Vital Signs Recorded");
                        act.put("patientName", patientName);
                        act.put("patientId", patientId);
                        act.put("patientIdentifier", patientIdentifier);
                        act.put("details", measurementSummary.isEmpty() ? "Recorded vital signs" : "Recorded: " + measurementSummary);
                        act.put("status", "RECORDED");
                        nurseActivities.add(act);
                    }
                }
            }

            // 2. Departments
            if (visit.departments() != null) {
                for (VisitDepartmentDto dept : visit.departments()) {
                    Map<String, Object> deptBilling = (dept.id() != null) ? billingQueryService.departmentBilling(dept.id()) : null;

                    String deptTime = dept.startedAt() != null ? dept.startedAt().toString() : (dept.createdAt() != null ? dept.createdAt().toString() : visitDate);
                    String deptId = dept.department() != null && dept.department().id() != null ? dept.department().id().toString() : (dept.id() != null ? dept.id().toString() : "");
                    String deptName = dept.department() != null && dept.department().name() != null ? dept.department().name() : "Consultation";
                    boolean isTriage = deptName.toLowerCase().contains("triage");

                    boolean isDeptAddedByWorker = dept.addedBy() != null && isUserMatch(dept.addedBy().id(), workerId);
                    if (isDeptAddedByWorker && isWithinDateRange(deptTime, fromDate, toDate)) {
                        departmentsDispatchedCount++;
                        patientTouchedByWorker = true;
                        if (!patientId.isEmpty()) receptionPatientsSet.add(patientId);

                        visitsInitiatedCount++;
                        if (hasInsurance) insuredVisitsCount++;
                        else privateVisitsCount++;

                        bumpActivityTimeline(timelineMap, deptTime, "reception", isDailyView);

                        Map<String, Object> rAct = new HashMap<>();
                        rAct.put("id", "dept-add-" + dept.id());
                        rAct.put("timestamp", deptTime);
                        rAct.put("role", "RECEPTION");
                        rAct.put("actionType", "Department Dispatched");
                        rAct.put("patientName", patientName);
                        rAct.put("patientId", patientId);
                        rAct.put("patientIdentifier", patientIdentifier);
                        rAct.put("departmentName", deptName);
                        rAct.put("details", "Dispatched to " + deptName + " (" + (hasInsurance ? "Insured" : "Private") + ")");
                        rAct.put("status", dept.status() != null ? dept.status().name() : "PENDING");
                        receptionActivities.add(rAct);
                    }

                    // Clinician check
                    boolean hasWorkerPrescribedOnDept = dept.products() != null && dept.products().stream().anyMatch(p ->
                            (p.addedBy() != null && isUserMatch(p.addedBy().id(), workerId)) ||
                            (p.processor() != null && isUserMatch(p.processor().id(), workerId))
                    );

                    boolean hasWorkerPrescribedInPeriod = dept.products() != null && dept.products().stream().anyMatch(p ->
                            ((p.addedBy() != null && isUserMatch(p.addedBy().id(), workerId)) ||
                             (p.processor() != null && isUserMatch(p.processor().id(), workerId))) &&
                            isWithinDateRange(p.createdAt() != null ? p.createdAt().toString() : deptTime, fromDate, toDate)
                    );

                    boolean isClinicianOnDept = isProcessorMatch(dept.processors(), workerId) ||
                            (dept.completedBy() != null && isUserMatch(dept.completedBy().id(), workerId)) ||
                            hasWorkerPrescribedOnDept ||
                            (isDeptAddedByWorker && !isTriage);

                    boolean isDeptInPeriod = isWithinDateRange(deptTime, fromDate, toDate) || hasWorkerPrescribedInPeriod;
                    String effectiveDeptTime = deptTime;
                    if (hasWorkerPrescribedInPeriod && dept.products() != null) {
                        for (VisitDepartmentProductDto p : dept.products()) {
                            if (((p.addedBy() != null && isUserMatch(p.addedBy().id(), workerId)) ||
                                 (p.processor() != null && isUserMatch(p.processor().id(), workerId))) &&
                                isWithinDateRange(p.createdAt() != null ? p.createdAt().toString() : deptTime, fromDate, toDate)) {
                                effectiveDeptTime = p.createdAt() != null ? p.createdAt().toString() : deptTime;
                                break;
                            }
                        }
                    }

                    if (isClinicianOnDept && isDeptInPeriod) {
                        consultationsCount++;
                        patientTouchedByWorker = true;
                        if (!patientId.isEmpty()) clinicianPatientsSet.add(patientId);

                        if (dept.status() == com.nexxserve.nexxclinic.model.VisitDepartmentStatus.COMPLETED) {
                            consultationsCompletedCount++;
                        } else {
                            consultationsInProgressCount++;
                        }

                        // Demographics
                        String gender = visit.patient() != null && visit.patient().gender() != null ? visit.patient().gender().name() : "";
                        if (gender.equalsIgnoreCase("MALE") || gender.startsWith("M")) {
                            clinicianMaleCount++;
                        } else if (gender.equalsIgnoreCase("FEMALE") || gender.startsWith("F")) {
                            clinicianFemaleCount++;
                        } else {
                            clinicianOtherGenderCount++;
                        }

                        Integer patAge = (visit.patient() != null)
                                ? calculatePatientAge(visit.patient().dateOfBirth() != null ? visit.patient().dateOfBirth().toString() : null, visit.patient().age())
                                : null;

                        if (patAge != null) {
                            clinicianTotalAgeSum += patAge;
                            clinicianAgeCount++;
                            if (patAge < 18) clinicianUnder18Count++;
                            else if (patAge <= 50) clinicianAdults18to50Count++;
                            else clinicianSeniors50plusCount++;
                        }

                        Integer durMins = calculateEncounterDurationMinutes(
                                dept.startedAt() != null ? dept.startedAt().toString() : null,
                                dept.completedAt() != null ? dept.completedAt().toString() : null
                        );
                        if (durMins != null) {
                            clinicianTotalDurationMinutes += durMins;
                            clinicianDurationCount++;
                        }

                        LocalDateTime deptDateTime = parseDateTime(effectiveDeptTime);
                        if (deptDateTime != null) {
                            String hKey = String.format("%02d:00", deptDateTime.getHour());
                            clinicianEncountersByHour.put(hKey, clinicianEncountersByHour.getOrDefault(hKey, 0) + 1);
                        }

                        bumpActivityTimeline(timelineMap, effectiveDeptTime, "clinician", isDailyView);

                        Map<String, Object> cAct = new HashMap<>();
                        cAct.put("id", "consult-" + dept.id());
                        cAct.put("timestamp", effectiveDeptTime);
                        cAct.put("role", "CLINICIAN");
                        cAct.put("actionType", "Consultation Encounter");
                        cAct.put("patientName", patientName);
                        cAct.put("patientId", patientId);
                        cAct.put("patientIdentifier", patientIdentifier);
                        cAct.put("departmentName", deptName);
                        cAct.put("details", "Consultation in " + deptName + " (" + (dept.status() != null ? dept.status().name() : "PENDING") + ")");
                        cAct.put("status", dept.status() != null ? dept.status().name() : "PENDING");
                        clinicianActivities.add(cAct);

                        // Financial accumulation for clinician encounter
                        double encounterGross = 0;
                        double encounterInsCov = 0;
                        double encounterPatPay = 0;
                        double encounterPaid = 0;
                        double encounterLoan = 0;
                        double encounterGiveaway = 0;

                        String resolvedDeptInsuranceName = resolveVisitInsuranceAcronym(dept, visit, deptBilling, hasInsurance);

                        if (deptBilling != null) {
                            Object insListObj = deptBilling.get("insuranceBillings");
                            if (insListObj instanceof List<?> list && !list.isEmpty()) {
                                for (Object itemObj : list) {
                                    if (itemObj instanceof Map<?, ?> ib) {
                                        String insName = resolvedDeptInsuranceName;
                                        Object pIns = ib.get("patientInsurance");
                                        if (pIns instanceof Map<?, ?> pMap) {
                                            Object prov = pMap.get("insuranceProvider");
                                            if (prov instanceof Map<?, ?> provMap) {
                                                String acr = getInsuranceAcronym((String) provMap.get("name"), (String) provMap.get("acronym"));
                                                if (acr != null) insName = acr;
                                            }
                                        }

                                        double tot = toDouble(ib.get("totalAmount"));
                                        double insCov = toDouble(ib.get("insuranceCoveredAmount"));
                                        double patPay = toDouble(ib.get("patientPayableAmount"));
                                        double paid = toDouble(ib.get("paidAmount"));
                                        double outstanding = toDouble(ib.get("outstandingAmount"));
                                        String outstandingType = ib.get("outstandingType") != null ? ib.get("outstandingType").toString() : "";
                                        boolean isLoan = "LOAN".equalsIgnoreCase(outstandingType);
                                        boolean isGiveaway = "GIVEAWAY".equalsIgnoreCase(outstandingType);

                                        double loanVal = isLoan ? outstanding : 0;
                                        double giveVal = isGiveaway ? outstanding : 0;

                                        encounterGross += tot;
                                        encounterInsCov += insCov;
                                        encounterPatPay += patPay;
                                        encounterPaid += paid;
                                        encounterLoan += loanVal;
                                        encounterGiveaway += giveVal;

                                        if (isLoan && loanVal > 0) clinicianLoanCount++;
                                        if (isGiveaway && giveVal > 0) clinicianGiveawayCount++;

                                        accumulateInsuranceSummary(clinicianInsuranceBreakdownMap, insName, tot, insCov, patPay, paid, loanVal, giveVal);
                                    }
                                }
                            } else {
                                double tot = toDouble(deptBilling.get("totalAmount"));
                                double insCov = toDouble(deptBilling.get("insuranceCoveredAmount"));
                                double patPay = toDouble(deptBilling.get("patientPayableAmount"));
                                double paid = toDouble(deptBilling.get("paidAmount"));
                                double outstanding = toDouble(deptBilling.get("outstandingAmount"));

                                encounterGross += tot;
                                encounterInsCov += insCov;
                                encounterPatPay += patPay;
                                encounterPaid += paid;
                                if (outstanding > 0) {
                                    encounterLoan += outstanding;
                                    clinicianLoanCount++;
                                }
                            }
                        } else if (dept.products() != null) {
                            for (VisitDepartmentProductDto p : dept.products()) {
                                String pStatus = p.status() != null ? p.status().name() : "";
                                boolean isProdBilled = "BILLED".equals(pStatus) || "EXEMPTED".equals(pStatus) || "PATIENT_SHARE_EXEMPTED".equals(pStatus);
                                if (!isProdBilled) continue;

                                Map<String, Object> pBillingItem = (p.id() != null) ? billingQueryService.productBillingItem(p.id()) : null;
                                double unitPrice = (pBillingItem != null && pBillingItem.get("unitPriceSnapshot") != null)
                                        ? toDouble(pBillingItem.get("unitPriceSnapshot"))
                                        : (p.product() != null && p.product().clinicPrice() != null ? p.product().clinicPrice().doubleValue() : 0);
                                double qty = p.quantity() != null ? p.quantity().doubleValue() : 1.0;
                                double lineTotal = qty * unitPrice;
                                encounterGross += lineTotal;
                                encounterPaid += lineTotal;
                                if ("EXEMPTED".equals(pStatus) || "PATIENT_SHARE_EXEMPTED".equals(pStatus)) {
                                    encounterGiveaway += lineTotal;
                                    clinicianGiveawayCount++;
                                }
                            }
                        }

                        // Encounter Products mapping
                        List<Map<String, Object>> encounterProducts = new ArrayList<>();
                        if (dept.products() != null) {
                            for (VisitDepartmentProductDto p : dept.products()) {
                                Map<String, Object> pBillingItem = (p.id() != null) ? billingQueryService.productBillingItem(p.id()) : null;
                                double uPrice = (pBillingItem != null && pBillingItem.get("unitPriceSnapshot") != null)
                                        ? toDouble(pBillingItem.get("unitPriceSnapshot"))
                                        : (p.product() != null && p.product().clinicPrice() != null ? p.product().clinicPrice().doubleValue() : 0);
                                double q = p.quantity() != null ? p.quantity().doubleValue() : 1.0;
                                double lTotal = q * uPrice;
                                double insC = (pBillingItem != null) ? toDouble(pBillingItem.get("insuranceCoveredAmount")) : 0;
                                double patP = (pBillingItem != null && pBillingItem.get("patientPayableAmount") != null)
                                        ? toDouble(pBillingItem.get("patientPayableAmount"))
                                        : Math.max(0, lTotal - insC);

                                Map<String, Object> pMap = new HashMap<>();
                                pMap.put("name", p.product() != null && p.product().name() != null ? p.product().name() : "Medical Item");
                                pMap.put("code", p.product() != null ? p.product().code() : null);
                                pMap.put("quantity", q);
                                pMap.put("unitPrice", uPrice);
                                pMap.put("lineTotal", lTotal);
                                pMap.put("insuranceCovered", insC);
                                pMap.put("patientShare", patP);
                                pMap.put("status", p.status() != null ? p.status().name() : "PENDING");
                                pMap.put("type", p.product() != null && p.product().type() != null ? p.product().type().name() : "OTHER");
                                encounterProducts.add(pMap);
                            }
                        }

                        Map<String, Object> encDetail = new HashMap<>();
                        encDetail.put("id", "clinician-enc-" + dept.id());
                        encDetail.put("visitId", visit.id() != null ? visit.id().toString() : "");
                        encDetail.put("visitDepartmentId", dept.id() != null ? dept.id().toString() : "");
                        encDetail.put("timestamp", effectiveDeptTime);
                        encDetail.put("startedAt", dept.startedAt() != null ? dept.startedAt().toString() : null);
                        encDetail.put("completedAt", dept.completedAt() != null ? dept.completedAt().toString() : null);
                        encDetail.put("durationMinutes", durMins);
                        encDetail.put("patientName", patientName);
                        encDetail.put("patientId", patientId);
                        encDetail.put("patientIdentifier", patientIdentifier);
                        encDetail.put("gender", visit.patient() != null && visit.patient().gender() != null ? visit.patient().gender().name() : null);
                        encDetail.put("age", patAge);
                        encDetail.put("dateOfBirth", visit.patient() != null && visit.patient().dateOfBirth() != null ? visit.patient().dateOfBirth().toString() : null);
                        encDetail.put("departmentId", deptId);
                        encDetail.put("departmentName", deptName);
                        encDetail.put("insuranceName", resolvedDeptInsuranceName);
                        encDetail.put("status", dept.status() != null ? dept.status().name() : "PENDING");
                        encDetail.put("products", encounterProducts);
                        encDetail.put("totalGross", encounterGross);
                        encDetail.put("totalInsurance", encounterInsCov);
                        encDetail.put("totalPatient", encounterPatPay);
                        clinicianEncountersList.add(encDetail);

                        clinicianTotalGrossBilled += encounterGross;
                        clinicianInsuranceCoveredAmount += encounterInsCov;
                        clinicianPatientShareAmount += encounterPatPay;
                        clinicianPatientCashCollected += encounterPaid;
                        clinicianPatientLoanAmount += encounterLoan;
                        clinicianGiveawayAmount += encounterGiveaway;

                        bumpMoneyTimeline(clinicianMoneyTimelineMap, effectiveDeptTime, encounterGross, encounterInsCov, encounterPaid, encounterLoan, encounterGiveaway, isDailyView);

                        accumulateDepartmentSummary(clinicianDepartmentBreakdownMap, deptId, deptName, encounterGross, encounterInsCov, encounterPatPay, encounterPaid, encounterLoan, encounterGiveaway);
                    }

                    // Triage encounter for nurses
                    if (isTriage && (isClinicianOnDept || isDeptAddedByWorker) && isWithinDateRange(deptTime, fromDate, toDate)) {
                        triageEncountersCount++;
                        Map<String, Object> tAct = new HashMap<>();
                        tAct.put("id", "triage-" + dept.id());
                        tAct.put("timestamp", deptTime);
                        tAct.put("role", "NURSE");
                        tAct.put("actionType", "Triage Assessment");
                        tAct.put("patientName", patientName);
                        tAct.put("patientId", patientId);
                        tAct.put("patientIdentifier", patientIdentifier);
                        tAct.put("departmentName", "Triage");
                        tAct.put("details", "Assisted triage for " + patientName);
                        tAct.put("status", dept.status() != null ? dept.status().name() : "COMPLETED");
                        nurseActivities.add(tAct);
                    }

                    // Products on department
                    boolean hasWorkerBilledOnDept = false;
                    if (dept.products() != null) {
                        for (VisitDepartmentProductDto prod : dept.products()) {
                            String prodTime = prod.createdAt() != null ? prod.createdAt().toString() : deptTime;
                            String prodName = prod.product() != null && prod.product().name() != null ? prod.product().name() : "Medical Item";
                            String prodType = prod.product() != null && prod.product().type() != null ? prod.product().type().name() : "OTHER";
                            double qty = prod.quantity() != null ? prod.quantity().doubleValue() : 1.0;
                            Map<String, Object> prodBillingItem = (prod.id() != null) ? billingQueryService.productBillingItem(prod.id()) : null;
                            double unitPrice = (prodBillingItem != null && prodBillingItem.get("unitPriceSnapshot") != null)
                                    ? toDouble(prodBillingItem.get("unitPriceSnapshot"))
                                    : (prod.product() != null && prod.product().clinicPrice() != null ? prod.product().clinicPrice().doubleValue() : 0);
                            double lineTotal = qty * unitPrice;
                            boolean isBilledByWorker = prod.billedBy() != null && isUserMatch(prod.billedBy().id(), workerId);
                            boolean isClinicianApproved = (prod.addedBy() != null && isUserMatch(prod.addedBy().id(), workerId)) ||
                                    (prod.processor() != null && isUserMatch(prod.processor().id(), workerId)) ||
                                    (isClinicianOnDept && dept.completedBy() != null && isUserMatch(dept.completedBy().id(), workerId));

                            if (isClinicianApproved && isWithinDateRange(prodTime, fromDate, toDate)) {
                                patientTouchedByWorker = true;
                                prescriptionsCount++;
                                clinicianProductCategoryBreakdown.put(prodType, clinicianProductCategoryBreakdown.getOrDefault(prodType, 0) + (int) Math.round(qty));

                                double insCov = (prodBillingItem != null) ? toDouble(prodBillingItem.get("insuranceCoveredAmount")) : 0;
                                double patPay = (prodBillingItem != null && prodBillingItem.get("patientPayableAmount") != null)
                                        ? toDouble(prodBillingItem.get("patientPayableAmount"))
                                        : Math.max(0, lineTotal - insCov);
                                String pStatus = prod.status() != null ? prod.status().name() : "PENDING";
                                boolean isBilled = "BILLED".equals(pStatus);
                                boolean isExempted = "EXEMPTED".equals(pStatus) || "PATIENT_SHARE_EXEMPTED".equals(pStatus);
                                boolean isBilledOrExempted = isBilled || isExempted;

                                if (isBilledOrExempted) {
                                    clinicianProductTurnoverTotal += lineTotal;
                                    clinicianProductItemsApprovedCount += qty;
                                    if (isExempted && !isClinicianOnDept) {
                                        clinicianGiveawayAmount += lineTotal;
                                        clinicianGiveawayCount += (int) Math.round(qty);
                                    }
                                } else {
                                    clinicianPendingBillingAmount += lineTotal;
                                    clinicianPendingBillingCount += (int) Math.round(qty);
                                }

                                if (!clinicianProductTurnoverByCategory.containsKey(prodType)) {
                                    Map<String, Object> catMap = new HashMap<>();
                                    catMap.put("category", prodType);
                                    catMap.put("count", 0.0);
                                    catMap.put("totalRevenue", 0.0);
                                    catMap.put("insuranceCovered", 0.0);
                                    catMap.put("patientShare", 0.0);
                                    clinicianProductTurnoverByCategory.put(prodType, catMap);
                                }
                                Map<String, Object> catEntry = clinicianProductTurnoverByCategory.get(prodType);
                                catEntry.put("count", toDouble(catEntry.get("count")) + qty);
                                if (isBilledOrExempted) {
                                    catEntry.put("totalRevenue", toDouble(catEntry.get("totalRevenue")) + lineTotal);
                                    catEntry.put("insuranceCovered", toDouble(catEntry.get("insuranceCovered")) + insCov);
                                    catEntry.put("patientShare", toDouble(catEntry.get("patientShare")) + patPay);
                                }

                                String deptInsuranceName = resolveVisitInsuranceAcronym(dept, visit, deptBilling, hasInsurance);

                                Map<String, Object> toItem = new HashMap<>();
                                toItem.put("id", "prod-turnover-" + prod.id());
                                toItem.put("productId", prod.product() != null && prod.product().id() != null ? prod.product().id().toString() : "");
                                toItem.put("productName", prodName);
                                toItem.put("productCode", prod.product() != null ? prod.product().code() : null);
                                toItem.put("productType", prodType);
                                toItem.put("quantity", qty);
                                toItem.put("unitPrice", unitPrice);
                                toItem.put("lineTotal", lineTotal);
                                toItem.put("insuranceCoveredAmount", insCov);
                                toItem.put("patientPayableAmount", patPay);
                                toItem.put("status", pStatus);
                                toItem.put("patientName", patientName);
                                toItem.put("patientId", patientId);
                                toItem.put("patientIdentifier", patientIdentifier);
                                toItem.put("departmentId", deptId);
                                toItem.put("departmentName", deptName);
                                toItem.put("timestamp", prodTime);
                                toItem.put("isBilled", isBilled);
                                toItem.put("isExempted", isExempted);
                                toItem.put("insuranceName", deptInsuranceName);
                                toItem.put("visitId", visit.id() != null ? visit.id().toString() : "");
                                toItem.put("visitDepartmentId", dept.id() != null ? dept.id().toString() : "");
                                clinicianProductTurnoverList.add(toItem);

                                boolean isNursingAct = "MEDICAL_ACT".equals(prodType) || "CONSUMABLE_DEVICE".equals(prodType);
                                if (isNursingAct && prod.addedBy() != null && isUserMatch(prod.addedBy().id(), workerId)) {
                                    nursingActsCount += (int) Math.round(qty);
                                    bumpActivityTimeline(timelineMap, prodTime, "nurse", isDailyView);

                                    Map<String, Object> nAct = new HashMap<>();
                                    nAct.put("id", "nursing-act-" + prod.id());
                                    nAct.put("timestamp", prodTime);
                                    nAct.put("role", "NURSE");
                                    nAct.put("actionType", "Nursing Act / Consumable");
                                    nAct.put("patientName", patientName);
                                    nAct.put("patientId", patientId);
                                    nAct.put("patientIdentifier", patientIdentifier);
                                    nAct.put("departmentName", deptName);
                                    nAct.put("details", prodName + " (Qty: " + qty + ")");
                                    nAct.put("amount", lineTotal);
                                    nAct.put("status", pStatus);
                                    nurseActivities.add(nAct);
                                } else if (prod.addedBy() != null && isUserMatch(prod.addedBy().id(), workerId)) {
                                    bumpActivityTimeline(timelineMap, prodTime, "clinician", isDailyView);

                                    Map<String, Object> pAct = new HashMap<>();
                                    pAct.put("id", "prod-add-" + prod.id());
                                    pAct.put("timestamp", prodTime);
                                    pAct.put("role", "CLINICIAN");
                                    pAct.put("actionType", "Product / Prescription Ordered");
                                    pAct.put("patientName", patientName);
                                    pAct.put("patientId", patientId);
                                    pAct.put("patientIdentifier", patientIdentifier);
                                    pAct.put("departmentName", deptName);
                                    pAct.put("details", "Prescribed " + prodName + " (Qty: " + qty + ")");
                                    pAct.put("amount", lineTotal);
                                    pAct.put("status", pStatus);
                                    clinicianActivities.add(pAct);
                                }
                            }

                            // 2. Billed by cashier / finance worker
                            if (isBilledByWorker && isWithinDateRange(prodTime, fromDate, toDate)) {
                                billedItemsCount++;
                                hasWorkerBilledOnDept = true;
                                patientTouchedByWorker = true;

                                if (!financeCategoryBreakdown.containsKey(prodType)) {
                                    Map<String, Object> cMap = new HashMap<>();
                                    cMap.put("count", 0.0);
                                    cMap.put("totalAmount", 0.0);
                                    financeCategoryBreakdown.put(prodType, cMap);
                                }
                                Map<String, Object> fCat = financeCategoryBreakdown.get(prodType);
                                fCat.put("count", toDouble(fCat.get("count")) + qty);
                                fCat.put("totalAmount", toDouble(fCat.get("totalAmount")) + lineTotal);

                                String pStatus = prod.status() != null ? prod.status().name() : "BILLED";
                                if ("EXEMPTED".equals(pStatus) || "PATIENT_SHARE_EXEMPTED".equals(pStatus)) {
                                    giveawayAmount += lineTotal;
                                    giveawayCount++;
                                }

                                bumpActivityTimeline(timelineMap, prodTime, "finance", isDailyView);

                                Map<String, Object> fAct = new HashMap<>();
                                fAct.put("id", "bill-" + prod.id());
                                fAct.put("timestamp", prodTime);
                                fAct.put("role", "FINANCE");
                                fAct.put("actionType", "Billed Line Item");
                                fAct.put("patientName", patientName);
                                fAct.put("patientId", patientId);
                                fAct.put("patientIdentifier", patientIdentifier);
                                fAct.put("departmentName", deptName);
                                fAct.put("details", "Billed: " + prodName + " (Qty: " + qty + ") [" + pStatus + "]");
                                fAct.put("amount", lineTotal);
                                fAct.put("status", pStatus);
                                financeActivities.add(fAct);
                            }
                        }
                    }

                    // Department Billing & Payments for Finance
                    if (deptBilling != null) {
                        String bTime = deptTime;
                        boolean isBilledInPeriod = isWithinDateRange(bTime, fromDate, toDate);

                        if (hasWorkerBilledOnDept && isBilledInPeriod) {
                            if (dept.id() != null) billedDepartmentsSet.add(dept.id().toString());

                            Object paymentsObj = deptBilling.get("payments");
                            if (paymentsObj instanceof List<?> pList) {
                                for (Object payObj : pList) {
                                    if (payObj instanceof Map<?, ?> pay) {
                                        double payAmt = toDouble(pay.get("amount"));
                                        if (payAmt <= 0) continue;
                                        String rawMethod = pay.get("paymentMethod") != null ? pay.get("paymentMethod").toString().toUpperCase() : "CASH";
                                        String methodKey = "CASH";
                                        if (rawMethod.contains("MOBILE") || rawMethod.contains("MOMO") || rawMethod.contains("MTN") || rawMethod.contains("AIRTEL")) {
                                            methodKey = "MOBILE_MONEY";
                                            momoCollected += payAmt;
                                        } else if (rawMethod.contains("CARD") || rawMethod.contains("POS") || rawMethod.contains("VISA") || rawMethod.contains("MASTERCARD")) {
                                            methodKey = "CARD";
                                            cardCollected += payAmt;
                                        } else if (rawMethod.contains("BANK") || rawMethod.contains("TRANSFER")) {
                                            methodKey = "BANK_TRANSFER";
                                            bankTransferCollected += payAmt;
                                        } else if (rawMethod.contains("CASH")) {
                                            methodKey = "CASH";
                                            cashCollected += payAmt;
                                        } else {
                                            methodKey = rawMethod;
                                            otherCollected += payAmt;
                                        }

                                        if (!paymentModesBreakdownMap.containsKey(methodKey)) {
                                            Map<String, Object> pModeMap = new HashMap<>();
                                            pModeMap.put("amount", 0.0);
                                            pModeMap.put("count", 0);
                                            paymentModesBreakdownMap.put(methodKey, pModeMap);
                                        }
                                        Map<String, Object> pmEntry = paymentModesBreakdownMap.get(methodKey);
                                        pmEntry.put("amount", toDouble(pmEntry.get("amount")) + payAmt);
                                        pmEntry.put("count", (int) pmEntry.get("count") + 1);
                                    }
                                }
                            }

                            Object insListObj = deptBilling.get("insuranceBillings");
                            if (insListObj instanceof List<?> list && !list.isEmpty()) {
                                for (Object itemObj : list) {
                                    if (itemObj instanceof Map<?, ?> ib) {
                                        String insName = resolveVisitInsuranceAcronym(dept, visit, deptBilling, hasInsurance);
                                        Object pIns = ib.get("patientInsurance");
                                        if (pIns instanceof Map<?, ?> pMap) {
                                            Object prov = pMap.get("insuranceProvider");
                                            if (prov instanceof Map<?, ?> provMap) {
                                                String acr = getInsuranceAcronym((String) provMap.get("name"), (String) provMap.get("acronym"));
                                                if (acr != null) insName = acr;
                                            }
                                        }

                                        double tot = toDouble(ib.get("totalAmount"));
                                        double insCov = toDouble(ib.get("insuranceCoveredAmount"));
                                        double patPay = toDouble(ib.get("patientPayableAmount"));
                                        double paid = toDouble(ib.get("paidAmount"));
                                        double outstanding = toDouble(ib.get("outstandingAmount"));
                                        String outstandingType = ib.get("outstandingType") != null ? ib.get("outstandingType").toString() : "";
                                        boolean isLoan = "LOAN".equalsIgnoreCase(outstandingType);
                                        boolean isGiveaway = "GIVEAWAY".equalsIgnoreCase(outstandingType);

                                        double loanVal = isLoan ? outstanding : 0;
                                        double giveVal = isGiveaway ? outstanding : 0;

                                        totalGrossBilled += tot;
                                        insuranceCoveredAmount += insCov;
                                        patientShareAmount += patPay;
                                        patientCashCollected += paid;
                                        patientLoanAmount += loanVal;
                                        if (isGiveaway) {
                                            giveawayAmount += giveVal;
                                            giveawayCount++;
                                        }
                                        if (isLoan && loanVal > 0) {
                                            loanCount++;
                                        }

                                        bumpMoneyTimeline(moneyTimelineMap, bTime, tot, insCov, paid, loanVal, giveVal, isDailyView);
                                        accumulateInsuranceSummary(insuranceBreakdownMap, insName, tot, insCov, patPay, paid, loanVal, giveVal);
                                    }
                                }
                            } else {
                                double tot = toDouble(deptBilling.get("totalAmount"));
                                double insCov = toDouble(deptBilling.get("insuranceCoveredAmount"));
                                double patPay = toDouble(deptBilling.get("patientPayableAmount"));
                                double paid = toDouble(deptBilling.get("paidAmount"));
                                double outstanding = toDouble(deptBilling.get("outstandingAmount"));

                                totalGrossBilled += tot;
                                insuranceCoveredAmount += insCov;
                                patientShareAmount += patPay;
                                patientCashCollected += paid;
                                if (outstanding > 0) {
                                    patientLoanAmount += outstanding;
                                    loanCount++;
                                }
                                bumpMoneyTimeline(moneyTimelineMap, bTime, tot, insCov, paid, outstanding, 0, isDailyView);
                            }
                        }
                    }
                }
            }

            if (patientTouchedByWorker && !patientId.isEmpty()) {
                uniquePatientsSet.add(patientId);
                if (!overallPatientsSet.contains(patientId)) {
                    overallPatientsSet.add(patientId);
                    String gen = visit.patient() != null && visit.patient().gender() != null ? visit.patient().gender().name() : "";
                    if (gen.equalsIgnoreCase("MALE") || gen.startsWith("M")) {
                        overallMaleCount++;
                    } else if (gen.equalsIgnoreCase("FEMALE") || gen.startsWith("F")) {
                        overallFemaleCount++;
                    } else {
                        overallOtherGenderCount++;
                    }

                    Integer pAge = (visit.patient() != null)
                            ? calculatePatientAge(visit.patient().dateOfBirth() != null ? visit.patient().dateOfBirth().toString() : null, visit.patient().age())
                            : null;
                    if (pAge != null) {
                        overallTotalAgeSum += pAge;
                        overallAgeCount++;
                        if (pAge < 18) overallUnder18Count++;
                        else if (pAge <= 50) overallAdults18to50Count++;
                        else overallSeniors50plusCount++;
                    }
                }
            }
        }

        // Sort activities by timestamp descending
        Comparator<Map<String, Object>> timeDesc = (a, b) -> {
            String ta = (String) a.get("timestamp");
            String tb = (String) b.get("timestamp");
            if (ta == null && tb == null) return 0;
            if (ta == null) return 1;
            if (tb == null) return -1;
            return tb.compareTo(ta);
        };

        receptionActivities.sort(timeDesc);
        clinicianActivities.sort(timeDesc);
        nurseActivities.sort(timeDesc);
        financeActivities.sort(timeDesc);
        clinicianProductTurnoverList.sort(timeDesc);
        clinicianEncountersList.sort(timeDesc);

        List<Map<String, Object>> allActivities = new ArrayList<>();
        if (hasReception) allActivities.addAll(receptionActivities);
        if (hasClinician) allActivities.addAll(clinicianActivities);
        if (hasNurse) allActivities.addAll(nurseActivities);
        if (hasFinance) allActivities.addAll(financeActivities);
        allActivities.sort(timeDesc);

        if (!canViewFinance) {
            allActivities.removeIf(activity -> "FINANCE".equals(activity.get("role")));
            allActivities.forEach(activity -> activity.remove("amount"));
            receptionActivities.forEach(activity -> activity.remove("amount"));
            clinicianActivities.forEach(activity -> activity.remove("amount"));
            nurseActivities.forEach(activity -> activity.remove("amount"));
            clinicianEncountersList.clear();
            timelineMap.values().forEach(bucket -> {
                bucket.put("finance", 0);
                bucket.put("total",
                        ((Number) bucket.get("reception")).intValue()
                                + ((Number) bucket.get("clinician")).intValue()
                                + ((Number) bucket.get("nurse")).intValue());
            });
        }

        // Peak hour
        String peakHourStr = "N/A";
        int maxHourCount = 0;
        for (Map.Entry<String, Integer> entry : clinicianEncountersByHour.entrySet()) {
            if (entry.getValue() > maxHourCount) {
                maxHourCount = entry.getValue();
                int hNum = Integer.parseInt(entry.getKey().split(":")[0]);
                peakHourStr = formatHourRange(hNum);
            }
        }

        // Demographics
        Map<String, Object> ageBrackets = new HashMap<>();
        ageBrackets.put("under18", clinicianUnder18Count);
        ageBrackets.put("adults18to50", clinicianAdults18to50Count);
        ageBrackets.put("seniors50plus", clinicianSeniors50plusCount);

        Map<String, Object> clinicianDemographics = new HashMap<>();
        clinicianDemographics.put("totalPatientsCount", clinicianPatientsSet.isEmpty() ? consultationsCount : clinicianPatientsSet.size());
        clinicianDemographics.put("maleCount", clinicianMaleCount);
        clinicianDemographics.put("femaleCount", clinicianFemaleCount);
        clinicianDemographics.put("otherGenderCount", clinicianOtherGenderCount);
        clinicianDemographics.put("averageAge", clinicianAgeCount > 0 ? Math.round((clinicianTotalAgeSum / clinicianAgeCount) * 10.0) / 10.0 : 0.0);
        clinicianDemographics.put("ageBrackets", ageBrackets);
        clinicianDemographics.put("averageEncounterTimeMinutes", clinicianDurationCount > 0 ? Math.round((double) clinicianTotalDurationMinutes / clinicianDurationCount) : 0);
        clinicianDemographics.put("peakActivityHour", peakHourStr);
        clinicianDemographics.put("encountersByHour", clinicianEncountersByHour);

        Map<String, Object> overallAgeBrackets = new HashMap<>();
        overallAgeBrackets.put("under18", overallUnder18Count);
        overallAgeBrackets.put("adults18to50", overallAdults18to50Count);
        overallAgeBrackets.put("seniors50plus", overallSeniors50plusCount);

        Map<String, Object> overallDemographics = new HashMap<>();
        if (!clinicianPatientsSet.isEmpty() || consultationsCount > 0) {
            overallDemographics = clinicianDemographics;
        } else {
            overallDemographics.put("totalPatientsCount", overallPatientsSet.size() > 0 ? overallPatientsSet.size() : uniquePatientsSet.size());
            overallDemographics.put("maleCount", overallMaleCount);
            overallDemographics.put("femaleCount", overallFemaleCount);
            overallDemographics.put("otherGenderCount", overallOtherGenderCount);
            overallDemographics.put("averageAge", overallAgeCount > 0 ? Math.round((overallTotalAgeSum / overallAgeCount) * 10.0) / 10.0 : 0.0);
            overallDemographics.put("ageBrackets", overallAgeBrackets);
            overallDemographics.put("averageEncounterTimeMinutes", clinicianDurationCount > 0 ? Math.round((double) clinicianTotalDurationMinutes / clinicianDurationCount) : 0);
            overallDemographics.put("peakActivityHour", peakHourStr);
            overallDemographics.put("encountersByHour", clinicianEncountersByHour);
        }

        // Timelines formatted
        List<Map<String, Object>> timeline = formatActivityTimeline(timelineMap, isDailyView, isWeeklyView);
        List<Map<String, Object>> moneyTimeline = formatMoneyTimeline(moneyTimelineMap, isDailyView, isWeeklyView);
        List<Map<String, Object>> clinicianMoneyTimeline = formatMoneyTimeline(clinicianMoneyTimelineMap, isDailyView, isWeeklyView);

        // Insurance and Department breakdown lists
        List<Map<String, Object>> insuranceBreakdown = new ArrayList<>(insuranceBreakdownMap.values());
        insuranceBreakdown.sort((a, b) -> Double.compare(toDouble(b.get("totalAmount")), toDouble(a.get("totalAmount"))));

        List<Map<String, Object>> clinicianInsuranceBreakdown = new ArrayList<>(clinicianInsuranceBreakdownMap.values());
        clinicianInsuranceBreakdown.sort((a, b) -> Double.compare(toDouble(b.get("totalAmount")), toDouble(a.get("totalAmount"))));

        List<Map<String, Object>> clinicianDepartmentBreakdown = new ArrayList<>(clinicianDepartmentBreakdownMap.values());
        clinicianDepartmentBreakdown.sort((a, b) -> Double.compare(toDouble(b.get("totalAmount")), toDouble(a.get("totalAmount"))));

        // Settlement rate
        double totalSettled = insuranceCoveredAmount + patientCashCollected + giveawayAmount;
        int settlementRatePct = totalGrossBilled > 0 ? (int) Math.min(100, Math.round((totalSettled / totalGrossBilled) * 100)) : 100;
        double outstandingBalance = Math.max(0, totalGrossBilled - insuranceCoveredAmount - patientCashCollected - giveawayAmount);

        // Reconcile payment modes with patient cash collected
        double totalPaymentModesCollected = momoCollected + cashCollected + cardCollected + bankTransferCollected + otherCollected;
        if (patientCashCollected > totalPaymentModesCollected) {
            double diff = patientCashCollected - totalPaymentModesCollected;
            cashCollected += diff;
            if (!paymentModesBreakdownMap.containsKey("CASH")) {
                Map<String, Object> cMap = new HashMap<>();
                cMap.put("amount", 0.0);
                cMap.put("count", 0);
                paymentModesBreakdownMap.put("CASH", cMap);
            }
            Map<String, Object> cEntry = paymentModesBreakdownMap.get("CASH");
            cEntry.put("amount", toDouble(cEntry.get("amount")) + diff);
            cEntry.put("count", (int) cEntry.get("count") + 1);
        }

        Map<String, Object> financeMoneyReport = new HashMap<>();
        financeMoneyReport.put("totalGrossBilled", totalGrossBilled);
        financeMoneyReport.put("insuranceCoveredAmount", insuranceCoveredAmount);
        financeMoneyReport.put("patientShareAmount", patientShareAmount);
        financeMoneyReport.put("patientCashCollected", patientCashCollected);
        financeMoneyReport.put("patientLoanAmount", patientLoanAmount);
        financeMoneyReport.put("giveawayAmount", giveawayAmount);
        financeMoneyReport.put("outstandingBalance", outstandingBalance);
        financeMoneyReport.put("loanCount", loanCount);
        financeMoneyReport.put("giveawayCount", giveawayCount);
        financeMoneyReport.put("settlementRatePct", settlementRatePct);
        financeMoneyReport.put("momoCollected", momoCollected);
        financeMoneyReport.put("cashCollected", cashCollected);
        financeMoneyReport.put("cardCollected", cardCollected);
        financeMoneyReport.put("bankTransferCollected", bankTransferCollected);
        financeMoneyReport.put("otherCollected", otherCollected);
        financeMoneyReport.put("paymentModesBreakdown", paymentModesBreakdownMap);
        financeMoneyReport.put("moneyTimeline", moneyTimeline);
        financeMoneyReport.put("insuranceBreakdown", insuranceBreakdown);

        Map<String, Object> clinicianMoneyReport = new HashMap<>();
        double clinicianGross = clinicianTotalGrossBilled > 0 ? clinicianTotalGrossBilled : clinicianProductTurnoverTotal;
        clinicianMoneyReport.put("totalGrossBilled", clinicianGross);
        clinicianMoneyReport.put("insuranceCoveredAmount", clinicianInsuranceCoveredAmount);
        clinicianMoneyReport.put("patientShareAmount", clinicianPatientShareAmount);
        clinicianMoneyReport.put("patientCashCollected", clinicianPatientCashCollected);
        clinicianMoneyReport.put("patientLoanAmount", clinicianPatientLoanAmount);
        clinicianMoneyReport.put("giveawayAmount", clinicianGiveawayAmount);
        clinicianMoneyReport.put("outstandingBalance", Math.max(0, clinicianGross - clinicianInsuranceCoveredAmount - clinicianPatientCashCollected - clinicianGiveawayAmount));
        clinicianMoneyReport.put("loanCount", clinicianLoanCount);
        clinicianMoneyReport.put("giveawayCount", clinicianGiveawayCount);
        clinicianMoneyReport.put("pendingBillingAmount", clinicianPendingBillingAmount);
        clinicianMoneyReport.put("pendingBillingCount", clinicianPendingBillingCount);
        clinicianMoneyReport.put("totalProductTurnover", clinicianProductTurnoverTotal);
        clinicianMoneyReport.put("productItemsApprovedCount", clinicianProductItemsApprovedCount);
        clinicianMoneyReport.put("productTurnoverByCategory", clinicianProductTurnoverByCategory);
        clinicianMoneyReport.put("productTurnoverList", clinicianProductTurnoverList);
        clinicianMoneyReport.put("moneyTimeline", clinicianMoneyTimeline);
        clinicianMoneyReport.put("insuranceBreakdown", clinicianInsuranceBreakdown);
        clinicianMoneyReport.put("departmentBreakdown", clinicianDepartmentBreakdown);

        Map<String, Object> financeGeneralReport = new HashMap<>();
        financeGeneralReport.put("billedItemsCount", billedItemsCount);
        financeGeneralReport.put("departmentsBilledCount", billedDepartmentsSet.size());
        financeGeneralReport.put("categoryBreakdown", financeCategoryBreakdown);
        financeGeneralReport.put("activities", financeActivities);

        Map<String, Object> receptionReport = new HashMap<>();
        receptionReport.put("visitsInitiatedCount", visitsInitiatedCount);
        receptionReport.put("departmentsDispatchedCount", departmentsDispatchedCount);
        receptionReport.put("uniquePatientsCount", receptionPatientsSet.size());
        receptionReport.put("insuredVisitsCount", insuredVisitsCount);
        receptionReport.put("privateVisitsCount", privateVisitsCount);
        receptionReport.put("activities", receptionActivities);

        Map<String, Object> clinicianReport = new HashMap<>();
        clinicianReport.put("consultationsCount", consultationsCount);
        clinicianReport.put("consultationsCompletedCount", consultationsCompletedCount);
        clinicianReport.put("consultationsInProgressCount", consultationsInProgressCount);
        clinicianReport.put("prescriptionsCount", prescriptionsCount);
        clinicianReport.put("referralsCount", referralsCount);
        clinicianReport.put("productCategoryBreakdown", clinicianProductCategoryBreakdown);
        clinicianReport.put("money", canViewFinance ? clinicianMoneyReport : null);
        clinicianReport.put("demographics", clinicianDemographics);
        clinicianReport.put("encountersList", clinicianEncountersList);
        clinicianReport.put("activities", clinicianActivities);

        Map<String, Object> nurseReport = new HashMap<>();
        nurseReport.put("vitalsRecordedCount", vitalsRecordedCount);
        nurseReport.put("triageEncountersCount", triageEncountersCount);
        nurseReport.put("nursingActsCount", nursingActsCount);
        nurseReport.put("activities", nurseActivities);

        Map<String, Object> financeReport = new HashMap<>();
        financeReport.put("general", financeGeneralReport);
        financeReport.put("money", financeMoneyReport);
        financeReport.put("totalRevenueBilled", totalGrossBilled > 0 ? totalGrossBilled : patientCashCollected);
        financeReport.put("billedItemsCount", billedItemsCount);
        financeReport.put("departmentsBilledCount", billedDepartmentsSet.size());
        financeReport.put("activities", financeActivities);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("workerId", workerId);
        result.put("workerName", workerName);
        result.put("roles", new ArrayList<>(workerRoleNames));
        result.put("period", period);
        result.put("fromDate", fromDate);
        result.put("toDate", toDate);
        result.put("hasReception", hasReception);
        result.put("hasClinician", hasClinician);
        result.put("hasNurse", hasNurse);
        result.put("hasFinance", hasFinance);
        result.put("canViewFinance", canViewFinance);
        result.put("isAdminOrManager", isAdminOrManager);
        result.put("allowedTabs", allowedTabs);
        result.put("totalInteractions", allActivities.size());
        result.put("uniquePatientsTouched", uniquePatientsSet.size());
        result.put("demographics", overallDemographics);
        result.put("reception", receptionReport);
        result.put("clinician", clinicianReport);
        result.put("nurse", nurseReport);
        result.put("finance", canViewFinance ? financeReport : null);
        result.put("timeline", timeline);
        result.put("allActivities", allActivities);

        return result;
    }

    private Map<String, Integer> createActivityTimelineBucket() {
        Map<String, Integer> m = new HashMap<>();
        m.put("reception", 0);
        m.put("clinician", 0);
        m.put("nurse", 0);
        m.put("finance", 0);
        m.put("total", 0);
        return m;
    }

    private Map<String, Double> createMoneyTimelineBucket() {
        Map<String, Double> m = new HashMap<>();
        m.put("gross", 0.0);
        m.put("insurance", 0.0);
        m.put("patientCash", 0.0);
        m.put("loan", 0.0);
        m.put("giveaway", 0.0);
        return m;
    }

    private String getBucketKey(String timestamp, boolean isDailyView) {
        if (isDailyView) {
            LocalDateTime dt = parseDateTime(timestamp);
            if (dt != null) {
                return String.format("%02d:00", dt.getHour());
            }
            return "08:00";
        }
        return toDateKey(timestamp);
    }

    private void bumpActivityTimeline(Map<String, Map<String, Integer>> timelineMap, String timestamp, String type, boolean isDailyView) {
        String key = getBucketKey(timestamp, isDailyView);
        if (!timelineMap.containsKey(key)) {
            timelineMap.put(key, createActivityTimelineBucket());
        }
        Map<String, Integer> bucket = timelineMap.get(key);
        bucket.put(type, bucket.getOrDefault(type, 0) + 1);
        bucket.put("total", bucket.getOrDefault("total", 0) + 1);
    }

    private void bumpMoneyTimeline(Map<String, Map<String, Double>> moneyTimelineMap, String timestamp, double gross, double ins, double cash, double loan, double give, boolean isDailyView) {
        String key = getBucketKey(timestamp, isDailyView);
        if (!moneyTimelineMap.containsKey(key)) {
            moneyTimelineMap.put(key, createMoneyTimelineBucket());
        }
        Map<String, Double> bucket = moneyTimelineMap.get(key);
        bucket.put("gross", bucket.getOrDefault("gross", 0.0) + gross);
        bucket.put("insurance", bucket.getOrDefault("insurance", 0.0) + ins);
        bucket.put("patientCash", bucket.getOrDefault("patientCash", 0.0) + cash);
        bucket.put("loan", bucket.getOrDefault("loan", 0.0) + loan);
        bucket.put("giveaway", bucket.getOrDefault("giveaway", 0.0) + give);
    }

    private void accumulateInsuranceSummary(Map<String, Map<String, Object>> map, String insName, double tot, double insCov, double patPay, double paid, double loanVal, double giveVal) {
        if (!map.containsKey(insName)) {
            Map<String, Object> entry = new HashMap<>();
            entry.put("insuranceName", insName);
            entry.put("totalAmount", 0.0);
            entry.put("insuranceCovered", 0.0);
            entry.put("patientShare", 0.0);
            entry.put("paidAmount", 0.0);
            entry.put("loanAmount", 0.0);
            entry.put("giveawayAmount", 0.0);
            entry.put("count", 0);
            map.put(insName, entry);
        }
        Map<String, Object> entry = map.get(insName);
        entry.put("totalAmount", toDouble(entry.get("totalAmount")) + tot);
        entry.put("insuranceCovered", toDouble(entry.get("insuranceCovered")) + insCov);
        entry.put("patientShare", toDouble(entry.get("patientShare")) + patPay);
        entry.put("paidAmount", toDouble(entry.get("paidAmount")) + paid);
        entry.put("loanAmount", toDouble(entry.get("loanAmount")) + loanVal);
        entry.put("giveawayAmount", toDouble(entry.get("giveawayAmount")) + giveVal);
        entry.put("count", (int) entry.get("count") + 1);
    }

    private void accumulateDepartmentSummary(Map<String, Map<String, Object>> map, String deptId, String deptName, double tot, double insCov, double patPay, double paid, double loanVal, double giveVal) {
        if (!map.containsKey(deptName)) {
            Map<String, Object> entry = new HashMap<>();
            entry.put("departmentId", deptId);
            entry.put("departmentName", deptName);
            entry.put("totalAmount", 0.0);
            entry.put("insuranceCovered", 0.0);
            entry.put("patientShare", 0.0);
            entry.put("paidAmount", 0.0);
            entry.put("loanAmount", 0.0);
            entry.put("giveawayAmount", 0.0);
            entry.put("encountersCount", 0);
            map.put(deptName, entry);
        }
        Map<String, Object> entry = map.get(deptName);
        entry.put("totalAmount", toDouble(entry.get("totalAmount")) + tot);
        entry.put("insuranceCovered", toDouble(entry.get("insuranceCovered")) + insCov);
        entry.put("patientShare", toDouble(entry.get("patientShare")) + patPay);
        entry.put("paidAmount", toDouble(entry.get("paidAmount")) + paid);
        entry.put("loanAmount", toDouble(entry.get("loanAmount")) + loanVal);
        entry.put("giveawayAmount", toDouble(entry.get("giveawayAmount")) + giveVal);
        entry.put("encountersCount", (int) entry.get("encountersCount") + 1);
    }

    private List<Map<String, Object>> formatActivityTimeline(Map<String, Map<String, Integer>> map, boolean isDailyView, boolean isWeeklyView) {
        List<String> sortedKeys = new ArrayList<>(map.keySet());
        Collections.sort(sortedKeys);
        List<Map<String, Object>> list = new ArrayList<>();
        for (String key : sortedKeys) {
            Map<String, Integer> bucket = map.get(key);
            Map<String, Object> item = new HashMap<>();
            item.put("date", key);
            item.put("label", formatTimelineLabel(key, isDailyView, isWeeklyView));
            item.put("reception", bucket.getOrDefault("reception", 0));
            item.put("clinician", bucket.getOrDefault("clinician", 0));
            item.put("nurse", bucket.getOrDefault("nurse", 0));
            item.put("finance", bucket.getOrDefault("finance", 0));
            item.put("total", bucket.getOrDefault("total", 0));
            list.add(item);
        }
        return list;
    }

    private List<Map<String, Object>> formatMoneyTimeline(Map<String, Map<String, Double>> map, boolean isDailyView, boolean isWeeklyView) {
        List<String> sortedKeys = new ArrayList<>(map.keySet());
        Collections.sort(sortedKeys);
        List<Map<String, Object>> list = new ArrayList<>();
        for (String key : sortedKeys) {
            Map<String, Double> bucket = map.get(key);
            Map<String, Object> item = new HashMap<>();
            item.put("date", key);
            item.put("label", formatTimelineLabel(key, isDailyView, isWeeklyView));
            item.put("gross", bucket.getOrDefault("gross", 0.0));
            item.put("insurance", bucket.getOrDefault("insurance", 0.0));
            item.put("patientCash", bucket.getOrDefault("patientCash", 0.0));
            item.put("loan", bucket.getOrDefault("loan", 0.0));
            item.put("giveaway", bucket.getOrDefault("giveaway", 0.0));
            list.add(item);
        }
        return list;
    }

    private String formatTimelineLabel(String key, boolean isDailyView, boolean isWeeklyView) {
        if (isDailyView) {
            return key;
        }
        try {
            LocalDate d = parseLocalDate(key);
            if (isWeeklyView) {
                return d.format(DateTimeFormatter.ofPattern("EEE, MMM d"));
            }
            return d.format(DateTimeFormatter.ofPattern("MMM d"));
        } catch (Exception e) {
            return key;
        }
    }

    private double toDouble(Object obj) {
        if (obj == null) return 0.0;
        if (obj instanceof Number num) return num.doubleValue();
        try {
            return Double.parseDouble(obj.toString());
        } catch (Exception e) {
            return 0.0;
        }
    }

    @Transactional(readOnly = true)
    public ApiResponse getClinicReports(
            String fromDate,
            String toDate,
            String periodInput,
            UUID departmentId,
            UUID clinicianId,
            UUID insuranceId,
            Boolean isPrivateOnly,
            AuthenticatedUser authUser
    ) {
        return getClinicReports(fromDate, toDate, periodInput, departmentId, null, clinicianId, null, insuranceId, isPrivateOnly, authUser);
    }

    @Transactional(readOnly = true)
    public ApiResponse getClinicReports(
            String fromDate,
            String toDate,
            String periodInput,
            UUID departmentId,
            UUID clinicianId,
            List<UUID> clinicianIds,
            UUID insuranceId,
            Boolean isPrivateOnly,
            AuthenticatedUser authUser
    ) {
        return getClinicReports(fromDate, toDate, periodInput, departmentId, null, clinicianId, clinicianIds, insuranceId, isPrivateOnly, authUser);
    }

    @Transactional(readOnly = true)
    public ApiResponse getClinicReports(
            String fromDate,
            String toDate,
            String periodInput,
            UUID departmentId,
            List<UUID> departmentIds,
            UUID clinicianId,
            List<UUID> clinicianIds,
            UUID insuranceId,
            Boolean isPrivateOnly,
            AuthenticatedUser authUser
    ) {
        return getClinicReports(fromDate, toDate, periodInput, departmentId, departmentIds, clinicianId, clinicianIds, insuranceId, null, isPrivateOnly, authUser);
    }

    @Transactional(readOnly = true)
    public ApiResponse getClinicReports(
            String fromDate,
            String toDate,
            String periodInput,
            UUID departmentId,
            List<UUID> departmentIds,
            UUID clinicianId,
            List<UUID> clinicianIds,
            UUID insuranceId,
            List<UUID> insuranceIds,
            Boolean isPrivateOnly,
            AuthenticatedUser authUser
    ) {
        if (authUser == null || authUser.userId() == null) {
            return ApiResponse.unauthenticated("Authentication is required to view reports.");
        }

        boolean canAccessClinicReports = authUser.roles() != null && authUser.roles().stream().anyMatch(r ->
                r == RoleName.ADMIN || r == RoleName.MANAGER || r == RoleName.CLINIC_ADMIN
                        || r == RoleName.FINANCE_REPORTS || r == RoleName.MEDICAL_REPORTS
        );

        if (!canAccessClinicReports) {
            return ApiResponse.unauthorised("You do not have permission to access clinic reports.");
        }

        LocalDate today = LocalDate.now();
        LocalDate startDate;
        LocalDate endDate;

        try {
            if (fromDate != null && !fromDate.isBlank()) {
                startDate = parseLocalDate(fromDate);
            } else {
                startDate = today;
            }
        } catch (Exception e) {
            startDate = today;
        }

        try {
            if (toDate != null && !toDate.isBlank()) {
                endDate = parseLocalDate(toDate);
            } else {
                endDate = startDate;
            }
        } catch (Exception e) {
            endDate = startDate;
        }

        if (endDate.isBefore(startDate)) {
            LocalDate tmp = startDate;
            startDate = endDate;
            endDate = tmp;
        }

        String fromDateStr = startDate.format(DateTimeFormatter.ISO_LOCAL_DATE);
        String toDateStr = endDate.format(DateTimeFormatter.ISO_LOCAL_DATE);

        long diffDays = java.time.temporal.ChronoUnit.DAYS.between(startDate, endDate);
        String period = (periodInput != null && !periodInput.isBlank())
                ? periodInput.toLowerCase().trim()
                : (diffDays == 0 ? "today" : (diffDays <= 7 ? "week" : (diffDays <= 31 ? "month" : "custom")));

        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(LocalTime.MAX);

        Specification<Visit> spec = (root, queryDef, builder) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(builder.or(
                    builder.and(
                            builder.greaterThanOrEqualTo(root.get("visitDate"), startDateTime),
                            builder.lessThanOrEqualTo(root.get("visitDate"), endDateTime)
                    ),
                    builder.and(
                            builder.greaterThanOrEqualTo(root.get("createdAt"), startDateTime),
                            builder.lessThanOrEqualTo(root.get("createdAt"), endDateTime)
                    )
            ));
            return builder.and(predicates.toArray(new Predicate[0]));
        };

        List<Visit> rawVisits = visitRepository.findAll(spec, Sort.by(Sort.Direction.DESC, "visitDate"));

        List<VisitDto> visits = rawVisits.stream()
                .map(v -> visitService.visitToDto(v, Set.of(), authUser))
                .toList();

        Map<String, Object> reportData = calculateClinicReportsData(
                visits,
                period,
                fromDateStr,
                toDateStr,
                startDate,
                endDate,
                diffDays,
                departmentId,
                departmentIds,
                clinicianId,
                clinicianIds,
                insuranceId,
                insuranceIds,
                isPrivateOnly
        );

        return ApiResponse.success("Clinic reports generated successfully.", reportData);
    }

    private Map<String, Object> calculateClinicReportsData(
            List<VisitDto> visits,
            String period,
            String fromDate,
            String toDate,
            LocalDate startDate,
            LocalDate endDate,
            long diffDays,
            UUID filterDeptId,
            List<UUID> filterDeptIds,
            UUID filterClinicianId,
            List<UUID> filterClinicianIds,
            UUID filterInsuranceId,
            List<UUID> filterInsuranceIds,
            Boolean filterPrivateOnly
    ) {
        boolean isDailyView = "today".equals(period) || fromDate.equals(toDate) || diffDays == 0;
        boolean isWeeklyView = !isDailyView && ("week".equals(period) || diffDays <= 7);

        Set<String> uniquePatientsSet = new HashSet<>();
        int maleCount = 0;
        int femaleCount = 0;
        int otherGenderCount = 0;
        List<Integer> patientAges = new ArrayList<>();
        int under18 = 0;
        int adults18to50 = 0;
        int seniors50plus = 0;

        int totalVisitsCount = 0;
        Map<String, Integer> encountersByHour = new HashMap<>();

        double totalGrossBilled = 0;
        double insuranceCoveredAmount = 0;
        double patientShareAmount = 0;
        double patientCashCollected = 0;
        double patientLoanAmount = 0;
        double giveawayAmount = 0;
        double waiverAmount = 0;
        double pendingBillingAmount = 0;

        int loanCount = 0;
        int giveawayCount = 0;
        int waiverCount = 0;
        int pendingBillingCount = 0;

        double momoCollected = 0;
        double cashCollected = 0;
        double cardCollected = 0;
        double bankTransferCollected = 0;
        double otherCollected = 0;

        Map<String, Map<String, Object>> paymentModesBreakdownMap = new HashMap<>();
        Map<String, Map<String, Object>> insuranceBreakdownMap = new HashMap<>();
        Map<String, Map<String, Object>> departmentBreakdownMap = new HashMap<>();
        Map<String, Map<String, Object>> clinicianBreakdownMap = new HashMap<>();
        Map<String, Map<String, Object>> categoryBreakdownMap = new HashMap<>();

        Map<String, Map<String, Double>> moneyTimelineMap = new LinkedHashMap<>();

        List<Map<String, Object>> waiversList = new ArrayList<>();
        List<Map<String, Object>> giveawaysList = new ArrayList<>();
        List<Map<String, Object>> adjustmentsList = new ArrayList<>();
        List<Map<String, Object>> loansList = new ArrayList<>();
        List<Map<String, Object>> diagnosticReportsList = new ArrayList<>();

        if (isDailyView) {
            for (int h = 6; h <= 20; h++) {
                String hKey = String.format("%02d:00", h);
                moneyTimelineMap.put(hKey, createClinicMoneyTimelineBucket());
            }
        } else if (diffDays <= 31) {
            LocalDate cur = startDate;
            while (!cur.isAfter(endDate)) {
                String dKey = cur.format(DateTimeFormatter.ISO_LOCAL_DATE);
                moneyTimelineMap.put(dKey, createClinicMoneyTimelineBucket());
                cur = cur.plusDays(1);
            }
        }

        Set<String> filterDeptIdsSet = new HashSet<>();
        if (filterDeptId != null) {
            filterDeptIdsSet.add(filterDeptId.toString());
        }
        if (filterDeptIds != null) {
            for (UUID did : filterDeptIds) {
                if (did != null) filterDeptIdsSet.add(did.toString());
            }
        }

        Set<String> filterClinicianIdsSet = new HashSet<>();
        if (filterClinicianId != null) {
            filterClinicianIdsSet.add(filterClinicianId.toString());
        }
        if (filterClinicianIds != null) {
            for (UUID cid : filterClinicianIds) {
                if (cid != null) filterClinicianIdsSet.add(cid.toString());
            }
        }

        Set<String> filterInsuranceIdsSet = new HashSet<>();
        if (filterInsuranceId != null) {
            filterInsuranceIdsSet.add(filterInsuranceId.toString());
        }
        if (filterInsuranceIds != null) {
            for (UUID iid : filterInsuranceIds) {
                if (iid != null) filterInsuranceIdsSet.add(iid.toString());
            }
        }

        for (VisitDto visit : visits) {
            String vTime = visit.visitDate() != null ? visit.visitDate().toString() : "";
            if (!isWithinDateRange(vTime, fromDate, toDate)) continue;

            boolean hasInsurance = visit.linkedInsurances() != null && !visit.linkedInsurances().isEmpty();

            // Payer filter
            if (Boolean.TRUE.equals(filterPrivateOnly) && hasInsurance) {
                continue;
            }

            if (!filterInsuranceIdsSet.isEmpty()) {
                boolean matchesIns = visit.linkedInsurances() != null && visit.linkedInsurances().stream().anyMatch(ins -> {
                    if (ins.id() != null && filterInsuranceIdsSet.contains(ins.id().toString())) return true;
                    if (ins.insuranceProvider() != null && ins.insuranceProvider().id() != null && filterInsuranceIdsSet.contains(ins.insuranceProvider().id().toString())) return true;
                    return false;
                });
                if (!matchesIns) continue;
            }

            // Department filter for visit
            if (!filterDeptIdsSet.isEmpty()) {
                boolean hasMatchingDept = visit.departments() != null && visit.departments().stream().anyMatch(d ->
                        d.department() != null && d.department().id() != null && filterDeptIdsSet.contains(d.department().id().toString())
                );
                if (!hasMatchingDept) continue;
            }

            // Clinician filter for visit
            if (!filterClinicianIdsSet.isEmpty()) {
                boolean hasMatchingClinician = visit.departments() != null && visit.departments().stream().anyMatch(d ->
                        isProcessorInSet(d.processors(), filterClinicianIdsSet) ||
                        (d.completedBy() != null && isUserInSet(d.completedBy().id(), filterClinicianIdsSet)) ||
                        (d.products() != null && d.products().stream().anyMatch(p ->
                                (p.processor() != null && isUserInSet(p.processor().id(), filterClinicianIdsSet)) ||
                                (p.addedBy() != null && isUserInSet(p.addedBy().id(), filterClinicianIdsSet))
                        ))
                );
                if (!hasMatchingClinician) continue;
            }

            totalVisitsCount++;

            // Patient demographics
            String patientId = visit.patient() != null && visit.patient().id() != null ? visit.patient().id().toString() : "";
            String patientName = "Patient";
            String patientIdentifier = "";
            String patientPhone = "";
            String patientGender = "";
            Integer patientAge = null;

            if (visit.patient() != null) {
                String fn = visit.patient().firstName() != null ? visit.patient().firstName().trim() : "";
                String ln = visit.patient().lastName() != null ? visit.patient().lastName().trim() : "";
                patientName = (!fn.isEmpty() || !ln.isEmpty()) ? (fn + " " + ln).trim() : (visit.patient().fullName() != null ? visit.patient().fullName() : "Patient");
                patientIdentifier = visit.patient().patientIdentifier() != null ? visit.patient().patientIdentifier() : "";
                patientPhone = visit.patient().primaryPhoneNumber() != null ? visit.patient().primaryPhoneNumber() : "";
                patientGender = visit.patient().gender() != null ? visit.patient().gender().name() : "";

                patientAge = calculatePatientAge(visit.patient().dateOfBirth() != null ? visit.patient().dateOfBirth().toString() : null, null);

                if (!patientId.isEmpty() && uniquePatientsSet.add(patientId)) {
                    if ("MALE".equalsIgnoreCase(patientGender)) maleCount++;
                    else if ("FEMALE".equalsIgnoreCase(patientGender)) femaleCount++;
                    else otherGenderCount++;

                    if (patientAge != null) {
                        patientAges.add(patientAge);
                        if (patientAge < 18) under18++;
                        else if (patientAge <= 50) adults18to50++;
                        else seniors50plus++;
                    }
                }
            }

            LocalDateTime vDateTime = parseDateTime(vTime);
            if (vDateTime != null) {
                String hKey = String.format("%02d:00", vDateTime.getHour());
                encountersByHour.put(hKey, encountersByHour.getOrDefault(hKey, 0) + 1);
            }

            // Iterate departments
            if (visit.departments() != null) {
                for (VisitDepartmentDto dept : visit.departments()) {
                    if (!filterDeptIdsSet.isEmpty()) {
                        if (dept.department() == null || dept.department().id() == null || !filterDeptIdsSet.contains(dept.department().id().toString())) {
                            continue;
                        }
                    }

                    if (!filterClinicianIdsSet.isEmpty()) {
                        boolean matchDeptClin = isProcessorInSet(dept.processors(), filterClinicianIdsSet) ||
                                (dept.completedBy() != null && isUserInSet(dept.completedBy().id(), filterClinicianIdsSet)) ||
                                (dept.products() != null && dept.products().stream().anyMatch(p ->
                                        (p.processor() != null && isUserInSet(p.processor().id(), filterClinicianIdsSet)) ||
                                        (p.addedBy() != null && isUserInSet(p.addedBy().id(), filterClinicianIdsSet))
                                ));
                        if (!matchDeptClin) continue;
                    }

                    String deptId = dept.department() != null && dept.department().id() != null ? dept.department().id().toString() : (dept.id() != null ? dept.id().toString() : "");
                    String deptName = dept.department() != null && dept.department().name() != null ? dept.department().name() : "Consultation";
                    String deptTime = dept.startedAt() != null ? dept.startedAt().toString() : (dept.createdAt() != null ? dept.createdAt().toString() : vTime);

                    // Resolve primary clinician for department
                    String clinName = "N/A";
                    String clinIdStr = "";
                    if (dept.completedBy() != null && dept.completedBy().id() != null) {
                        clinIdStr = dept.completedBy().id().toString();
                        String fn = dept.completedBy().firstName() != null ? dept.completedBy().firstName() : "";
                        String ln = dept.completedBy().lastName() != null ? dept.completedBy().lastName() : "";
                        clinName = (fn + " " + ln).trim();
                    } else if (dept.processors() != null && !dept.processors().isEmpty()) {
                        var p0 = dept.processors().iterator().next();
                        if (p0 != null && p0.id() != null) {
                            clinIdStr = p0.id().toString();
                            String fn = p0.firstName() != null ? p0.firstName() : "";
                            String ln = p0.lastName() != null ? p0.lastName() : "";
                            clinName = (fn + " " + ln).trim();
                        }
                    }

                    Map<String, Object> deptBilling = (dept.id() != null) ? billingQueryService.departmentBilling(dept.id()) : null;

                    double deptGross = 0;
                    double deptInsCov = 0;
                    double deptPatPay = 0;
                    double deptPaid = 0;
                    double deptLoan = 0;
                    double deptGiveaway = 0;
                    double deptWaiver = 0;
                    double deptPending = 0;

                    if (deptBilling != null) {
                        Object insListObj = deptBilling.get("insuranceBillings");
                        if (insListObj instanceof List<?> list && !list.isEmpty()) {
                            for (Object itemObj : list) {
                                if (itemObj instanceof Map<?, ?> ib) {
                                    String insName = resolveVisitInsuranceAcronym(dept, visit, deptBilling, hasInsurance);
                                    Object pIns = ib.get("patientInsurance");
                                    if (pIns instanceof Map<?, ?> pMap) {
                                        Object prov = pMap.get("insuranceProvider");
                                        if (prov instanceof Map<?, ?> provMap) {
                                            String acr = getInsuranceAcronym((String) provMap.get("name"), (String) provMap.get("acronym"));
                                            if (acr != null) insName = acr;
                                        }
                                    }

                                    double tot = toDouble(ib.get("totalAmount"));
                                    double insCov = toDouble(ib.get("insuranceCoveredAmount"));
                                    double patPay = toDouble(ib.get("patientPayableAmount"));
                                    double paid = toDouble(ib.get("paidAmount"));
                                    double outstanding = toDouble(ib.get("outstandingAmount"));
                                    String outstandingType = ib.get("outstandingType") != null ? ib.get("outstandingType").toString() : "";
                                    String outstandingReason = ib.get("outstandingReason") != null ? ib.get("outstandingReason").toString() : "";

                                    boolean isGiveaway = "GIVEAWAY".equalsIgnoreCase(outstandingType);
                                    boolean isLoan = "LOAN".equalsIgnoreCase(outstandingType) || (!isGiveaway && outstanding > 0);

                                    double loanVal = isLoan ? outstanding : 0;
                                    double giveVal = isGiveaway ? outstanding : 0;

                                    deptGross += tot;
                                    deptInsCov += insCov;
                                    deptPatPay += patPay;
                                    deptPaid += paid;
                                    deptLoan += loanVal;
                                    deptGiveaway += giveVal;

                                    totalGrossBilled += tot;
                                    insuranceCoveredAmount += insCov;
                                    patientShareAmount += patPay;
                                    patientCashCollected += paid;

                                    if (loanVal > 0) {
                                        patientLoanAmount += loanVal;
                                        loanCount++;
                                        Map<String, Object> loanEntry = new HashMap<>();
                                        loanEntry.put("id", "loan-" + dept.id() + "-" + loansList.size());
                                        loanEntry.put("patientName", patientName);
                                        loanEntry.put("patientIdentifier", patientIdentifier);
                                        loanEntry.put("patientPhone", patientPhone);
                                        loanEntry.put("departmentName", deptName);
                                        loanEntry.put("clinicianName", clinName);
                                        loanEntry.put("totalBilled", patPay);
                                        loanEntry.put("paidAmount", paid);
                                        loanEntry.put("outstandingAmount", loanVal);
                                        loanEntry.put("reason", outstandingReason.isBlank() ? "Outstanding patient balance" : outstandingReason);
                                        loanEntry.put("status", paid > 0 ? "PARTIALLY_PAID" : "UNPAID");
                                        loanEntry.put("date", deptTime);
                                        loansList.add(loanEntry);
                                    }

                                    if (giveVal > 0) {
                                        giveawayAmount += giveVal;
                                        giveawayCount++;
                                        Map<String, Object> giveEntry = new HashMap<>();
                                        giveEntry.put("id", "give-" + dept.id() + "-" + giveawaysList.size());
                                        giveEntry.put("patientName", patientName);
                                        giveEntry.put("patientIdentifier", patientIdentifier);
                                        giveEntry.put("departmentName", deptName);
                                        giveEntry.put("clinicianName", clinName);
                                        giveEntry.put("amount", giveVal);
                                        giveEntry.put("productName", "Absorbed department balance");
                                        giveEntry.put("reason", outstandingReason.isBlank() ? "Clinic absorbed" : outstandingReason);
                                        giveEntry.put("date", deptTime);
                                        giveawaysList.add(giveEntry);
                                    }

                                    accumulateInsuranceSummary(insuranceBreakdownMap, insName, tot, insCov, patPay, paid, loanVal, giveVal);
                                }
                            }
                        } else {
                            double tot = toDouble(deptBilling.get("totalAmount"));
                            double insCov = toDouble(deptBilling.get("insuranceCoveredAmount"));
                            double patPay = toDouble(deptBilling.get("patientPayableAmount"));
                            double paid = toDouble(deptBilling.get("paidAmount"));
                            double outstanding = toDouble(deptBilling.get("outstandingAmount"));

                            deptGross += tot;
                            deptInsCov += insCov;
                            deptPatPay += patPay;
                            deptPaid += paid;

                            totalGrossBilled += tot;
                            insuranceCoveredAmount += insCov;
                            patientShareAmount += patPay;
                            patientCashCollected += paid;

                            if (outstanding > 0) {
                                deptLoan += outstanding;
                                patientLoanAmount += outstanding;
                                loanCount++;
                                Map<String, Object> loanEntry = new HashMap<>();
                                loanEntry.put("id", "loan-" + dept.id() + "-" + loansList.size());
                                loanEntry.put("patientName", patientName);
                                loanEntry.put("patientIdentifier", patientIdentifier);
                                loanEntry.put("patientPhone", patientPhone);
                                loanEntry.put("departmentName", deptName);
                                loanEntry.put("clinicianName", clinName);
                                loanEntry.put("totalBilled", patPay);
                                loanEntry.put("paidAmount", paid);
                                loanEntry.put("outstandingAmount", outstanding);
                                loanEntry.put("reason", "Outstanding patient balance");
                                loanEntry.put("status", paid > 0 ? "PARTIALLY_PAID" : "UNPAID");
                                loanEntry.put("date", deptTime);
                                loansList.add(loanEntry);
                            }
                        }

                        // Collect payment modes
                        Object paymentsObj = deptBilling.get("payments");
                        if (paymentsObj instanceof List<?> pList) {
                            for (Object payObj : pList) {
                                if (payObj instanceof Map<?, ?> pay) {
                                    double payAmt = toDouble(pay.get("amount"));
                                    if (payAmt <= 0) continue;
                                    String rawMethod = pay.get("paymentMethod") != null ? pay.get("paymentMethod").toString().toUpperCase() : "CASH";
                                    String methodKey = "CASH";
                                    if (rawMethod.contains("MOBILE") || rawMethod.contains("MOMO") || rawMethod.contains("MTN") || rawMethod.contains("AIRTEL")) {
                                        methodKey = "MOBILE_MONEY";
                                        momoCollected += payAmt;
                                    } else if (rawMethod.contains("CARD") || rawMethod.contains("POS") || rawMethod.contains("VISA") || rawMethod.contains("MASTERCARD")) {
                                        methodKey = "CARD";
                                        cardCollected += payAmt;
                                    } else if (rawMethod.contains("BANK") || rawMethod.contains("TRANSFER")) {
                                        methodKey = "BANK_TRANSFER";
                                        bankTransferCollected += payAmt;
                                    } else if (rawMethod.contains("CASH")) {
                                        methodKey = "CASH";
                                        cashCollected += payAmt;
                                    } else {
                                        methodKey = rawMethod;
                                        otherCollected += payAmt;
                                    }

                                    if (!paymentModesBreakdownMap.containsKey(methodKey)) {
                                        Map<String, Object> pModeMap = new HashMap<>();
                                        pModeMap.put("amount", 0.0);
                                        pModeMap.put("count", 0);
                                        paymentModesBreakdownMap.put(methodKey, pModeMap);
                                    }
                                    Map<String, Object> pmEntry = paymentModesBreakdownMap.get(methodKey);
                                    pmEntry.put("amount", toDouble(pmEntry.get("amount")) + payAmt);
                                    pmEntry.put("count", (int) pmEntry.get("count") + 1);
                                }
                            }
                        }
                    }

                    // Products inspection (Category breakdown, Waivers, Adjustments, Pending billing)
                    if (dept.products() != null) {
                        for (VisitDepartmentProductDto prod : dept.products()) {
                            String pStatus = prod.status() != null ? prod.status().name() : "";
                            String prodName = prod.product() != null && prod.product().name() != null ? prod.product().name() : "Item";
                            String prodType = prod.product() != null && prod.product().type() != null ? prod.product().type().name() : "CONSULTATION";
                            double qty = prod.quantity() != null ? prod.quantity().doubleValue() : 1.0;

                            Map<String, Object> pBillingItem = (prod.id() != null) ? billingQueryService.productBillingItem(prod.id()) : null;
                            double catalogPrice = prod.product() != null && prod.product().clinicPrice() != null ? prod.product().clinicPrice().doubleValue() : 0.0;
                            double unitPrice = (pBillingItem != null && pBillingItem.get("unitPriceSnapshot") != null)
                                    ? toDouble(pBillingItem.get("unitPriceSnapshot"))
                                    : catalogPrice;
                            double lineTotal = qty * unitPrice;

                            // Category accumulation
                            if (!categoryBreakdownMap.containsKey(prodType)) {
                                Map<String, Object> catMap = new HashMap<>();
                                catMap.put("count", 0.0);
                                catMap.put("totalAmount", 0.0);
                                categoryBreakdownMap.put(prodType, catMap);
                            }
                            Map<String, Object> cEntry = categoryBreakdownMap.get(prodType);
                            cEntry.put("count", toDouble(cEntry.get("count")) + qty);
                            cEntry.put("totalAmount", toDouble(cEntry.get("totalAmount")) + lineTotal);

                            // Waivers check
                            if ("EXEMPTED".equals(pStatus)) {
                                waiverAmount += lineTotal;
                                waiverCount++;
                                deptWaiver += lineTotal;
                                Map<String, Object> wEntry = new HashMap<>();
                                wEntry.put("id", "waive-" + prod.id());
                                wEntry.put("patientName", patientName);
                                wEntry.put("patientIdentifier", patientIdentifier);
                                wEntry.put("departmentName", deptName);
                                wEntry.put("clinicianName", clinName);
                                wEntry.put("amount", lineTotal);
                                wEntry.put("waiverType", "FULL");
                                wEntry.put("productName", prodName);
                                wEntry.put("reason", "Full clinical waiver exemption");
                                wEntry.put("date", deptTime);
                                waiversList.add(wEntry);
                            } else if ("PATIENT_SHARE_EXEMPTED".equals(pStatus)) {
                                double patShare = (pBillingItem != null && pBillingItem.get("patientPayableAmount") != null)
                                        ? toDouble(pBillingItem.get("patientPayableAmount"))
                                        : lineTotal;
                                waiverAmount += patShare;
                                waiverCount++;
                                deptWaiver += patShare;
                                Map<String, Object> wEntry = new HashMap<>();
                                wEntry.put("id", "waive-" + prod.id());
                                wEntry.put("patientName", patientName);
                                wEntry.put("patientIdentifier", patientIdentifier);
                                wEntry.put("departmentName", deptName);
                                wEntry.put("clinicianName", clinName);
                                wEntry.put("amount", patShare);
                                wEntry.put("waiverType", "PATIENT_SHARE");
                                wEntry.put("productName", prodName);
                                wEntry.put("reason", "Patient share waived");
                                wEntry.put("date", deptTime);
                                waiversList.add(wEntry);
                            }

                            // Price adjustment check
                            if (catalogPrice > 0 && Math.abs(catalogPrice - unitPrice) > 0.01) {
                                Map<String, Object> adj = new HashMap<>();
                                adj.put("id", "adj-" + prod.id());
                                adj.put("patientName", patientName);
                                adj.put("patientIdentifier", patientIdentifier);
                                adj.put("departmentName", deptName);
                                adj.put("clinicianName", clinName);
                                adj.put("productName", prodName);
                                adj.put("originalPrice", catalogPrice);
                                adj.put("adjustedPrice", unitPrice);
                                adj.put("difference", Math.abs(catalogPrice - unitPrice) * qty);
                                adj.put("reason", catalogPrice > unitPrice ? "Price discount override" : "Custom price adjustment");
                                adj.put("date", deptTime);
                                adjustmentsList.add(adj);
                            }

                            // Pending on billing check
                            boolean isUnbilled = !"BILLED".equals(pStatus) && !"EXEMPTED".equals(pStatus) && !"PATIENT_SHARE_EXEMPTED".equals(pStatus);
                            if (isUnbilled && lineTotal > 0) {
                                pendingBillingAmount += lineTotal;
                                pendingBillingCount++;
                                deptPending += lineTotal;
                            }
                        }
                    }

                    // Department summary accumulation
                    if (!departmentBreakdownMap.containsKey(deptId)) {
                        Map<String, Object> dMap = new HashMap<>();
                        dMap.put("departmentId", deptId);
                        dMap.put("name", deptName);
                        dMap.put("gross", 0.0);
                        dMap.put("insurance", 0.0);
                        dMap.put("patientPaid", 0.0);
                        dMap.put("loans", 0.0);
                        dMap.put("giveaways", 0.0);
                        dMap.put("waivers", 0.0);
                        dMap.put("pending", 0.0);
                        dMap.put("visitsCount", 0);
                        departmentBreakdownMap.put(deptId, dMap);
                    }
                    Map<String, Object> dEntry = departmentBreakdownMap.get(deptId);
                    dEntry.put("gross", toDouble(dEntry.get("gross")) + deptGross);
                    dEntry.put("insurance", toDouble(dEntry.get("insurance")) + deptInsCov);
                    dEntry.put("patientPaid", toDouble(dEntry.get("patientPaid")) + deptPaid);
                    dEntry.put("loans", toDouble(dEntry.get("loans")) + deptLoan);
                    dEntry.put("giveaways", toDouble(dEntry.get("giveaways")) + deptGiveaway);
                    dEntry.put("waivers", toDouble(dEntry.get("waivers")) + deptWaiver);
                    dEntry.put("pending", toDouble(dEntry.get("pending")) + deptPending);
                    dEntry.put("visitsCount", (int) dEntry.get("visitsCount") + 1);

                    // Clinician summary accumulation
                    if (!clinIdStr.isEmpty()) {
                        if (!clinicianBreakdownMap.containsKey(clinIdStr)) {
                            Map<String, Object> cMap = new HashMap<>();
                            cMap.put("clinicianId", clinIdStr);
                            cMap.put("name", clinName);
                            cMap.put("consultationsCount", 0);
                            cMap.put("gross", 0.0);
                            cMap.put("insurance", 0.0);
                            cMap.put("patientPaid", 0.0);
                            cMap.put("pending", 0.0);
                            cMap.put("waivers", 0.0);
                            clinicianBreakdownMap.put(clinIdStr, cMap);
                        }
                        Map<String, Object> cEntry = clinicianBreakdownMap.get(clinIdStr);
                        cEntry.put("consultationsCount", (int) cEntry.get("consultationsCount") + 1);
                        cEntry.put("gross", toDouble(cEntry.get("gross")) + deptGross);
                        cEntry.put("insurance", toDouble(cEntry.get("insurance")) + deptInsCov);
                        cEntry.put("patientPaid", toDouble(cEntry.get("patientPaid")) + deptPaid);
                        cEntry.put("pending", toDouble(cEntry.get("pending")) + deptPending);
                        cEntry.put("waivers", toDouble(cEntry.get("waivers")) + deptWaiver);
                    }

                    // Bump timeline
                    bumpClinicMoneyTimeline(moneyTimelineMap, deptTime, deptGross, deptPaid, deptInsCov, deptLoan, deptGiveaway, deptWaiver, isDailyView);

                    // Collect clinical diagnostics for epidemiological / diagnostic reports
                    if (dept.diagnostics() != null && !dept.diagnostics().isEmpty()) {
                        List<String> deptSymptoms = new ArrayList<>();
                        if (dept.symptoms() != null) {
                            for (VisitDepartmentSymptomDto s : dept.symptoms()) {
                                if (s != null && s.symptomName() != null && !s.symptomName().isBlank()) {
                                    deptSymptoms.add(s.symptomName().trim());
                                }
                            }
                        }

                        List<String> deptMedications = new ArrayList<>();
                        if (dept.medications() != null) {
                            for (VisitDepartmentMedicationDto m : dept.medications()) {
                                if (m != null && m.medicationName() != null && !m.medicationName().isBlank()) {
                                    deptMedications.add(m.medicationName().trim());
                                }
                            }
                        }

                        for (VisitDepartmentDiagnosisDto diag : dept.diagnostics()) {
                            if (diag == null) continue;
                            String dName = diag.diagnosisName() != null ? diag.diagnosisName().trim() : "";
                            if (dName.isEmpty()) continue;

                            String icdCode = diag.icd11Code() != null ? diag.icd11Code().trim() : "";
                            String diagType = diag.type() != null ? diag.type().name() : "FINAL";

                            Map<String, Object> diagRecord = new HashMap<>();
                            diagRecord.put("id", diag.id() != null ? diag.id().toString() : ("diag-" + diagnosticReportsList.size()));
                            diagRecord.put("diagnosisName", dName);
                            diagRecord.put("icd11Code", icdCode);
                            diagRecord.put("type", diagType);
                            diagRecord.put("notes", diag.notes() != null ? diag.notes() : "");
                            diagRecord.put("patientId", patientId);
                            diagRecord.put("patientName", patientName);
                            diagRecord.put("patientGender", patientGender);
                            diagRecord.put("patientAge", patientAge);
                            diagRecord.put("departmentId", deptId);
                            diagRecord.put("departmentName", deptName);
                            diagRecord.put("clinicianId", clinIdStr);
                            diagRecord.put("clinicianName", clinName);
                            diagRecord.put("date", deptTime);
                            diagRecord.put("symptoms", deptSymptoms);
                            diagRecord.put("medications", deptMedications);

                            diagnosticReportsList.add(diagRecord);
                        }
                    }
                }
            }
        }

        // Reconcile payment modes with patient cash collected
        double totalPaymentModesCollected = momoCollected + cashCollected + cardCollected + bankTransferCollected + otherCollected;
        if (patientCashCollected > totalPaymentModesCollected) {
            double diff = patientCashCollected - totalPaymentModesCollected;
            cashCollected += diff;
            if (!paymentModesBreakdownMap.containsKey("CASH")) {
                Map<String, Object> cMap = new HashMap<>();
                cMap.put("amount", 0.0);
                cMap.put("count", 0);
                paymentModesBreakdownMap.put("CASH", cMap);
            }
            Map<String, Object> cEntry = paymentModesBreakdownMap.get("CASH");
            cEntry.put("amount", toDouble(cEntry.get("amount")) + diff);
            cEntry.put("count", (int) cEntry.get("count") + 1);
        }

        // Peak hour
        String peakActivityHour = "N/A";
        int maxEncounters = 0;
        for (Map.Entry<String, Integer> e : encountersByHour.entrySet()) {
            if (e.getValue() > maxEncounters) {
                maxEncounters = e.getValue();
                peakActivityHour = e.getKey();
            }
        }
        if (!"N/A".equals(peakActivityHour)) {
            try {
                int h = Integer.parseInt(peakActivityHour.substring(0, 2));
                peakActivityHour = formatHourRange(h);
            } catch (Exception ignored) {}
        }

        double avgAge = patientAges.isEmpty() ? 0 : patientAges.stream().mapToInt(Integer::intValue).average().orElse(0.0);
        double totalSettled = insuranceCoveredAmount + patientCashCollected + giveawayAmount + waiverAmount;
        int settlementRatePct = totalGrossBilled > 0 ? (int) Math.min(100, Math.round((totalSettled / totalGrossBilled) * 100)) : 100;
        double outstandingBalance = Math.max(0, totalGrossBilled - insuranceCoveredAmount - patientCashCollected - giveawayAmount - waiverAmount);

        List<Map<String, Object>> insuranceBreakdown = new ArrayList<>(insuranceBreakdownMap.values());
        insuranceBreakdown.sort((a, b) -> Double.compare(toDouble(b.get("totalAmount")), toDouble(a.get("totalAmount"))));

        List<Map<String, Object>> departmentBreakdown = new ArrayList<>(departmentBreakdownMap.values());
        departmentBreakdown.sort((a, b) -> Double.compare(toDouble(b.get("gross")), toDouble(a.get("gross"))));

        List<Map<String, Object>> clinicianBreakdown = new ArrayList<>(clinicianBreakdownMap.values());
        clinicianBreakdown.sort((a, b) -> Double.compare(toDouble(b.get("gross")), toDouble(a.get("gross"))));

        List<Map<String, Object>> moneyTimeline = formatClinicMoneyTimeline(moneyTimelineMap, isDailyView, isWeeklyView);

        // Assemble overview
        Map<String, Object> overview = new HashMap<>();
        overview.put("totalPatients", uniquePatientsSet.size());
        overview.put("totalVisits", totalVisitsCount);
        overview.put("maleCount", maleCount);
        overview.put("femaleCount", femaleCount);
        overview.put("otherGenderCount", otherGenderCount);
        overview.put("averageAge", (int) Math.round(avgAge));
        overview.put("ageBrackets", Map.of("under18", under18, "adults18to50", adults18to50, "seniors50plus", seniors50plus));
        overview.put("peakActivityHour", peakActivityHour);
        overview.put("totalGrossBilled", totalGrossBilled);
        overview.put("insuranceCoveredAmount", insuranceCoveredAmount);
        overview.put("patientShareAmount", patientShareAmount);
        overview.put("patientCashCollected", patientCashCollected);
        overview.put("patientLoanAmount", patientLoanAmount);
        overview.put("giveawayAmount", giveawayAmount);
        overview.put("waiverAmount", waiverAmount);
        overview.put("pendingBillingAmount", pendingBillingAmount);
        overview.put("outstandingBalance", outstandingBalance);
        overview.put("loanCount", loanCount);
        overview.put("giveawayCount", giveawayCount);
        overview.put("waiverCount", waiverCount);
        overview.put("pendingBillingCount", pendingBillingCount);
        overview.put("settlementRatePct", settlementRatePct);

        // Assemble financial breakdown
        Map<String, Object> financialBreakdown = new HashMap<>();
        financialBreakdown.put("momoCollected", momoCollected);
        financialBreakdown.put("cashCollected", cashCollected);
        financialBreakdown.put("cardCollected", cardCollected);
        financialBreakdown.put("bankTransferCollected", bankTransferCollected);
        financialBreakdown.put("otherCollected", otherCollected);
        financialBreakdown.put("paymentModesBreakdown", paymentModesBreakdownMap);
        financialBreakdown.put("insuranceBreakdown", insuranceBreakdown);
        financialBreakdown.put("categoryBreakdown", categoryBreakdownMap);
        financialBreakdown.put("departmentBreakdown", departmentBreakdown);
        financialBreakdown.put("clinicianBreakdown", clinicianBreakdown);
        financialBreakdown.put("moneyTimeline", moneyTimeline);

        // Assemble deep reports
        Map<String, Object> deepReports = new HashMap<>();
        deepReports.put("waivers", waiversList);
        deepReports.put("giveaways", giveawaysList);
        deepReports.put("adjustments", adjustmentsList);
        deepReports.put("loans", loansList);
        deepReports.put("diagnosticReports", diagnosticReportsList);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("period", period);
        result.put("fromDate", fromDate);
        result.put("toDate", toDate);
        result.put("overview", overview);
        result.put("financialBreakdown", financialBreakdown);
        result.put("deepReports", deepReports);
        result.put("diagnosticReports", diagnosticReportsList);

        return result;
    }

    private Map<String, Double> createClinicMoneyTimelineBucket() {
        Map<String, Double> m = new HashMap<>();
        m.put("gross", 0.0);
        m.put("collected", 0.0);
        m.put("insurance", 0.0);
        m.put("loan", 0.0);
        m.put("giveaway", 0.0);
        m.put("waiver", 0.0);
        return m;
    }

    private void bumpClinicMoneyTimeline(
            Map<String, Map<String, Double>> moneyTimelineMap,
            String timestamp,
            double gross,
            double collected,
            double insCov,
            double loan,
            double giveaway,
            double waiver,
            boolean isDailyView
    ) {
        if (timestamp == null || timestamp.isBlank() || moneyTimelineMap.isEmpty()) return;
        LocalDateTime dt = parseDateTime(timestamp);
        if (dt == null) return;
        String key = isDailyView
                ? String.format("%02d:00", dt.getHour())
                : dt.format(DateTimeFormatter.ISO_LOCAL_DATE);
        Map<String, Double> bucket = moneyTimelineMap.get(key);
        if (bucket != null) {
            bucket.put("gross", bucket.getOrDefault("gross", 0.0) + gross);
            bucket.put("collected", bucket.getOrDefault("collected", 0.0) + collected);
            bucket.put("insurance", bucket.getOrDefault("insurance", 0.0) + insCov);
            bucket.put("loan", bucket.getOrDefault("loan", 0.0) + loan);
            bucket.put("giveaway", bucket.getOrDefault("giveaway", 0.0) + giveaway);
            bucket.put("waiver", bucket.getOrDefault("waiver", 0.0) + waiver);
        }
    }

    private List<Map<String, Object>> formatClinicMoneyTimeline(
            Map<String, Map<String, Double>> timelineMap,
            boolean isDailyView,
            boolean isWeeklyView
    ) {
        List<Map<String, Object>> list = new ArrayList<>();
        for (Map.Entry<String, Map<String, Double>> entry : timelineMap.entrySet()) {
            Map<String, Object> item = new HashMap<>();
            item.put("label", formatTimelineLabel(entry.getKey(), isDailyView, isWeeklyView));
            item.put("rawKey", entry.getKey());
            Map<String, Double> bucket = entry.getValue();
            item.put("gross", bucket.getOrDefault("gross", 0.0));
            item.put("collected", bucket.getOrDefault("collected", 0.0));
            item.put("insurance", bucket.getOrDefault("insurance", 0.0));
            item.put("loan", bucket.getOrDefault("loan", 0.0));
            item.put("giveaway", bucket.getOrDefault("giveaway", 0.0));
            item.put("waiver", bucket.getOrDefault("waiver", 0.0));
            list.add(item);
        }
        return list;
    }
}
