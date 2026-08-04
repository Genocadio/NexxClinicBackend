package com.nexxserve.nexxclinic.service;

import com.nexxserve.nexxclinic.dto.out.ApiResponse;
import com.nexxserve.nexxclinic.dto.out.PatientDto;
import com.nexxserve.nexxclinic.dto.out.PatientInsuranceDto;
import com.nexxserve.nexxclinic.dto.out.VisitDto;
import com.nexxserve.nexxclinic.entity.*;
import com.nexxserve.nexxclinic.graphql.input.*;
import com.nexxserve.nexxclinic.mappers.out.PatientInsuranceMapper;
import com.nexxserve.nexxclinic.mappers.out.PatientMapper;
import com.nexxserve.nexxclinic.model.ResponseStatus;
import com.nexxserve.nexxclinic.repository.DepartmentInsuranceBillingRepository;
import com.nexxserve.nexxclinic.repository.InsuranceProviderRepository;
import com.nexxserve.nexxclinic.repository.PatientInsuranceRepository;
import com.nexxserve.nexxclinic.repository.PatientRepository;
import com.nexxserve.nexxclinic.repository.VisitBillingItemRepository;
import com.nexxserve.nexxclinic.repository.VisitInsuranceRepository;
import com.nexxserve.nexxclinic.repository.VisitRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.Period;
import java.util.*;

@Service
public class PatientService {

    private static final Logger log = LoggerFactory.getLogger(PatientService.class);

    private final PatientRepository patientRepository;
    private final PatientMapper patientMapper;
    private final PatientInsuranceMapper patientInsuranceMapper;
    private final PatientInsuranceRepository patientInsuranceRepository;
    private final InsuranceProviderRepository insuranceProviderRepository;
    private final VisitRepository visitRepository;
    private final VisitInsuranceRepository visitInsuranceRepository;
    private final VisitBillingItemRepository visitBillingItemRepository;
    private final DepartmentInsuranceBillingRepository departmentInsuranceBillingRepository;
    private final VisitService visitService;
    private final MeilisearchIndexService meilisearchIndexService;

    public PatientService(
            PatientRepository patientRepository,
            PatientInsuranceRepository patientInsuranceRepository,
            InsuranceProviderRepository insuranceProviderRepository,
            PatientMapper patientMapper,
            PatientInsuranceMapper patientInsuranceMapper,
            VisitRepository visitRepository,
            VisitInsuranceRepository visitInsuranceRepository,
            VisitBillingItemRepository visitBillingItemRepository,
            DepartmentInsuranceBillingRepository departmentInsuranceBillingRepository,
            VisitService visitService,
            MeilisearchIndexService meilisearchIndexService
    ) {
        this.patientRepository = patientRepository;
        this.patientInsuranceRepository = patientInsuranceRepository;
        this.insuranceProviderRepository = insuranceProviderRepository;
        this.patientMapper = patientMapper;
        this.patientInsuranceMapper = patientInsuranceMapper;
        this.visitRepository = visitRepository;
        this.visitInsuranceRepository = visitInsuranceRepository;
        this.visitBillingItemRepository = visitBillingItemRepository;
        this.departmentInsuranceBillingRepository = departmentInsuranceBillingRepository;
        this.visitService = visitService;
        this.meilisearchIndexService = meilisearchIndexService;
    }

