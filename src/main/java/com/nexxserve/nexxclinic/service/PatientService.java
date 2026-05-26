package com.nexxserve.nexxclinic.service;

import com.nexxserve.nexxclinic.entity.InsuranceProvider;
import com.nexxserve.nexxclinic.entity.Patient;
import com.nexxserve.nexxclinic.entity.PatientInsurance;
import com.nexxserve.nexxclinic.graphql.input.CreatePatientInput;
import com.nexxserve.nexxclinic.graphql.input.CreatePatientInsuranceInput;
import com.nexxserve.nexxclinic.graphql.input.CreateVisitInput;
import com.nexxserve.nexxclinic.graphql.input.SearchPatientsInput;
import com.nexxserve.nexxclinic.graphql.input.UpdatePatientInput;
import com.nexxserve.nexxclinic.graphql.input.UpdatePatientInsuranceInput;
import com.nexxserve.nexxclinic.model.ApiResponse;
import com.nexxserve.nexxclinic.model.ResponseStatus;
import com.nexxserve.nexxclinic.repository.InsuranceProviderRepository;
import com.nexxserve.nexxclinic.repository.PatientInsuranceRepository;
import com.nexxserve.nexxclinic.repository.PatientRepository;
import java.time.LocalDate;
import java.time.Period;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PatientService {

    private final PatientRepository patientRepository;
    private final PatientInsuranceRepository patientInsuranceRepository;
    private final InsuranceProviderRepository insuranceProviderRepository;
    private final VisitService visitService;

    public PatientService(
            PatientRepository patientRepository,
            PatientInsuranceRepository patientInsuranceRepository,
            InsuranceProviderRepository insuranceProviderRepository,
            VisitService visitService
    ) {
        this.patientRepository = patientRepository;
        this.patientInsuranceRepository = patientInsuranceRepository;
        this.insuranceProviderRepository = insuranceProviderRepository;
        this.visitService = visitService;
    }

    @Transactional(readOnly = true)
    public ApiResponse searchPatients(SearchPatientsInput input) {
        int page = normalizePage(input == null ? null : input.page());
        int size = normalizeSize(input == null ? null : input.size());
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));

        Integer exactAge = input == null ? null : input.age();
        Integer minAge = input == null ? null : input.minAge();
        Integer maxAge = input == null ? null : input.maxAge();

        if (exactAge != null && (minAge != null || maxAge != null)) {
            return ApiResponse.error("Use either age or minAge/maxAge filters, not both.", "VALIDATION_ERROR");
        }

        if (exactAge != null && exactAge < 0) {
            return ApiResponse.error("age must be a positive number.", "VALIDATION_ERROR");
        }

        if (minAge != null && minAge < 0) {
            return ApiResponse.error("minAge must be a positive number.", "VALIDATION_ERROR");
        }

        if (maxAge != null && maxAge < 0) {
            return ApiResponse.error("maxAge must be a positive number.", "VALIDATION_ERROR");
        }

        if (minAge != null && maxAge != null && minAge > maxAge) {
            return ApiResponse.error("minAge cannot be greater than maxAge.", "VALIDATION_ERROR");
        }

        Set<UUID> filteredPatientIds = null;
        UUID insuranceProviderId = input == null ? null : input.insuranceProviderId();
        if (insuranceProviderId != null) {
            if (!insuranceProviderRepository.existsById(insuranceProviderId)) {
                return ApiResponse.error("Insurance provider not found.", "NOT_FOUND");
            }

            filteredPatientIds = patientInsuranceRepository.findByInsuranceProviderId(insuranceProviderId)
                    .stream()
                    .map(patientInsurance -> patientInsurance.getPatient().getId())
                    .collect(java.util.stream.Collectors.toSet());

            if (filteredPatientIds.isEmpty()) {
                return ApiResponse.success(
                        "Patients fetched.",
                    List.of(),
                    Map.of("total", 0, "perPage", size, "currentPage", page, "totalPages", 0)
                );
            }
        }

        Specification<Patient> spec = (root, queryDef, builder) -> builder.conjunction();

        if (filteredPatientIds != null) {
            Set<UUID> patientIds = filteredPatientIds;
            spec = spec.and((root, queryDef, builder) -> root.get("id").in(patientIds));
        }

        String phoneNumber = input == null ? null : blankToNull(input.phoneNumber());
        if (phoneNumber != null) {
            String normalizedPhone = phoneNumber.toLowerCase();
            spec = spec.and((root, queryDef, builder) ->
                    builder.or(
                            builder.like(builder.lower(root.get("primaryPhoneNumber")), "%" + normalizedPhone + "%"),
                            builder.like(builder.lower(root.get("alternativePhone")), "%" + normalizedPhone + "%")
                    )
            );
        }

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
                spec = spec.and((root, queryDef, builder) ->
                        builder.like(builder.lower(root.get("fullName")), "%" + token + "%")
                );
            }
        }

        LocalDate today = LocalDate.now();
        if (exactAge != null) {
            LocalDate dobUpper = today.minusYears(exactAge);
            LocalDate dobLower = today.minusYears(exactAge + 1L).plusDays(1);
            spec = spec.and((root, queryDef, builder) -> builder.between(root.get("dateOfBirth"), dobLower, dobUpper));
        } else {
            if (minAge != null) {
                LocalDate maxDob = today.minusYears(minAge);
                spec = spec.and((root, queryDef, builder) -> builder.lessThanOrEqualTo(root.get("dateOfBirth"), maxDob));
            }
            if (maxAge != null) {
                LocalDate minDob = today.minusYears(maxAge + 1L).plusDays(1);
                spec = spec.and((root, queryDef, builder) -> builder.greaterThanOrEqualTo(root.get("dateOfBirth"), minDob));
            }
        }

        Page<Patient> patientPage = patientRepository.findAll(spec, pageable);
        List<Map<String, Object>> patients = patientPage.getContent().stream().map(this::patientToMap).toList();

        return ApiResponse.success(
                "Patients fetched.",
            patients,
            paginationToMap(patientPage)
        );
    }

    @Transactional
    public ApiResponse createPatient(CreatePatientInput input) {
        if (input == null) {
            return ApiResponse.error("input is required.", "VALIDATION_ERROR");
        }

        String firstName = requiredTrim(input.firstName());
        if (firstName == null) {
            return ApiResponse.error("firstName is required.", "VALIDATION_ERROR");
        }

        if (input.lastName() != null && input.lastName().isBlank()) {
            return ApiResponse.error("lastName cannot be blank when provided.", "VALIDATION_ERROR");
        }

        if (input.dateOfBirth() == null || input.gender() == null) {
            return ApiResponse.error("dateOfBirth and gender are required.", "VALIDATION_ERROR");
        }

        String nationalId = blankToNull(input.nationalIdNumber());
        if (nationalId != null && patientRepository.existsByNationalIdNumber(nationalId)) {
            return ApiResponse.error("nationalIdNumber already exists.", "DUPLICATE_NATIONAL_ID");
        }

        String passport = blankToNull(input.passportNumber());
        if (passport != null && patientRepository.existsByPassportNumber(passport)) {
            return ApiResponse.error("passportNumber already exists.", "DUPLICATE_PASSPORT");
        }

        Patient patient = new Patient();
        applyPatientInput(patient, input.firstName(), input.middleName(), input.lastName(), input.dateOfBirth(), input.gender(),
                input.primaryPhoneNumber(), input.alternativePhone(), input.village(), input.city(), input.district(),
                input.postalAddress(), input.nationalIdNumber(), input.passportNumber(), input.emergencyContactName(),
                input.emergencyContactRelationship(), input.emergencyContactPhoneNumber());

        if (input.insurances() != null && !input.insurances().isEmpty()) {
            ApiResponse insuranceValidation = validatePatientInsuranceInputs(patient, input.insurances());
            if (insuranceValidation.status() != ResponseStatus.SUCCESS) {
                return insuranceValidation;
            }
        }

        Patient saved = patientRepository.save(patient);

        List<UUID> linkedInsuranceIds = new ArrayList<>();
        if (input.insurances() != null && !input.insurances().isEmpty()) {
            for (CreatePatientInsuranceInput insuranceInput : input.insurances()) {
                ApiResponse insuranceResponse = createPatientInsuranceForSavedPatient(saved, insuranceInput);
                if (insuranceResponse.status() != ResponseStatus.SUCCESS) {
                    return insuranceResponse;
                }

                PatientInsurance savedInsurance = (PatientInsurance) insuranceResponse.data();
                linkedInsuranceIds.add(savedInsurance.getId());
            }
        }

        CreateVisitInput visitInput = new CreateVisitInput(
                saved.getId(),
                java.time.LocalDateTime.now(),
                linkedInsuranceIds.isEmpty() ? null : linkedInsuranceIds,
                null
        );

        ApiResponse visitResponse = visitService.createVisit(visitInput, null);
        if (visitResponse.status() != ResponseStatus.SUCCESS) {
            return visitResponse;
        }

        return visitResponse;
    }

    @Transactional
    public ApiResponse updatePatient(UUID patientId, UpdatePatientInput input) {
        if (patientId == null || input == null) {
            return ApiResponse.error("patientId and input are required.", "VALIDATION_ERROR");
        }

        Optional<Patient> patientOptional = patientRepository.findById(patientId);
        if (patientOptional.isEmpty()) {
            return ApiResponse.error("Patient not found.", "NOT_FOUND");
        }

        Patient patient = patientOptional.get();

        if (input.firstName() != null) {
            String firstName = requiredTrim(input.firstName());
            if (firstName == null) {
                return ApiResponse.error("firstName cannot be blank.", "VALIDATION_ERROR");
            }
            patient.setFirstName(firstName);
        }

        if (input.middleName() != null) {
            patient.setMiddleName(blankToNull(input.middleName()));
        }

        if (input.lastName() != null) {
            String lastName = requiredTrim(input.lastName());
            if (lastName == null) {
                return ApiResponse.error("lastName cannot be blank when provided.", "VALIDATION_ERROR");
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
                return ApiResponse.error("nationalIdNumber already exists.", "DUPLICATE_NATIONAL_ID");
            }
            patient.setNationalIdNumber(nationalId);
        }

        if (input.passportNumber() != null) {
            String passport = blankToNull(input.passportNumber());
            if (passport != null && patientRepository.existsByPassportNumberAndIdNot(passport, patient.getId())) {
                return ApiResponse.error("passportNumber already exists.", "DUPLICATE_PASSPORT");
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

        patient.setFullName(buildFullName(patient.getFirstName(), patient.getMiddleName(), patient.getLastName()));

        Patient saved = patientRepository.save(patient);
        return ApiResponse.success("Patient updated.", patientToMap(saved));
    }

    @Transactional
    public ApiResponse deletePatient(UUID patientId) {
        if (patientId == null) {
            return ApiResponse.error("patientId is required.", "VALIDATION_ERROR");
        }

        Optional<Patient> patientOptional = patientRepository.findById(patientId);
        if (patientOptional.isEmpty()) {
            return ApiResponse.error("Patient not found.", "NOT_FOUND");
        }

        List<PatientInsurance> patientInsurances = patientInsuranceRepository.findByPatientId(patientId);
        if (!patientInsurances.isEmpty()) {
            patientInsuranceRepository.deleteAll(patientInsurances);
        }

        patientRepository.deleteById(patientId);
        return ApiResponse.success("Patient deleted.", true);
    }

    @Transactional(readOnly = true)
    public ApiResponse patient(UUID patientId) {
        if (patientId == null) {
            return ApiResponse.error("patientId is required.", "VALIDATION_ERROR");
        }

        Optional<Patient> patientOptional = patientRepository.findById(patientId);
        if (patientOptional.isEmpty()) {
            return ApiResponse.error("Patient not found.", "NOT_FOUND");
        }

        return ApiResponse.success("Patient fetched.", patientToMap(patientOptional.get()));
    }

    @Transactional(readOnly = true)
    public ApiResponse patients() {
        List<Map<String, Object>> patients = patientRepository.findAll().stream().map(this::patientToMap).toList();
        int total = patients.size();
        int totalPages = total == 0 ? 0 : 1;
        return ApiResponse.success(
            "Patients fetched.",
            patients,
            Map.of("total", total, "perPage", total, "currentPage", 0, "totalPages", totalPages)
        );
    }

    @Transactional
    public ApiResponse createPatientInsurance(CreatePatientInsuranceInput input) {
        if (input == null || input.patientId() == null || input.insuranceProviderId() == null) {
            return ApiResponse.error("patientId, insuranceProviderId and input are required.", "VALIDATION_ERROR");
        }

        Optional<Patient> patientOptional = patientRepository.findById(input.patientId());
        if (patientOptional.isEmpty()) {
            return ApiResponse.error("Patient not found.", "NOT_FOUND");
        }

        Optional<InsuranceProvider> providerOptional = insuranceProviderRepository.findById(input.insuranceProviderId());
        if (providerOptional.isEmpty()) {
            return ApiResponse.error("Insurance provider not found.", "NOT_FOUND");
        }

        if (!providerOptional.get().isSupportedByClinic()) {
            return ApiResponse.error("Insurance provider is not currently supported by clinic.", "INSURANCE_NOT_SUPPORTED");
        }

        PatientInsurance patientInsurance = new PatientInsurance();
        patientInsurance.setPatient(patientOptional.get());
        patientInsurance.setInsuranceProvider(providerOptional.get());

        ApiResponse validationError = applyPatientInsuranceInput(
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
            return validationError;
        }

        ApiResponse businessRuleError = validateInsuranceBusinessRules(patientOptional.get(), patientInsurance);
        if (businessRuleError != null) {
            return businessRuleError;
        }

        PatientInsurance saved = patientInsuranceRepository.save(patientInsurance);
        return ApiResponse.success("Patient insurance added.", patientInsuranceToMap(saved));
    }

    private ApiResponse validatePatientInsuranceInputs(Patient patient, List<CreatePatientInsuranceInput> insurances) {
        for (CreatePatientInsuranceInput input : insurances) {
            if (input == null || input.insuranceProviderId() == null) {
                return ApiResponse.error("insuranceProviderId is required for patient insurance.", "VALIDATION_ERROR");
            }

            Optional<InsuranceProvider> providerOptional = insuranceProviderRepository.findById(input.insuranceProviderId());
            if (providerOptional.isEmpty()) {
                return ApiResponse.error("Insurance provider not found.", "NOT_FOUND");
            }

            if (!providerOptional.get().isSupportedByClinic()) {
                return ApiResponse.error("Insurance provider is not currently supported by clinic.", "INSURANCE_NOT_SUPPORTED");
            }

            PatientInsurance patientInsurance = new PatientInsurance();
            patientInsurance.setPatient(patient);
            patientInsurance.setInsuranceProvider(providerOptional.get());

            ApiResponse validationError = applyPatientInsuranceInput(
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
                return validationError;
            }

            ApiResponse businessRuleError = validateInsuranceBusinessRules(patient, patientInsurance);
            if (businessRuleError != null) {
                return businessRuleError;
            }
        }
        return ApiResponse.success("Insurance inputs validated.", null);
    }

    private ApiResponse createPatientInsuranceForSavedPatient(Patient patient, CreatePatientInsuranceInput input) {
        if (input == null || input.insuranceProviderId() == null) {
            return ApiResponse.error("insuranceProviderId is required for patient insurance.", "VALIDATION_ERROR");
        }

        Optional<InsuranceProvider> providerOptional = insuranceProviderRepository.findById(input.insuranceProviderId());
        if (providerOptional.isEmpty()) {
            return ApiResponse.error("Insurance provider not found.", "NOT_FOUND");
        }

        if (!providerOptional.get().isSupportedByClinic()) {
            return ApiResponse.error("Insurance provider is not currently supported by clinic.", "INSURANCE_NOT_SUPPORTED");
        }

        PatientInsurance patientInsurance = new PatientInsurance();
        patientInsurance.setPatient(patient);
        patientInsurance.setInsuranceProvider(providerOptional.get());

        ApiResponse validationError = applyPatientInsuranceInput(
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
            return validationError;
        }

        ApiResponse businessRuleError = validateInsuranceBusinessRules(patient, patientInsurance);
        if (businessRuleError != null) {
            return businessRuleError;
        }

        PatientInsurance saved = patientInsuranceRepository.save(patientInsurance);
        return ApiResponse.success("Patient insurance added.", saved);
    }

    @Transactional
    public ApiResponse updatePatientInsurance(UUID patientInsuranceId, UpdatePatientInsuranceInput input) {
        if (patientInsuranceId == null || input == null) {
            return ApiResponse.error("patientInsuranceId and input are required.", "VALIDATION_ERROR");
        }

        Optional<PatientInsurance> patientInsuranceOptional = patientInsuranceRepository.findById(patientInsuranceId);
        if (patientInsuranceOptional.isEmpty()) {
            return ApiResponse.error("Patient insurance record not found.", "NOT_FOUND");
        }

        PatientInsurance patientInsurance = patientInsuranceOptional.get();

        if (input.patientId() != null) {
            Optional<Patient> patientOptional = patientRepository.findById(input.patientId());
            if (patientOptional.isEmpty()) {
                return ApiResponse.error("Patient not found.", "NOT_FOUND");
            }
            patientInsurance.setPatient(patientOptional.get());
        }

        if (input.insuranceProviderId() != null) {
            Optional<InsuranceProvider> providerOptional = insuranceProviderRepository.findById(input.insuranceProviderId());
            if (providerOptional.isEmpty()) {
                return ApiResponse.error("Insurance provider not found.", "NOT_FOUND");
            }
            if (!providerOptional.get().isSupportedByClinic()) {
                return ApiResponse.error("Insurance provider is not currently supported by clinic.", "INSURANCE_NOT_SUPPORTED");
            }
            patientInsurance.setInsuranceProvider(providerOptional.get());
        }

        ApiResponse validationError = applyPatientInsuranceInput(
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
            return validationError;
        }

        ApiResponse businessRuleError = validateInsuranceBusinessRules(patientInsurance.getPatient(), patientInsurance);
        if (businessRuleError != null) {
            return businessRuleError;
        }

        PatientInsurance saved = patientInsuranceRepository.save(patientInsurance);
        return ApiResponse.success("Patient insurance updated.", patientInsuranceToMap(saved));
    }

    @Transactional
    public ApiResponse deletePatientInsurance(UUID patientInsuranceId) {
        if (patientInsuranceId == null) {
            return ApiResponse.error("patientInsuranceId is required.", "VALIDATION_ERROR");
        }

        if (!patientInsuranceRepository.existsById(patientInsuranceId)) {
            return ApiResponse.error("Patient insurance record not found.", "NOT_FOUND");
        }

        patientInsuranceRepository.deleteById(patientInsuranceId);
        return ApiResponse.success("Patient insurance deleted.", true);
    }

    @Transactional(readOnly = true)
    public ApiResponse patientInsurances(UUID patientId) {
        if (patientId == null) {
            return ApiResponse.error("patientId is required.", "VALIDATION_ERROR");
        }

        if (!patientRepository.existsById(patientId)) {
            return ApiResponse.error("Patient not found.", "NOT_FOUND");
        }

        List<Map<String, Object>> insurances = patientInsuranceRepository.findByPatientId(patientId)
                .stream()
                .map(this::patientInsuranceToMap)
                .toList();

        int total = insurances.size();
        int totalPages = total == 0 ? 0 : 1;
        return ApiResponse.success(
            "Patient insurances fetched.",
            insurances,
            Map.of("total", total, "perPage", total, "currentPage", 0, "totalPages", totalPages)
        );
    }

    private ApiResponse applyPatientInsuranceInput(
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
                        "insuranceCardNumber, principalMember, validFrom and validUntil are required.",
                        "VALIDATION_ERROR"
                );
            }
        }

        if (insuranceCardNumber != null) {
            if (insuranceCardNumber.isBlank()) {
                return ApiResponse.error("insuranceCardNumber cannot be blank.", "VALIDATION_ERROR");
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

        if (patientInsurance.getValidFrom() != null
                && patientInsurance.getValidUntil() != null
                && patientInsurance.getValidFrom().isAfter(patientInsurance.getValidUntil())) {
            return ApiResponse.error("validFrom cannot be after validUntil.", "VALIDATION_ERROR");
        }

        if (!patientInsurance.isPrincipalMember()) {
            if (patientInsurance.getPrincipalMemberName() == null || patientInsurance.getPrincipalMemberPhoneNumber() == null) {
                return ApiResponse.error(
                        "principalMemberName and principalMemberPhoneNumber are required for dependent members.",
                        "VALIDATION_ERROR"
                );
            }
            if (patientInsurance.getProvidingCompanyOrEmployer() == null || patientInsurance.getProvidingCompanyOrEmployer().isBlank()) {
                return ApiResponse.error(
                        "providingCompanyOrEmployer is required for dependent members.",
                        "VALIDATION_ERROR"
                );
            }
        }

        if (patientInsurance.getValidUntil() != null && patientInsurance.getValidUntil().isBefore(LocalDate.now())) {
            return ApiResponse.error("Insurance validity has already expired.", "INVALID_INSURANCE_VALIDITY");
        }

        return null;
    }

    private ApiResponse validateInsuranceBusinessRules(Patient patient, PatientInsurance patientInsurance) {
        if (patient.getDateOfBirth() != null) {
            int age = Period.between(patient.getDateOfBirth(), LocalDate.now()).getYears();
            if (age < 18 && patientInsurance.isPrincipalMember()) {
                return ApiResponse.error("A patient under 18 years old cannot be a principal member.", "INVALID_PRINCIPAL_MEMBER");
            }
        }

        return null;
    }

    private void applyPatientInput(
            Patient patient,
            String firstName,
            String middleName,
            String lastName,
            LocalDate dateOfBirth,
            com.nexxserve.nexxclinic.model.Gender gender,
            String primaryPhoneNumber,
            String alternativePhone,
            String village,
            String city,
            String district,
            String postalAddress,
            String nationalIdNumber,
            String passportNumber,
            String emergencyContactName,
            String emergencyContactRelationship,
            String emergencyContactPhoneNumber
    ) {
        patient.setFirstName(firstName.trim());
        patient.setMiddleName(blankToNull(middleName));
        patient.setLastName(blankToNull(lastName));
        patient.setFullName(buildFullName(firstName, middleName, lastName));
        patient.setDateOfBirth(dateOfBirth);
        patient.setGender(gender);
        patient.setPrimaryPhoneNumber(blankToNull(primaryPhoneNumber));
        patient.setAlternativePhone(blankToNull(alternativePhone));
        patient.setVillage(blankToNull(village));
        patient.setCity(blankToNull(city));
        patient.setDistrict(blankToNull(district));
        patient.setPostalAddress(blankToNull(postalAddress));
        patient.setNationalIdNumber(blankToNull(nationalIdNumber));
        patient.setPassportNumber(blankToNull(passportNumber));
        patient.setEmergencyContactName(blankToNull(emergencyContactName));
        patient.setEmergencyContactRelationship(blankToNull(emergencyContactRelationship));
        patient.setEmergencyContactPhoneNumber(blankToNull(emergencyContactPhoneNumber));
    }

    private Map<String, Object> patientToMap(Patient patient) {
        Map<String, Object> data = new HashMap<>();
        data.put("id", patient.getId());
        data.put("firstName", patient.getFirstName());
        data.put("middleName", patient.getMiddleName());
        data.put("lastName", patient.getLastName());
        data.put("fullName", patient.getFullName());
        data.put("dateOfBirth", patient.getDateOfBirth());
        data.put("age", patient.getDateOfBirth() == null ? null : Period.between(patient.getDateOfBirth(), LocalDate.now()).getYears());
        data.put("gender", patient.getGender());
        data.put("primaryPhoneNumber", patient.getPrimaryPhoneNumber());
        data.put("alternativePhone", patient.getAlternativePhone());
        data.put("village", patient.getVillage());
        data.put("city", patient.getCity());
        data.put("district", patient.getDistrict());
        data.put("postalAddress", patient.getPostalAddress());
        data.put("nationalIdNumber", patient.getNationalIdNumber());
        data.put("passportNumber", patient.getPassportNumber());
        data.put("emergencyContactName", patient.getEmergencyContactName());
        data.put("emergencyContactRelationship", patient.getEmergencyContactRelationship());
        data.put("emergencyContactPhoneNumber", patient.getEmergencyContactPhoneNumber());
        data.put("patientInsurances", patientInsuranceRepository.findByPatientId(patient.getId()).stream().map(this::patientInsuranceToMap).toList());
        data.put("createdAt", patient.getCreatedAt());
        data.put("updatedAt", patient.getUpdatedAt());
        return data;
    }

    private Map<String, Object> patientInsuranceToMap(PatientInsurance patientInsurance) {
        Map<String, Object> data = new HashMap<>();
        data.put("id", patientInsurance.getId());
        data.put("patientId", patientInsurance.getPatient().getId());
        data.put("insuranceProviderId", patientInsurance.getInsuranceProvider().getId());
        data.put("insuranceName", patientInsurance.getInsuranceProvider().getInsuranceName());
        data.put("insuranceAcronym", patientInsurance.getInsuranceProvider().getAcronym());
        data.put("coveragePercentage", patientInsurance.getInsuranceProvider().getDefaultCoveragePercentage());
        data.put("insuranceCardNumber", patientInsurance.getInsuranceCardNumber());
        data.put("providingCompanyOrEmployer", patientInsurance.getProvidingCompanyOrEmployer());
        data.put("principalMember", patientInsurance.isPrincipalMember());
        data.put("principalMemberName", patientInsurance.getPrincipalMemberName());
        data.put("principalMemberPhoneNumber", patientInsurance.getPrincipalMemberPhoneNumber());
        data.put("validFrom", patientInsurance.getValidFrom());
        data.put("validUntil", patientInsurance.getValidUntil());
        data.put("createdAt", patientInsurance.getCreatedAt());
        data.put("updatedAt", patientInsurance.getUpdatedAt());
        return data;
    }

    private String buildFullName(String firstName, String middleName, String lastName) {
        String first = firstName.trim();
        String middle = blankToNull(middleName);
        String last = blankToNull(lastName);

        if (middle == null && last == null) {
            return first;
        }

        if (middle == null) {
            return first + " " + last;
        }

        if (last == null) {
            return first + " " + middle;
        }

        return first + " " + middle + " " + last;
    }

    private String requiredTrim(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private int normalizePage(Integer page) {
        if (page == null || page < 0) {
            return 0;
        }
        return page;
    }

    private int normalizeSize(Integer size) {
        if (size == null || size <= 0) {
            return 20;
        }
        return Math.min(size, 100);
    }

    private Map<String, Object> paginationToMap(Page<?> page) {
        Map<String, Object> pagination = new HashMap<>();
        pagination.put("total", page.getTotalElements());
        pagination.put("perPage", page.getSize());
        pagination.put("currentPage", page.getNumber());
        pagination.put("totalPages", page.getTotalPages());
        return pagination;
    }

    private String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
