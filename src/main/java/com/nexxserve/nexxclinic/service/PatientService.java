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
import com.nexxserve.nexxclinic.repository.InsuranceProviderRepository;
import com.nexxserve.nexxclinic.repository.PatientInsuranceRepository;
import com.nexxserve.nexxclinic.repository.PatientRepository;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.Period;
import java.util.*;

@Service
public class PatientService {

    private final PatientRepository patientRepository;
    private final PatientMapper patientMapper;
    private final PatientInsuranceMapper patientInsuranceMapper;
    private final PatientInsuranceRepository patientInsuranceRepository;
    private final InsuranceProviderRepository insuranceProviderRepository;
    private final VisitService visitService;

    public PatientService(
            PatientRepository patientRepository,
            PatientInsuranceRepository patientInsuranceRepository,
            InsuranceProviderRepository insuranceProviderRepository,
            PatientMapper patientMapper,
            PatientInsuranceMapper patientInsuranceMapper,
            VisitService visitService
    ) {
        this.patientRepository = patientRepository;
        this.patientInsuranceRepository = patientInsuranceRepository;
        this.insuranceProviderRepository = insuranceProviderRepository;
        this.patientMapper = patientMapper;
        this.patientInsuranceMapper = patientInsuranceMapper;
        this.visitService = visitService;
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

        Specification<Patient> spec = (root, query, cb) -> cb.conjunction();

        // Insurance provider filter
        UUID insuranceProviderId = input == null ? null : input.insuranceProviderId();
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
        String phoneNumber = input == null ? null : blankToNull(input.phoneNumber());
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
        String name = input == null ? null : blankToNull(input.name());
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
        List<PatientDto> dtos = patientPage.getContent().stream().map(this::mapToDto).toList();

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

        // Create and save patient
        Patient patient = new Patient();
        patient.setPatientIdentifier(generateUniquePatientIdentifier(input));
        applyPatientInput(patient, input);
        Patient savedPatient = patientRepository.save(patient);

        // Handle insurance if provided
        List<UUID> linkedInsuranceIds = new ArrayList<>();
        if (input.insurances() != null && !input.insurances().isEmpty()) {
            ApiResponse<List<PatientInsuranceDto>> insuranceResponse = validateAndCreateInsurances(savedPatient, input.insurances());
            if (insuranceResponse.status() != ResponseStatus.SUCCESS) {
                return ApiResponse.error(insuranceResponse.message());
            }
            // Extract insurance IDs from the response
            if (insuranceResponse.data() != null) {
                for (PatientInsuranceDto insuranceDto : insuranceResponse.data()) {
                    // We need to fetch the actual IDs since we don't have them in the DTO
                    // Alternative: have createPatientInsurance return the entity
                    // For now, we'll handle this differently
                }
            }
        }

        // Create visit
        CreateVisitInput visitInput = new CreateVisitInput(
                savedPatient.getId(),
                java.time.LocalDateTime.now(),
                linkedInsuranceIds.isEmpty() ? null : linkedInsuranceIds,
                null
        );

        ApiResponse<VisitDto> visitResponse = visitService.createVisit(visitInput, null);
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
        List<PatientDto> dtos = patientRepository.findAll()
                .stream()
                .map(this::mapToDto)
                .toList();
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
                .orElseThrow(() -> new RuntimeException("Patient not found"));

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

        // Delete associated insurances first
        List<PatientInsurance> patientInsurances = patientInsuranceRepository.findByPatientId(patientId);
        if (!patientInsurances.isEmpty()) {
            patientInsuranceRepository.deleteAll(patientInsurances);
        }

        patientRepository.deleteById(patientId);
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

        PatientInsurance saved = patientInsuranceRepository.save(patientInsurance);
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
            return ApiResponse.error(validationError.message());
        }

        // Validate business rules
        ApiResponse<Void> businessRuleError = validateInsuranceBusinessRules(patientInsurance.getPatient(), patientInsurance);
        if (businessRuleError != null) {
            return ApiResponse.error(businessRuleError.message());
        }

        PatientInsurance saved = patientInsuranceRepository.save(patientInsurance);
        return ApiResponse.success("Patient insurance updated.", patientInsuranceMapper.toDto(saved));
    }

    @Transactional
    public ApiResponse<Boolean> deletePatientInsurance(UUID patientInsuranceId) {
        if (patientInsuranceId == null) {
            return ApiResponse.error("patientInsuranceId is required.");
        }

        if (!patientInsuranceRepository.existsById(patientInsuranceId)) {
            return ApiResponse.error("Patient insurance not found.");
        }

        patientInsuranceRepository.deleteById(patientInsuranceId);
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

    private ApiResponse<List<PatientInsuranceDto>> validateAndCreateInsurances(
            Patient patient,
            List<CreatePatientInsuranceInput> insurances) {

        List<PatientInsuranceDto> createdInsurances = new ArrayList<>();

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

            PatientInsurance saved = patientInsuranceRepository.save(patientInsurance);
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

        throw new IllegalStateException("Unable to generate a unique patient identifier.");
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