    // =========================
    // SEARCH PATIENTS
    // =========================
    @Transactional(readOnly = true)
    public ApiResponse<List<PatientDto>> searchPatients(SearchPatientsInput input) {
        int page = normalizePage(input == null ? null : input.page());
        int size = normalizeSize(input == null ? null : input.size());
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));

        // Age validation
        Integer exactAge = input == null ? null : input.age();
        Integer minAge = input == null ? null : input.minAge();
        Integer maxAge = input == null ? null : input.maxAge();

        if (exactAge != null && (minAge != null || maxAge != null)) {
            return ApiResponse.error("Use either age or minAge/maxAge filters, not both.");
        }

        if (exactAge != null && exactAge < 0) {
            return ApiResponse.error("age must be a positive number.");
        }

        if (minAge != null && minAge < 0) {
            return ApiResponse.error("minAge must be a positive number.");
        }

        if (maxAge != null && maxAge < 0) {
            return ApiResponse.error("maxAge must be a positive number.");
        }

        if (minAge != null && maxAge != null && minAge > maxAge) {
            return ApiResponse.error("minAge cannot be greater than maxAge.");
        }

        UUID insuranceProviderId = input == null ? null : input.insuranceProviderId();
        String phoneNumber = input == null ? null : blankToNull(input.phoneNumber());
        String name = input == null ? null : blankToNull(input.name());

        if (insuranceProviderId != null && !insuranceProviderRepository.existsById(insuranceProviderId)) {
            return ApiResponse.error("Insurance provider not found.");
        }

        // Meilisearch first (typo-tolerant, ranked); the DB spec below is the fallback.
        if (meilisearchIndexService.isEnabled()) {
            try {
                MeilisearchIndexService.SearchHit hit = meilisearchIndexService.searchPatients(
                        name,
                        phoneNumber,
                        insuranceProviderId,
                        exactAge,
                        minAge,
                        maxAge,
                        page,
                        size
                );
                List<PatientDto> dtos = loadPatientsByHits(hit.ids());
                int totalPages = size == 0 ? 0 : (int) Math.ceil((double) hit.total() / size);
                return ApiResponse.success(
                        "Patients fetched.",
                        dtos,
                        new com.nexxserve.nexxclinic.dto.out.PaginationDto(hit.total(), size, page, totalPages)
                );
            } catch (MeilisearchIndexService.SearchUnavailableException e) {
                log.warn("Meilisearch unavailable for patients, falling back to DB: {}", e.getMessage());
            }
        }

        Specification<Patient> spec = (root, query, cb) -> cb.conjunction();

        // Insurance provider filter
        if (insuranceProviderId != null) {
            if (!insuranceProviderRepository.existsById(insuranceProviderId)) {
                return ApiResponse.error("Insurance provider not found.");
            }

            Set<UUID> filteredPatientIds = patientInsuranceRepository.findByInsuranceProviderId(insuranceProviderId)
                    .stream()
                    .map(pi -> pi.getPatient().getId())
                    .collect(java.util.stream.Collectors.toSet());

            if (filteredPatientIds.isEmpty()) {
                return ApiResponse.success(
                        "Patients fetched.",
                        List.of(),
                        new com.nexxserve.nexxclinic.dto.out.PaginationDto(0, size, page, 0)
                );
            }

            spec = spec.and((root, query, cb) -> root.get("id").in(filteredPatientIds));
        }

        // Phone number filter
        if (phoneNumber != null) {
            String normalizedPhone = phoneNumber.toLowerCase();
            spec = spec.and((root, query, cb) ->
                    cb.or(
                            cb.like(cb.lower(root.get("primaryPhoneNumber")), "%" + normalizedPhone + "%"),
                            cb.like(cb.lower(root.get("alternativePhone")), "%" + normalizedPhone + "%")
                    )
            );
        }

        // Name filter with tokenization
        if (name != null) {
            String[] tokens = name.toLowerCase().split("\\s+");
            List<String> validTokens = new ArrayList<>();
            for (String token : tokens) {
                if (!token.isBlank()) {
                    validTokens.add(token.trim());
                }
            }

            for (String token : validTokens) {
                spec = spec.and((root, query, cb) ->
                        cb.or(
                                cb.like(cb.lower(root.get("fullName")), "%" + token + "%"),
                                cb.like(cb.lower(root.get("patientIdentifier")), "%" + token + "%")
                        )
                );
            }
        }

        // Age filters
        LocalDate today = LocalDate.now();
        if (exactAge != null) {
            LocalDate dobUpper = today.minusYears(exactAge);
            LocalDate dobLower = today.minusYears(exactAge + 1L).plusDays(1);
            spec = spec.and((root, query, cb) -> cb.between(root.get("dateOfBirth"), dobLower, dobUpper));
        } else {
            if (minAge != null) {
                LocalDate maxDob = today.minusYears(minAge);
                spec = spec.and((root, query, cb) -> cb.lessThanOrEqualTo(root.get("dateOfBirth"), maxDob));
            }
            if (maxAge != null) {
                LocalDate minDob = today.minusYears(maxAge + 1L).plusDays(1);
                spec = spec.and((root, query, cb) -> cb.greaterThanOrEqualTo(root.get("dateOfBirth"), minDob));
            }
        }

        Page<Patient> patientPage = patientRepository.findAll(spec, pageable);
        List<PatientDto> dtos = mapToDtos(patientPage.getContent());

        return ApiResponse.success(
                "Patients fetched.",
                dtos,
                new com.nexxserve.nexxclinic.dto.out.PaginationDto(
                        patientPage.getTotalElements(),
                        patientPage.getSize(),
                        patientPage.getNumber(),
                        patientPage.getTotalPages()
                )
        );
    }

    /** Hydrates full patient DTOs from Meilisearch hit ids, preserving hit order. */
    private List<PatientDto> loadPatientsByHits(List<UUID> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        Map<UUID, Patient> byId = new HashMap<>();
        patientRepository.findAllById(ids).forEach(p -> byId.put(p.getId(), p));
        List<Patient> ordered = new ArrayList<>();
        for (UUID id : ids) {
            Patient patient = byId.get(id);
            if (patient != null) {
                ordered.add(patient);
            }
        }
        return mapToDtos(ordered);
    }

    // =========================
    // CREATE PATIENT
    // =========================
    @Transactional
    public ApiResponse<VisitDto> createPatient(CreatePatientInput input) {
        if (input == null) {
            return ApiResponse.error("input is required.");
        }

        // Validate required fields
        String firstName = requiredTrim(input.firstName());
        if (firstName == null) {
            return ApiResponse.error("firstName is required.");
        }

        if (input.lastName() != null && input.lastName().isBlank()) {
            return ApiResponse.error("lastName cannot be blank when provided.");
        }

        if (input.dateOfBirth() == null || input.gender() == null) {
            return ApiResponse.error("dateOfBirth and gender are required.");
        }

        // Check for duplicate identifiers
        String nationalId = blankToNull(input.nationalIdNumber());
        if (nationalId != null && patientRepository.existsByNationalIdNumber(nationalId)) {
            return ApiResponse.error("nationalIdNumber already exists.");
        }

        String passport = blankToNull(input.passportNumber());
        if (passport != null && patientRepository.existsByPassportNumber(passport)) {
            return ApiResponse.error("passportNumber already exists.");
        }

        // Create and save patient (guarded: a duplicate nationalId/passport that slips
        // past the pre-checks under concurrency must surface as a clean error, not a 500).
        Patient patient = new Patient();
        patient.setPatientIdentifier(generateUniquePatientIdentifier(input));
        applyPatientInput(patient, input);
        Patient savedPatient;
        try {
            // saveAndFlush (not save): the unique index on nationalId/passport is only
            // checked at flush time — a plain save() defers the violation to a later
            // auto-flush query/commit, so this catch would never fire.
            savedPatient = patientRepository.saveAndFlush(patient);
        } catch (org.springframework.dao.DataIntegrityViolationException ex) {
            org.springframework.transaction.interceptor.TransactionAspectSupport
                .currentTransactionStatus()
                .setRollbackOnly();
            return ApiResponse.error(mapPatientPersistenceError(ex));
        }

        // Handle insurance if provided. On failure the whole createPatient must roll
        // back (patient + insurances + visit are one atomic unit) — otherwise a failed
        // request would commit an orphan patient with no visit.
        List<UUID> linkedInsuranceIds = new ArrayList<>();
        if (input.insurances() != null && !input.insurances().isEmpty()) {
            ApiResponse<List<PatientInsuranceDto>> insuranceResponse = validateAndCreateInsurances(savedPatient, input.insurances());
            if (insuranceResponse.status() != ResponseStatus.SUCCESS) {
                org.springframework.transaction.interceptor.TransactionAspectSupport
                    .currentTransactionStatus()
                    .setRollbackOnly();
                return ApiResponse.error(insuranceResponse.message());
            }
            // Link the just-created insurances to the visit so billing can apply them.
            if (insuranceResponse.data() != null) {
                for (PatientInsuranceDto insuranceDto : insuranceResponse.data()) {
                    if (insuranceDto != null && insuranceDto.id() != null) {
                        linkedInsuranceIds.add(insuranceDto.id());
                    }
                }
            }
        }

        // Index AFTER insurance creation so the document carries the insurance provider ids.
        meilisearchIndexService.indexPatient(savedPatient.getId());

        // Create visit
        CreateVisitInput visitInput = new CreateVisitInput(
                savedPatient.getId(),
                java.time.LocalDateTime.now(),
                linkedInsuranceIds.isEmpty() ? null : linkedInsuranceIds,
                null
        );

        ApiResponse<VisitDto> visitResponse = visitService.createVisit(visitInput, null);
        if (visitResponse.status() != ResponseStatus.SUCCESS) {
            // The patient (and insurances) were already saved in this transaction;
            // roll back so a failed visit never leaves an orphan patient behind.
            org.springframework.transaction.interceptor.TransactionAspectSupport
                .currentTransactionStatus()
                .setRollbackOnly();
        }
        return visitResponse;
    }

    // =========================
    // GET PATIENT
    // =========================
    @Transactional(readOnly = true)
    public ApiResponse<PatientDto> patient(UUID patientId) {
        if (patientId == null) {
            return ApiResponse.error("patientId is required.");
        }

        return patientRepository.findById(patientId)
                .map(p -> ApiResponse.success("Patient fetched.", mapToDto(p)))
                .orElseGet(() -> ApiResponse.error("Patient not found."));
    }

    // =========================
    // GET ALL PATIENTS
    // =========================
    @Transactional(readOnly = true)
    public ApiResponse<List<PatientDto>> patients() {
        List<PatientDto> dtos = mapToDtos(patientRepository.findAll());
        return ApiResponse.success("Patients fetched.", dtos);
    }

    // =========================
    // UPDATE PATIENT
    // =========================
    @Transactional
    public ApiResponse<PatientDto> updatePatient(UUID patientId, UpdatePatientInput input) {
        if (patientId == null || input == null) {
            return ApiResponse.error("patientId and input are required.");
        }

        Patient patient = patientRepository.findById(patientId)
                .orElseGet(() -> null);
        if (patient == null) {
            return ApiResponse.error("Patient not found.");
        }

        // Update fields if present
        if (input.firstName() != null) {
            String firstName = requiredTrim(input.firstName());
            if (firstName == null) {
                return ApiResponse.error("firstName cannot be blank.");
            }
            patient.setFirstName(firstName);
        }

        if (input.middleName() != null) {
            patient.setMiddleName(blankToNull(input.middleName()));
        }

        if (input.lastName() != null) {
            String lastName = requiredTrim(input.lastName());
            if (lastName == null) {
                return ApiResponse.error("lastName cannot be blank when provided.");
            }
            patient.setLastName(lastName);
        }

        if (input.dateOfBirth() != null) {
            patient.setDateOfBirth(input.dateOfBirth());
        }

        if (input.gender() != null) {
            patient.setGender(input.gender());
        }

        if (input.primaryPhoneNumber() != null) {
            patient.setPrimaryPhoneNumber(blankToNull(input.primaryPhoneNumber()));
        }

        if (input.alternativePhone() != null) {
            patient.setAlternativePhone(blankToNull(input.alternativePhone()));
        }

        if (input.village() != null) {
            patient.setVillage(blankToNull(input.village()));
        }

        if (input.cell() != null) {
            patient.setCell(blankToNull(input.cell()));
        }

        if (input.city() != null) {
            patient.setCity(blankToNull(input.city()));
        }

        if (input.district() != null) {
            patient.setDistrict(blankToNull(input.district()));
        }

        if (input.postalAddress() != null) {
            patient.setPostalAddress(blankToNull(input.postalAddress()));
        }

        if (input.nationalIdNumber() != null) {
            String nationalId = blankToNull(input.nationalIdNumber());
            if (nationalId != null && patientRepository.existsByNationalIdNumberAndIdNot(nationalId, patient.getId())) {
                return ApiResponse.error("nationalIdNumber already exists.");
            }
            patient.setNationalIdNumber(nationalId);
        }

        if (input.passportNumber() != null) {
            String passport = blankToNull(input.passportNumber());
            if (passport != null && patientRepository.existsByPassportNumberAndIdNot(passport, patient.getId())) {
                return ApiResponse.error("passportNumber already exists.");
            }
            patient.setPassportNumber(passport);
        }

        if (input.emergencyContactName() != null) {
            patient.setEmergencyContactName(blankToNull(input.emergencyContactName()));
        }

        if (input.emergencyContactRelationship() != null) {
            patient.setEmergencyContactRelationship(blankToNull(input.emergencyContactRelationship()));
        }

        if (input.emergencyContactPhoneNumber() != null) {
            patient.setEmergencyContactPhoneNumber(blankToNull(input.emergencyContactPhoneNumber()));
        }

        // Update full name
        patient.setFullName(buildFullName(
                patient.getFirstName(),
                patient.getMiddleName(),
                patient.getLastName()
        ));

        Patient saved = patientRepository.save(patient);
        meilisearchIndexService.indexPatient(saved);
        return ApiResponse.success("Patient updated.", mapToDto(saved));
    }

    // =========================
    // DELETE PATIENT
    // =========================
    @Transactional
    public ApiResponse<Boolean> deletePatient(UUID patientId) {
        if (patientId == null) {
            return ApiResponse.error("patientId is required.");
        }

        if (!patientRepository.existsById(patientId)) {
            return ApiResponse.error("Patient not found.");
        }

        // FK guard: a patient with visits (or visit-insurances referencing their
        // insurances) cannot be hard-deleted — the FK would throw DataIntegrityViolation.
        if (visitRepository.existsByPatientId(patientId)) {
            return ApiResponse.error(
                "Patient has visit history and cannot be deleted."
            );
        }

        // Delete associated insurances first (guarded: insurances linked to a visit
        // cannot be removed either).
        List<PatientInsurance> patientInsurances = patientInsuranceRepository.findByPatientId(patientId);
        for (PatientInsurance pi : patientInsurances) {
            if (visitInsuranceRepository.existsByPatientInsuranceId(pi.getId())) {
                return ApiResponse.error(
                    "Patient has insurances linked to visits and cannot be deleted."
                );
            }
        }
        if (!patientInsurances.isEmpty()) {
            patientInsuranceRepository.deleteAll(patientInsurances);
        }

        try {
            patientRepository.deleteById(patientId);
        } catch (org.springframework.dao.DataIntegrityViolationException ex) {
            return ApiResponse.error(
                "Patient has records that prevent deletion. Reassign or archive the patient's history first."
            );
        }
        meilisearchIndexService.deletePatient(patientId);
        return ApiResponse.success("Patient deleted.", true);
    }

    // =========================
    // PATIENT INSURANCE MANAGEMENT
    // =========================

    @Transactional
    public ApiResponse<PatientInsuranceDto> createPatientInsurance(CreatePatientInsuranceInput input) {
        if (input == null || input.patientId() == null || input.insuranceProviderId() == null) {
            return ApiResponse.error("patientId, insuranceProviderId and input are required.");
        }

        // Validate patient exists
        Optional<Patient> patientOptional = patientRepository.findById(input.patientId());
        if (patientOptional.isEmpty()) {
            return ApiResponse.error("Patient not found.");
        }

        // Validate insurance provider exists and is supported
        Optional<InsuranceProvider> providerOptional = insuranceProviderRepository.findById(input.insuranceProviderId());
        if (providerOptional.isEmpty()) {
            return ApiResponse.error("Insurance provider not found.");
        }

        InsuranceProvider provider = providerOptional.get();
        if (!provider.isSupportedByClinic()) {
            return ApiResponse.error("Insurance provider is not currently supported by clinic.");
        }

        Patient patient = patientOptional.get();
        PatientInsurance patientInsurance = new PatientInsurance();
        patientInsurance.setPatient(patient);
        patientInsurance.setInsuranceProvider(provider);

        // Apply input with validation
        ApiResponse<Void> validationError = applyPatientInsuranceInput(
                patientInsurance,
                input.insuranceCardNumber(),
                input.providingCompanyOrEmployer(),
                input.principalMember(),
                input.principalMemberName(),
                input.principalMemberPhoneNumber(),
                input.validFrom(),
                input.validUntil(),
                true
        );
        if (validationError != null) {
            return ApiResponse.error(validationError.message());
        }

        // Validate business rules
        ApiResponse<Void> businessRuleError = validateInsuranceBusinessRules(patient, patientInsurance);
        if (businessRuleError != null) {
            return ApiResponse.error(businessRuleError.message());
        }

        // One active policy per provider + one card number per patient.
        ApiResponse<Void> duplicateError = checkInsuranceDuplicates(
                patientInsuranceRepository.findByPatientId(patient.getId()),
                patientInsurance
        );
        if (duplicateError != null) {
            return ApiResponse.error(duplicateError.message());
        }

        PatientInsurance saved = patientInsuranceRepository.save(patientInsurance);
        meilisearchIndexService.indexPatient(patient.getId());
        return ApiResponse.success("Patient insurance added.", patientInsuranceMapper.toDto(saved));
    }

    @Transactional
    public ApiResponse<PatientInsuranceDto> updatePatientInsurance(UUID patientInsuranceId, UpdatePatientInsuranceInput input) {
        if (patientInsuranceId == null || input == null) {
            return ApiResponse.error("patientInsuranceId and input are required.");
        }

        Optional<PatientInsurance> patientInsuranceOptional = patientInsuranceRepository.findById(patientInsuranceId);
        if (patientInsuranceOptional.isEmpty()) {
            return ApiResponse.error("Patient insurance record not found.");
        }

        PatientInsurance patientInsurance = patientInsuranceOptional.get();

        // Update patient if provided
        if (input.patientId() != null) {
            Optional<Patient> patientOptional = patientRepository.findById(input.patientId());
            if (patientOptional.isEmpty()) {
                return ApiResponse.error("Patient not found.");
            }
            // A used insurance is part of the original patient's financial/visit audit
            // trail; moving it to another patient would orphan those references.
            if (!patientOptional.get().getId().equals(patientInsurance.getPatient().getId())
                    && isInsuranceInUse(patientInsuranceId)) {
                return ApiResponse.error(
                    "This insurance is in use and cannot be moved to another patient."
                );
            }
            patientInsurance.setPatient(patientOptional.get());
        }

        // Update insurance provider if provided
        if (input.insuranceProviderId() != null) {
            Optional<InsuranceProvider> providerOptional = insuranceProviderRepository.findById(input.insuranceProviderId());
            if (providerOptional.isEmpty()) {
                return ApiResponse.error("Insurance provider not found.");
            }
            InsuranceProvider provider = providerOptional.get();
            if (!provider.isSupportedByClinic()) {
                return ApiResponse.error("Insurance provider is not currently supported by clinic.");
            }
            patientInsurance.setInsuranceProvider(provider);
        }

        // Apply input with validation (createMode = false for update)
        ApiResponse<Void> validationError = applyPatientInsuranceInput(
                patientInsurance,
                input.insuranceCardNumber(),
                input.providingCompanyOrEmployer(),
                input.principalMember(),
                input.principalMemberName(),
                input.principalMemberPhoneNumber(),
                input.validFrom(),
                input.validUntil(),
                false
        );
        if (validationError != null) {
            // applyPatientInsuranceInput mutates the managed entity before returning;
            // an error ApiResponse does not roll back by itself, so mark the
            // transaction rollback-only to avoid committing the partial mutation.
            org.springframework.transaction.interceptor.TransactionAspectSupport
                .currentTransactionStatus()
                .setRollbackOnly();
            return ApiResponse.error(validationError.message());
        }

        // Validate business rules
        ApiResponse<Void> businessRuleError = validateInsuranceBusinessRules(patientInsurance.getPatient(), patientInsurance);
        if (businessRuleError != null) {
            org.springframework.transaction.interceptor.TransactionAspectSupport
                .currentTransactionStatus()
                .setRollbackOnly();
            return ApiResponse.error(businessRuleError.message());
        }

        // One active policy per provider + one card number per patient. The record
        // itself (same id) is excluded; the effective patient is the possibly-changed one.
        ApiResponse<Void> duplicateError = checkInsuranceDuplicates(
                patientInsuranceRepository.findByPatientId(patientInsurance.getPatient().getId()),
                patientInsurance
        );
        if (duplicateError != null) {
            org.springframework.transaction.interceptor.TransactionAspectSupport
                .currentTransactionStatus()
                .setRollbackOnly();
            return ApiResponse.error(duplicateError.message());
        }

        PatientInsurance saved = patientInsuranceRepository.save(patientInsurance);
        meilisearchIndexService.indexPatient(patientInsurance.getPatient().getId());
        return ApiResponse.success("Patient insurance updated.", patientInsuranceMapper.toDto(saved));
    }

    @Transactional
    public ApiResponse<Boolean> deletePatientInsurance(UUID patientInsuranceId) {
        if (patientInsuranceId == null) {
            return ApiResponse.error("patientInsuranceId is required.");
        }

        Optional<PatientInsurance> patientInsuranceOptional = patientInsuranceRepository.findById(patientInsuranceId);
        if (patientInsuranceOptional.isEmpty()) {
            return ApiResponse.error("Patient insurance not found.");
        }

        UUID patientId = patientInsuranceOptional.get().getPatient().getId();

        // A used insurance (linked to a visit or applied to a billing line) keeps an
        // audit/financial trail pointing at it, so it can never be hard-deleted.
        // Deactivate it instead; it stays visible in the patient's list and remains
        // editable, but can no longer be applied to new bills or linked to visits.
        if (isInsuranceInUse(patientInsuranceId)) {
            PatientInsurance insurance = patientInsuranceOptional.get();
            insurance.setDeactivated(true);
            patientInsuranceRepository.save(insurance);
            meilisearchIndexService.indexPatient(patientId);
            return ApiResponse.success(
                "This insurance is in use and cannot be deleted. It has been deactivated instead.",
                true
            );
        }

        patientInsuranceRepository.deleteById(patientInsuranceId);
        meilisearchIndexService.indexPatient(patientId);
        return ApiResponse.success("Patient insurance deleted.", true);
    }

    @Transactional(readOnly = true)
    public ApiResponse<List<PatientInsuranceDto>> patientInsurances(UUID patientId) {
        if (patientId == null) {
            return ApiResponse.error("patientId is required.");
        }

        if (!patientRepository.existsById(patientId)) {
            return ApiResponse.error("Patient not found.");
        }

        List<PatientInsuranceDto> insurances = patientInsuranceRepository.findByPatientId(patientId)
                .stream()
                .map(patientInsuranceMapper::toDto)
                .toList();

        return ApiResponse.success("Patient insurances fetched.", insurances);
    }

    // =========================
    // PRIVATE HELPER METHODS
    // =========================

    private ApiResponse<Void> applyPatientInsuranceInput(
            PatientInsurance patientInsurance,
            String insuranceCardNumber,
            String providingCompanyOrEmployer,
            Boolean principalMember,
            String principalMemberName,
            String principalMemberPhoneNumber,
            LocalDate validFrom,
            LocalDate validUntil,
            boolean createMode
    ) {
        if (createMode) {
            if (insuranceCardNumber == null || insuranceCardNumber.isBlank()
                    || principalMember == null
                    || validFrom == null
                    || validUntil == null) {
                return ApiResponse.error(
                        "insuranceCardNumber, principalMember, validFrom and validUntil are required."
                );
            }
        }

        if (insuranceCardNumber != null) {
            if (insuranceCardNumber.isBlank()) {
                return ApiResponse.error("insuranceCardNumber cannot be blank.");
            }
            patientInsurance.setInsuranceCardNumber(insuranceCardNumber.trim());
        }

        if (providingCompanyOrEmployer != null) {
            patientInsurance.setProvidingCompanyOrEmployer(blankToNull(providingCompanyOrEmployer));
        }

        if (principalMember != null) {
            patientInsurance.setPrincipalMember(principalMember);
        }

        if (principalMemberName != null) {
            patientInsurance.setPrincipalMemberName(blankToNull(principalMemberName));
        }

        if (principalMemberPhoneNumber != null) {
            patientInsurance.setPrincipalMemberPhoneNumber(blankToNull(principalMemberPhoneNumber));
        }

        if (validFrom != null) {
            patientInsurance.setValidFrom(validFrom);
        }

        if (validUntil != null) {
            patientInsurance.setValidUntil(validUntil);
        }

        // Validate date range
        if (patientInsurance.getValidFrom() != null
                && patientInsurance.getValidUntil() != null
                && patientInsurance.getValidFrom().isAfter(patientInsurance.getValidUntil())) {
            return ApiResponse.error("validFrom cannot be after validUntil.");
        }

        // Validate dependent requirements
        if (!patientInsurance.isPrincipalMember()) {
            if (patientInsurance.getPrincipalMemberName() == null ||
                    patientInsurance.getPrincipalMemberPhoneNumber() == null) {
                return ApiResponse.error(
                        "principalMemberName and principalMemberPhoneNumber are required for dependent members."
                );
            }
            if (patientInsurance.getProvidingCompanyOrEmployer() == null ||
                    patientInsurance.getProvidingCompanyOrEmployer().isBlank()) {
                return ApiResponse.error(
                        "providingCompanyOrEmployer is required for dependent members."
                );
            }
        }

        // Validate insurance is not expired
        if (patientInsurance.getValidUntil() != null &&
                patientInsurance.getValidUntil().isBefore(LocalDate.now())) {
            return ApiResponse.error("Insurance validity has already expired.");
        }

        return null;
    }

    private ApiResponse<Void> validateInsuranceBusinessRules(Patient patient, PatientInsurance patientInsurance) {
        if (patient.getDateOfBirth() != null) {
            int age = Period.between(patient.getDateOfBirth(), LocalDate.now()).getYears();
            if (age < 18 && patientInsurance.isPrincipalMember()) {
                return ApiResponse.error("A patient under 18 years old cannot be a principal member.");
            }
        }
        return null;
    }

    /**
     * Rejects a candidate policy against the patient's other policies:
     * <ul>
     *   <li>a duplicate card number (case-insensitive) is never allowed, and</li>
     *   <li>a patient cannot hold two non-deactivated policies from the same
     *       provider whose validity windows overlap (one active policy per provider).</li>
     * </ul>
     * The candidate itself (matched by id) is skipped, so this also works for updates.
     */
    private ApiResponse<Void> checkInsuranceDuplicates(
            List<PatientInsurance> patientInsurances,
            PatientInsurance candidate) {
        if (patientInsurances == null) {
            return null;
        }

        UUID candidateId = candidate.getId();
        String candidateCard = candidate.getInsuranceCardNumber();
        for (PatientInsurance pi : patientInsurances) {
            if (candidateId != null && candidateId.equals(pi.getId())) {
                continue;
            }
            if (candidateCard != null && pi.getInsuranceCardNumber() != null
                    && candidateCard.equalsIgnoreCase(pi.getInsuranceCardNumber().trim())) {
                return ApiResponse.error(
                    "An insurance with this card number already exists for this patient."
                );
            }
        }

        for (PatientInsurance pi : patientInsurances) {
            if (candidateId != null && candidateId.equals(pi.getId())) {
                continue;
            }
            if (pi.isDeactivated()) {
                continue;
            }
            if (!pi.getInsuranceProvider().getId().equals(candidate.getInsuranceProvider().getId())) {
                continue;
            }
            if (validityOverlaps(
                    candidate.getValidFrom(),
                    candidate.getValidUntil(),
                    pi.getValidFrom(),
                    pi.getValidUntil()
            )) {
                return ApiResponse.error(
                    "This patient already has an active insurance from the same provider. "
                    + "Deactivate the existing one before adding another."
                );
            }
        }

        return null;
    }

    private boolean validityOverlaps(
            LocalDate fromA, LocalDate untilA,
            LocalDate fromB, LocalDate untilB) {
        if (fromA == null || untilA == null || fromB == null || untilB == null) {
            return false;
        }
        return !fromA.isAfter(untilB) && !fromB.isAfter(untilA);
    }

    /**
     * Whether an insurance is referenced by any visit link, billing line or
     * generated invoice — once referenced it must never be hard-deleted, only
     * deactivated.
     */
    private boolean isInsuranceInUse(UUID patientInsuranceId) {
        return visitInsuranceRepository.existsByPatientInsuranceId(patientInsuranceId)
                || visitBillingItemRepository.existsByAppliedPatientInsuranceId(patientInsuranceId)
                || departmentInsuranceBillingRepository
                       .existsByPatientInsuranceIdAndInvoiceUrlIsNotNull(patientInsuranceId);
    }

    private ApiResponse<List<PatientInsuranceDto>> validateAndCreateInsurances(
            Patient patient,
            List<CreatePatientInsuranceInput> insurances) {

        List<PatientInsuranceDto> createdInsurances = new ArrayList<>();
        // Existing policies plus the ones created earlier in this payload; every
        // candidate is checked against all of them so duplicate card numbers and
        // overlapping same-provider policies are rejected even within one request.
        List<PatientInsurance> known = new ArrayList<>(
                patientInsuranceRepository.findByPatientId(patient.getId())
        );

        for (CreatePatientInsuranceInput input : insurances) {
            if (input == null || input.insuranceProviderId() == null) {
                return ApiResponse.error("insuranceProviderId is required for patient insurance.");
            }

            Optional<InsuranceProvider> providerOptional = insuranceProviderRepository.findById(input.insuranceProviderId());
            if (providerOptional.isEmpty()) {
                return ApiResponse.error("Insurance provider not found.");
            }

            InsuranceProvider provider = providerOptional.get();
            if (!provider.isSupportedByClinic()) {
                return ApiResponse.error("Insurance provider is not currently supported by clinic.");
            }

            PatientInsurance patientInsurance = new PatientInsurance();
            patientInsurance.setPatient(patient);
            patientInsurance.setInsuranceProvider(provider);

            ApiResponse<Void> validationError = applyPatientInsuranceInput(
                    patientInsurance,
                    input.insuranceCardNumber(),
                    input.providingCompanyOrEmployer(),
                    input.principalMember(),
                    input.principalMemberName(),
                    input.principalMemberPhoneNumber(),
                    input.validFrom(),
                    input.validUntil(),
                    true
            );
            if (validationError != null) {
                return ApiResponse.error(validationError.message());
            }

            ApiResponse<Void> businessRuleError = validateInsuranceBusinessRules(patient, patientInsurance);
            if (businessRuleError != null) {
                return ApiResponse.error(businessRuleError.message());
            }

            ApiResponse<Void> duplicateError = checkInsuranceDuplicates(known, patientInsurance);
            if (duplicateError != null) {
                return ApiResponse.error(duplicateError.message());
            }

            PatientInsurance saved = patientInsuranceRepository.save(patientInsurance);
            known.add(saved);
            createdInsurances.add(patientInsuranceMapper.toDto(saved));
        }

        return ApiResponse.success("Insurances validated and created.", createdInsurances);
    }

    private String generateUniquePatientIdentifier(CreatePatientInput input) {
        String firstName = requiredTrim(input.firstName());
        String lastName = blankToNull(input.lastName());
        int birthMonth = input.dateOfBirth() == null ? LocalDate.now().getMonthValue() : input.dateOfBirth().getMonthValue();

        // Use only 1 char from each name to keep it short
        String seed = "" + toDigit(firstName, 0) + toDigit(lastName, 0);
        String monthPart = String.format("%01d", birthMonth); // Single digit month

        for (int attempt = 0; attempt < 100; attempt++) {
            int suffix = java.util.concurrent.ThreadLocalRandom.current().nextInt(0, 100);
            String identifier = seed + monthPart + String.format("%02d", suffix);
            // This gives: 2 + 1 + 2 = 5 characters
            // or add a 6th char: use 3 from seed
            if (!patientRepository.existsByPatientIdentifier(identifier)) {
                return identifier;
            }
        }

        // Fallback: generate exactly 6 digits
        for (int attempt = 0; attempt < 1000; attempt++) {
            String identifier = String.format("%06d",
                    java.util.concurrent.ThreadLocalRandom.current().nextInt(0, 1_000_000));
            if (!patientRepository.existsByPatientIdentifier(identifier)) {
                return identifier;
            }
        }

        // Absolute last resort: a short random UUID fragment. Never throw here — a
        // patient must be creatable even under extreme identifier pressure. Existence-check
        // the fragment so the eventual save does not collide.
        for (int attempt = 0; attempt < 100; attempt++) {
            String identifier = java.util.UUID.randomUUID().toString().substring(0, 8).toUpperCase();
            if (!patientRepository.existsByPatientIdentifier(identifier)) {
                return identifier;
            }
        }
        return java.util.UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    private int toDigit(String value, int index) {
        if (value == null || value.isBlank() || index >= value.trim().length()) {
            return 0;
        }
        char c = Character.toUpperCase(value.trim().charAt(index));
        if (c < 'A' || c > 'Z') {
            return 0;
        }
        return ((c - 'A') % 10);
    }

    private void applyPatientInput(Patient patient, CreatePatientInput input) {
        patient.setFirstName(input.firstName().trim());
        patient.setMiddleName(blankToNull(input.middleName()));
        patient.setLastName(blankToNull(input.lastName()));
        patient.setFullName(buildFullName(
                patient.getFirstName(),
                patient.getMiddleName(),
                patient.getLastName()
        ));
        patient.setDateOfBirth(input.dateOfBirth());
        patient.setGender(input.gender());
        patient.setPrimaryPhoneNumber(blankToNull(input.primaryPhoneNumber()));
        patient.setAlternativePhone(blankToNull(input.alternativePhone()));
        patient.setVillage(blankToNull(input.village()));
        patient.setCell(blankToNull(input.cell()));
        patient.setCity(blankToNull(input.city()));
        patient.setDistrict(blankToNull(input.district()));
        patient.setPostalAddress(blankToNull(input.postalAddress()));
        patient.setNationalIdNumber(blankToNull(input.nationalIdNumber()));
        patient.setPassportNumber(blankToNull(input.passportNumber()));
        patient.setEmergencyContactName(blankToNull(input.emergencyContactName()));
        patient.setEmergencyContactRelationship(blankToNull(input.emergencyContactRelationship()));
        patient.setEmergencyContactPhoneNumber(blankToNull(input.emergencyContactPhoneNumber()));
    }

    private PatientDto mapToDto(Patient patient) {
        List<PatientInsurance> insurances = patientInsuranceRepository.findByPatientId(patient.getId());
        return patientMapper.toDto(patient, insurances);
    }

    /**
     * Maps many patients with a SINGLE insurance lookup (N+1 avoidance): loads all
     * insurances for the given patients in one query and groups them by patient.
     */
    private List<PatientDto> mapToDtos(List<Patient> patients) {
        if (patients == null || patients.isEmpty()) {
            return List.of();
        }
        Set<UUID> patientIds = patients.stream()
                .map(Patient::getId)
                .collect(java.util.stream.Collectors.toSet());
        Map<UUID, List<PatientInsurance>> insurancesByPatient =
                patientInsuranceRepository.findByPatientIdIn(patientIds)
                        .stream()
                        .collect(java.util.stream.Collectors.groupingBy(
                                pi -> pi.getPatient().getId()
                        ));
        return patients.stream()
                .map(p -> patientMapper.toDto(
                        p,
                        insurancesByPatient.getOrDefault(p.getId(), List.of())
                ))
                .toList();
    }

    private String mapPatientPersistenceError(org.springframework.dao.DataIntegrityViolationException ex) {
        String lowered = ex.getMessage() == null ? "" : ex.getMessage().toLowerCase();
        if (lowered.contains("national") || lowered.contains("patients_national_id_number_key")) {
            return "nationalIdNumber already exists.";
        }
        if (lowered.contains("passport") || lowered.contains("patients_passport_number_key")) {
            return "passportNumber already exists.";
        }
        if (lowered.contains("identifier") || lowered.contains("patients_patient_identifier_key")) {
            return "Unable to generate a unique patient identifier. Please retry.";
        }
        return "Unable to save the patient due to invalid or duplicate data.";
    }

    private String buildFullName(String first, String middle, String last) {
        String firstTrim = first.trim();
        String middleTrim = blankToNull(middle);
        String lastTrim = blankToNull(last);

        if (middleTrim == null && lastTrim == null) {
            return firstTrim;
        }
        if (middleTrim == null) {
            return firstTrim + " " + lastTrim;
        }
        if (lastTrim == null) {
            return firstTrim + " " + middleTrim;
        }
        return firstTrim + " " + middleTrim + " " + lastTrim;
    }

    private String blankToNull(String value) {
        return (value == null || value.isBlank()) ? null : value.trim();
    }

    private String requiredTrim(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private int normalizePage(Integer page) {
        return (page == null || page < 0) ? 0 : page;
    }

    private int normalizeSize(Integer size) {
        return (size == null || size <= 0) ? 20 : Math.min(size, 100);
    }
}
