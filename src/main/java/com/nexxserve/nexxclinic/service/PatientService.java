package com.nexxserve.nexxclinic.service;

import com.nexxserve.nexxclinic.dto.out.ApiResponse;
import com.nexxserve.nexxclinic.dto.out.PatientDto;
import com.nexxserve.nexxclinic.entity.*;
import com.nexxserve.nexxclinic.graphql.input.*;
import com.nexxserve.nexxclinic.mappers.out.PatientMapper;
import com.nexxserve.nexxclinic.repository.*;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
public class PatientService {

    private final PatientRepository patientRepository;
    private final PatientMapper patientMapper;
    private final PatientInsuranceRepository patientInsuranceRepository;

    public PatientService(
            PatientRepository patientRepository,
            PatientInsuranceRepository patientInsuranceRepository,
            PatientMapper patientMapper
    ) {
        this.patientRepository = patientRepository;
        this.patientInsuranceRepository = patientInsuranceRepository;
        this.patientMapper = patientMapper;
    }

    // =========================
    // SEARCH PATIENTS
    // =========================
    @Transactional(readOnly = true)
    public ApiResponse<Page<PatientDto>> searchPatients(SearchPatientsInput input) {
        int page = normalizePage(input == null ? null : input.page());
        int size = normalizeSize(input == null ? null : input.size());

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));

        Specification<Patient> spec = (root, query, cb) -> cb.conjunction();

        if (input != null && input.name() != null) {
            String name = input.name().toLowerCase();
            spec = spec.and((root, q, cb) ->
                    cb.like(cb.lower(root.get("fullName")), "%" + name + "%")
            );
        }

        Page<Patient> patientPage = patientRepository.findAll(spec, pageable);

        // Uses the mapper to convert the page of entities to DTOs
        Page<PatientDto> dtoPage = patientPage.map(this::mapToDto);

        return ApiResponse.success("Patients fetched.", dtoPage);
    }

    // =========================
    // CREATE PATIENT
    // =========================
    @Transactional
    public ApiResponse<PatientDto> createPatient(CreatePatientInput input) {
        if (input == null) {
            return ApiResponse.error("input is required.");
        }

        if (input.firstName() == null || input.firstName().isBlank()) {
            return ApiResponse.error("firstName is required.");
        }

        Patient patient = new Patient();
        applyPatientInput(patient, input);
        Patient saved = patientRepository.save(patient);

        return ApiResponse.success("Patient created.", mapToDto(saved));
    }

    // =========================
    // GET PATIENT
    // =========================
    @Transactional(readOnly = true)
    public ApiResponse<PatientDto> patient(UUID patientId) {
        return patientRepository.findById(patientId)
                .map(p -> ApiResponse.success("Patient fetched.", mapToDto(p)))
                .orElseGet(() -> ApiResponse.error("Patient not found."));
    }

    // =========================
    // UPDATE PATIENT
    // =========================
    @Transactional
    public ApiResponse<PatientDto> updatePatient(UUID patientId, UpdatePatientInput input) {
        Patient patient = patientRepository.findById(patientId)
                .orElseThrow(() -> new RuntimeException("Patient not found"));

        if (input.firstName() != null) {
            patient.setFirstName(input.firstName().trim());
        }

        if (input.lastName() != null) {
            patient.setLastName(input.lastName().trim());
        }

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
        if (!patientRepository.existsById(patientId)) {
            return ApiResponse.error("Patient not found.");
        }

        patientRepository.deleteById(patientId);
        return ApiResponse.success("Patient deleted.", true);
    }

    /**
     * Helper to orchestrate fetching dependencies and using the MapStruct mapper.
     */
    private PatientDto mapToDto(Patient patient) {
        List<PatientInsurance> insurances = patientInsuranceRepository.findByPatientId(patient.getId());
        return patientMapper.toDto(patient, insurances);
    }

    // =========================
    // HELPERS
    // =========================
    private void applyPatientInput(Patient patient, CreatePatientInput input) {
        patient.setFirstName(input.firstName().trim());
        patient.setLastName(blankToNull(input.lastName()));
        patient.setMiddleName(blankToNull(input.middleName()));
        patient.setDateOfBirth(input.dateOfBirth());
        patient.setGender(input.gender());
        patient.setPrimaryPhoneNumber(blankToNull(input.primaryPhoneNumber()));
        patient.setAlternativePhone(blankToNull(input.alternativePhone()));

        patient.setFullName(buildFullName(
                patient.getFirstName(),
                patient.getMiddleName(),
                patient.getLastName()
        ));
    }

    private String buildFullName(String first, String middle, String last) {
        if (middle == null && last == null) return first;
        if (middle == null) return first + " " + last;
        if (last == null) return first + " " + middle;
        return first + " " + middle + " " + last;
    }

    private String blankToNull(String value) {
        return (value == null || value.isBlank()) ? null : value.trim();
    }

    private int normalizePage(Integer page) {
        return (page == null || page < 0) ? 0 : page;
    }

    private int normalizeSize(Integer size) {
        return (size == null || size <= 0) ? 20 : Math.min(size, 100);
    }
}