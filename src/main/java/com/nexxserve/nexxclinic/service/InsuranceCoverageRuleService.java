package com.nexxserve.nexxclinic.service;

import com.nexxserve.nexxclinic.dto.out.ApiResponse;
import com.nexxserve.nexxclinic.dto.out.InsuranceCoverageRuleDto;
import com.nexxserve.nexxclinic.entity.Department;
import com.nexxserve.nexxclinic.entity.InsuranceCoverageRule;
import com.nexxserve.nexxclinic.entity.InsuranceProvider;
import com.nexxserve.nexxclinic.graphql.input.CreateInsuranceCoverageRuleInput;
import com.nexxserve.nexxclinic.graphql.input.SearchInsuranceCoverageRulesInput;
import com.nexxserve.nexxclinic.graphql.input.UpdateInsuranceCoverageRuleInput;
import com.nexxserve.nexxclinic.repository.DepartmentRepository;
import com.nexxserve.nexxclinic.repository.InsuranceCoverageRuleRepository;
import com.nexxserve.nexxclinic.repository.InsuranceProviderRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InsuranceCoverageRuleService {

    private final InsuranceCoverageRuleRepository ruleRepository;
    private final InsuranceProviderRepository insuranceProviderRepository;
    private final DepartmentRepository departmentRepository;

    public InsuranceCoverageRuleService(
        InsuranceCoverageRuleRepository ruleRepository,
        InsuranceProviderRepository insuranceProviderRepository,
        DepartmentRepository departmentRepository
    ) {
        this.ruleRepository = ruleRepository;
        this.insuranceProviderRepository = insuranceProviderRepository;
        this.departmentRepository = departmentRepository;
    }

    @Transactional
    public ApiResponse<InsuranceCoverageRuleDto> createRule(CreateInsuranceCoverageRuleInput input) {
        if (input == null) {
            return ApiResponse.error("input is required.");
        }
        if (input.insuranceProviderId() == null) {
            return ApiResponse.error("insuranceProviderId is required.");
        }
        if (input.patientSharePercentage() == null) {
            return ApiResponse.error("patientSharePercentage is required.");
        }

        Optional<InsuranceProvider> providerOpt =
            insuranceProviderRepository.findById(input.insuranceProviderId());
        if (providerOpt.isEmpty()) {
            return ApiResponse.error("Insurance provider not found.");
        }

        Department department = null;
        if (input.departmentId() != null) {
            Optional<Department> deptOpt = departmentRepository.findById(input.departmentId());
            if (deptOpt.isEmpty()) {
                return ApiResponse.error("Department not found.");
            }
            department = deptOpt.get();
        }

        // Check for duplicate rule
        Optional<InsuranceCoverageRule> existing = ruleRepository
            .findByInsuranceProviderIdAndDepartmentIdAndEncounterType(
                input.insuranceProviderId(),
                input.departmentId(),
                input.encounterType()
            );
        if (existing.isPresent()) {
            return ApiResponse.error(
                "A rule already exists for this insurance provider, department, and encounter type combination. Use update instead."
            );
        }

        InsuranceCoverageRule rule = new InsuranceCoverageRule();
        rule.setInsuranceProvider(providerOpt.get());
        rule.setDepartment(department);
        rule.setEncounterType(input.encounterType());
        rule.setPatientSharePercentage(input.patientSharePercentage());

        InsuranceCoverageRule saved = ruleRepository.save(rule);
        return ApiResponse.success("Coverage rule created.", toDto(saved));
    }

    @Transactional
    public ApiResponse<InsuranceCoverageRuleDto> updateRule(
        UUID ruleId,
        UpdateInsuranceCoverageRuleInput input
    ) {
        if (ruleId == null || input == null) {
            return ApiResponse.error("ruleId and input are required.");
        }

        Optional<InsuranceCoverageRule> ruleOpt = ruleRepository.findById(ruleId);
        if (ruleOpt.isEmpty()) {
            return ApiResponse.error("Coverage rule not found.");
        }

        InsuranceCoverageRule rule = ruleOpt.get();

        if (input.insuranceProviderId() != null) {
            Optional<InsuranceProvider> providerOpt =
                insuranceProviderRepository.findById(input.insuranceProviderId());
            if (providerOpt.isEmpty()) {
                return ApiResponse.error("Insurance provider not found.");
            }
            rule.setInsuranceProvider(providerOpt.get());
        }

        if (input.departmentId() != null) {
            Optional<Department> deptOpt = departmentRepository.findById(input.departmentId());
            if (deptOpt.isEmpty()) {
                return ApiResponse.error("Department not found.");
            }
            rule.setDepartment(deptOpt.get());
        } else if (input.departmentId() == null && input.insuranceProviderId() != null) {
            // Allow explicitly clearing department to create a provider-wide rule
            rule.setDepartment(null);
        }

        if (input.encounterType() != null) {
            rule.setEncounterType(input.encounterType());
        }

        if (input.patientSharePercentage() != null) {
            rule.setPatientSharePercentage(input.patientSharePercentage());
        }

        InsuranceCoverageRule saved = ruleRepository.save(rule);
        return ApiResponse.success("Coverage rule updated.", toDto(saved));
    }

    @Transactional
    public ApiResponse<Boolean> deleteRule(UUID ruleId) {
        if (ruleId == null) {
            return ApiResponse.error("ruleId is required.");
        }
        if (!ruleRepository.existsById(ruleId)) {
            return ApiResponse.error("Coverage rule not found.");
        }
        ruleRepository.deleteById(ruleId);
        return ApiResponse.success("Coverage rule deleted.", true);
    }

    @Transactional(readOnly = true)
    public ApiResponse<InsuranceCoverageRuleDto> getRule(UUID ruleId) {
        if (ruleId == null) {
            return ApiResponse.error("ruleId is required.");
        }
        return ruleRepository.findById(ruleId)
            .map(r -> ApiResponse.success("Coverage rule fetched.", toDto(r)))
            .orElseGet(() -> ApiResponse.error("Coverage rule not found."));
    }

    @Transactional(readOnly = true)
    public ApiResponse<List<InsuranceCoverageRuleDto>> listRules(SearchInsuranceCoverageRulesInput input) {
        List<InsuranceCoverageRule> rules;
        if (input != null && input.insuranceProviderId() != null) {
            if (input.departmentId() != null) {
                rules = ruleRepository.findByInsuranceProviderIdAndDepartmentIdOrderByEncounterTypeAsc(
                    input.insuranceProviderId(), input.departmentId()
                );
            } else {
                rules = ruleRepository.findByInsuranceProviderIdOrderByDepartmentNameAscEncounterTypeAsc(
                    input.insuranceProviderId()
                );
            }
        } else {
            rules = ruleRepository.findAll();
        }

        List<InsuranceCoverageRuleDto> dtos = rules.stream().map(this::toDto).toList();
        return ApiResponse.success("Coverage rules fetched.", dtos);
    }

    private InsuranceCoverageRuleDto toDto(InsuranceCoverageRule rule) {
        return new InsuranceCoverageRuleDto(
            rule.getId(),
            rule.getInsuranceProvider().getId(),
            rule.getInsuranceProvider().getInsuranceName(),
            rule.getDepartment() != null ? rule.getDepartment().getId() : null,
            rule.getDepartment() != null ? rule.getDepartment().getName() : null,
            rule.getEncounterType(),
            rule.getPatientSharePercentage(),
            rule.getCreatedAt(),
            rule.getUpdatedAt()
        );
    }
}
