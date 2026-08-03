package com.nexxserve.nexxclinic.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexxserve.nexxclinic.auth.AuthenticatedUser;
import com.nexxserve.nexxclinic.dto.out.*;
import com.nexxserve.nexxclinic.entity.Department;
import com.nexxserve.nexxclinic.entity.DepartmentStandaloneForm;
import com.nexxserve.nexxclinic.entity.Visit;
import com.nexxserve.nexxclinic.entity.VisitDepartment;
import com.nexxserve.nexxclinic.entity.StandaloneForm;
import com.nexxserve.nexxclinic.entity.StandaloneFormAnswer;
import com.nexxserve.nexxclinic.entity.StandaloneFormVersion;
import com.nexxserve.nexxclinic.graphql.input.StandaloneFormInput;
import com.nexxserve.nexxclinic.mappers.out.StandaloneFormMapper;
import com.nexxserve.nexxclinic.model.AnswerStatus;
import com.nexxserve.nexxclinic.model.FormStatus;
import com.nexxserve.nexxclinic.repository.DepartmentRepository;
import com.nexxserve.nexxclinic.repository.DepartmentStandaloneFormRepository;
import com.nexxserve.nexxclinic.repository.VisitDepartmentRepository;
import com.nexxserve.nexxclinic.repository.VisitRepository;
import com.nexxserve.nexxclinic.repository.StandaloneFormAnswerRepository;
import com.nexxserve.nexxclinic.repository.StandaloneFormRepository;
import com.nexxserve.nexxclinic.repository.StandaloneFormVersionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class StandaloneFormService {

    private final StandaloneFormRepository formRepository;
    private final StandaloneFormVersionRepository versionRepository;
    private final StandaloneFormAnswerRepository answerRepository;
    private final DepartmentStandaloneFormRepository departmentStandaloneFormRepository;
    private final DepartmentRepository departmentRepository;
    private final VisitRepository visitRepository;
    private final VisitDepartmentRepository visitDepartmentRepository;
    private final VisitDepartmentService visitDepartmentService;
    private final StandaloneFormMapper mapper;
    private static final Logger log = LoggerFactory.getLogger(StandaloneFormService.class);
    private final ObjectMapper objectMapper = new ObjectMapper();

    public StandaloneFormService(StandaloneFormRepository formRepository,
                                 StandaloneFormVersionRepository versionRepository,
                                 StandaloneFormAnswerRepository answerRepository,
                                 DepartmentStandaloneFormRepository departmentStandaloneFormRepository,
                                 DepartmentRepository departmentRepository,
                                 VisitRepository visitRepository,
                                 VisitDepartmentRepository visitDepartmentRepository,
                                 @Lazy VisitDepartmentService visitDepartmentService,
                                 StandaloneFormMapper mapper) {
        this.formRepository = formRepository;
        this.versionRepository = versionRepository;
        this.answerRepository = answerRepository;
        this.departmentStandaloneFormRepository = departmentStandaloneFormRepository;
        this.departmentRepository = departmentRepository;
        this.visitRepository = visitRepository;
        this.visitDepartmentRepository = visitDepartmentRepository;
        this.visitDepartmentService = visitDepartmentService;
        this.mapper = mapper;
    }

    @Transactional
    public ApiResponse<StandaloneFormDto> createForm(StandaloneFormInput input, UUID workerId) {
        StandaloneForm form = new StandaloneForm();
        form.setName(input.name());
        form.setDescription(input.description());
        form.setType(input.type());
        form.setCategory(input.category());
        form.setTemplate(input.isTemplate() != null && input.isTemplate());
        form.setCreatedBy(workerId);

        StandaloneForm savedForm = formRepository.save(form);

        StandaloneFormVersion version = new StandaloneFormVersion();
        version.setForm(savedForm);
        version.setMajorVersion(0);
        version.setMinorVersion(0);
        version.setVersionLabel("0.0");
        version.setBlocks(serializeJson(input.blocks()));
        version.setTheme(serializeJson(input.theme()));
        version.setStatus(FormStatus.DRAFT);

        versionRepository.save(version);

        return ApiResponse.success("Form created successfully", mapToDto(savedForm, version));
    }

    @Transactional
    public ApiResponse<StandaloneFormDto> updateForm(UUID id, StandaloneFormInput input, Boolean markFinal) {
        Optional<StandaloneForm> formOpt = formRepository.findById(id);
        if (formOpt.isEmpty() || formOpt.get().isDeleted()) {
            return ApiResponse.error("Form not found");
        }

        StandaloneForm form = formOpt.get();
        form.setName(input.name());
        form.setDescription(input.description());
        form.setType(input.type());
        form.setCategory(input.category());
        if (input.isTemplate() != null) {
            form.setTemplate(input.isTemplate());
        }

        // S8 fix: a form with no version rows must fail with a clean error, not a
        // RuntimeException -> 500.
        Optional<StandaloneFormVersion> latestOptional =
                versionRepository.findTopByFormIdOrderByMajorVersionDescMinorVersionDesc(id);
        if (latestOptional.isEmpty()) {
            return ApiResponse.error("No version found for form");
        }
        StandaloneFormVersion latest = latestOptional.get();

        StandaloneFormVersion targetVersion;
        if (latest.getStatus() == FormStatus.DRAFT) {
            // Overwrite existing DRAFT
            targetVersion = latest;
            targetVersion.setBlocks(serializeJson(input.blocks()));
            targetVersion.setTheme(serializeJson(input.theme()));
        } else {
            // Create new version because latest is FINAL
            targetVersion = new StandaloneFormVersion();
            targetVersion.setForm(form);

            int major = latest.getMajorVersion();
            int minor = latest.getMinorVersion();

            if (minor >= 10) {
                major++;
                minor = 0;
            } else {
                minor++;
            }

            targetVersion.setMajorVersion(major);
            targetVersion.setMinorVersion(minor);
            targetVersion.setVersionLabel(major + "." + minor);
            targetVersion.setBlocks(serializeJson(input.blocks()));
            targetVersion.setTheme(serializeJson(input.theme()));
            targetVersion.setStatus(FormStatus.DRAFT);
        }

        if (markFinal != null && markFinal) {
            targetVersion.setStatus(FormStatus.FINAL);
        }

        versionRepository.save(targetVersion);
        formRepository.save(form);

        return ApiResponse.success("Form updated successfully", mapToDto(form, targetVersion));
    }

    @Transactional
    public ApiResponse<StandaloneFormDto> duplicateForm(UUID sourceFormId, UUID workerId) {
        Optional<StandaloneForm> sourceOpt = formRepository.findById(sourceFormId);
        if (sourceOpt.isEmpty()) {
            return ApiResponse.error("Source form not found");
        }

        StandaloneForm source = sourceOpt.get();
        StandaloneFormVersion latestFinal = versionRepository.findTopByFormIdAndStatusOrderByMajorVersionDescMinorVersionDesc(sourceFormId, FormStatus.FINAL)
                .orElseGet(() -> versionRepository.findTopByFormIdOrderByMajorVersionDescMinorVersionDesc(sourceFormId).orElse(null));

        if (latestFinal == null) {
            return ApiResponse.error("No versions found to duplicate");
        }

        StandaloneForm newForm = new StandaloneForm();
        newForm.setName("Copy of " + source.getName());
        newForm.setDescription(source.getDescription());
        newForm.setType(source.getType());
        newForm.setCategory(source.getCategory());
        newForm.setTemplate(false);
        newForm.setCreatedBy(workerId);

        StandaloneForm savedForm = formRepository.save(newForm);

        StandaloneFormVersion newVersion = new StandaloneFormVersion();
        newVersion.setForm(savedForm);
        newVersion.setMajorVersion(0);
        newVersion.setMinorVersion(0);
        newVersion.setVersionLabel("0.0");
        newVersion.setBlocks(latestFinal.getBlocks());
        newVersion.setTheme(latestFinal.getTheme());
        newVersion.setStatus(FormStatus.DRAFT);

        versionRepository.save(newVersion);

        return ApiResponse.success("Form duplicated successfully", mapToDto(savedForm, newVersion));
    }

    @Transactional
    public ApiResponse<Boolean> deleteForm(UUID id, Boolean confirmDeleteAnswers) {
        Optional<StandaloneForm> formOpt = formRepository.findById(id);
        if (formOpt.isEmpty() || formOpt.get().isDeleted()) {
            return ApiResponse.error("Form not found");
        }

        List<StandaloneFormVersion> versions = versionRepository.findByFormIdOrderByCreatedAtDesc(id);
        boolean hasAnswers = false;
        for (StandaloneFormVersion v : versions) {
            if (answerRepository.existsByFormVersionId(v.getId())) {
                hasAnswers = true;
                break;
            }
        }

        if (hasAnswers && (confirmDeleteAnswers == null || !confirmDeleteAnswers)) {
            return ApiResponse.error("Cannot delete form with associated answers. Please confirm deletion.");
        }

        StandaloneForm form = formOpt.get();
        form.setDeleted(true);
        formRepository.save(form);

        return ApiResponse.success("Form deleted successfully", true);
    }

    public ApiResponse<List<StandaloneFormDto>> getForms(Boolean isTemplate, String category, String name) {
        List<StandaloneForm> forms = formRepository.findAll(StandaloneFormRepository.filter(isTemplate, category, name));

        List<StandaloneFormDto> dtos = forms.stream().map(f -> {
            StandaloneFormVersion latest = versionRepository.findTopByFormIdOrderByMajorVersionDescMinorVersionDesc(f.getId()).orElse(null);
            return mapToDto(f, latest);
        }).toList();

        return ApiResponse.success("Forms fetched successfully", dtos);
    }

    public ApiResponse<StandaloneFormDto> getForm(UUID id) {
        return formRepository.findById(id)
                .filter(f -> !f.isDeleted())
                .map(f -> {
                    StandaloneFormVersion latest = versionRepository.findTopByFormIdOrderByMajorVersionDescMinorVersionDesc(f.getId()).orElse(null);
                    return ApiResponse.success("Form fetched successfully", mapToDto(f, latest));
                })
                .orElse(ApiResponse.error("Form not found"));
    }

    public StandaloneFormVersion getVersion(UUID versionId) {
        return versionRepository.findById(versionId).orElse(null);
    }

    @Transactional
    public ApiResponse<StandaloneFormAnswerDto> saveAnswer(UUID versionId, Object answers, AnswerStatus status, Double score, UUID workerId) {
        Optional<StandaloneFormVersion> versionOpt = versionRepository.findById(versionId);
        if (versionOpt.isEmpty()) {
            return ApiResponse.error("Form version not found");
        }

        StandaloneFormAnswer answer = new StandaloneFormAnswer();
        answer.setFormVersion(versionOpt.get());
        answer.setAnswers(serializeJson(answers));
        answer.setStatus(status != null ? status : AnswerStatus.DRAFT);
        if (score != null) {
            answer.setScore(BigDecimal.valueOf(score));
        }
        answer.setSubmittedBy(workerId);
        if (status == AnswerStatus.FINAL) {
            answer.setSubmittedAt(LocalDateTime.now());
        }

        StandaloneFormAnswer saved = answerRepository.save(answer);
        return ApiResponse.success("Answer saved successfully", mapper.toDto(saved));
    }

    @Transactional
    public ApiResponse<StandaloneFormAnswerDto> updateAnswer(UUID answerId, Object answers, AnswerStatus status, Double score) {
        Optional<StandaloneFormAnswer> answerOpt = answerRepository.findById(answerId);
        if (answerOpt.isEmpty()) {
            return ApiResponse.error("Answer not found");
        }

        StandaloneFormAnswer answer = answerOpt.get();
        if (answer.getStatus() == AnswerStatus.FINAL) {
            return ApiResponse.error("Cannot update a finalized answer");
        }

        answer.setAnswers(serializeJson(answers));
        if (status != null) {
            answer.setStatus(status);
            if (status == AnswerStatus.FINAL) {
                answer.setSubmittedAt(LocalDateTime.now());
            }
        }
        if (score != null) {
            answer.setScore(BigDecimal.valueOf(score));
        }

        StandaloneFormAnswer saved = answerRepository.save(answer);
        return ApiResponse.success("Answer updated successfully", mapper.toDto(saved));
    }

    @Transactional(readOnly = true)
    public ApiResponse<StandaloneFormAnswerDto> getAnswer(UUID id) {
        return answerRepository.findById(id)
                .map(a -> ApiResponse.success("Answer fetched successfully", mapper.toDto(a)))
                .orElse(ApiResponse.error("Answer not found"));
    }

    @Transactional(readOnly = true)
    public ApiResponse<List<StandaloneFormAnswerDto>> getStandaloneFormAnswers(UUID formId) {
        if (formId == null) {
            return ApiResponse.error("formId is required.");
        }

        List<StandaloneFormAnswer> answers = answerRepository.findByFormVersionFormId(formId);
        return ApiResponse.success("Standalone form answers fetched successfully", mapper.toAnswerDtoList(answers));
    }

    @Transactional
    public ApiResponse<Boolean> deleteAnswer(UUID id) {
        if (!answerRepository.existsById(id)) {
            return ApiResponse.error("Answer not found");
        }
        answerRepository.deleteById(id);
        return ApiResponse.success("Answer deleted successfully", true);
    }

    @Transactional
    public ApiResponse<VisitStandaloneAnswerDto> saveVisitStandaloneAnswer(
            UUID visitId,
            UUID visitDepartmentId,
            UUID versionId,
            Object answers,
            AnswerStatus status,
            Double score,
            UUID workerId,
            AuthenticatedUser authUser
    ) {
        Optional<StandaloneFormVersion> versionOpt = versionRepository.findById(versionId);
        if (versionOpt.isEmpty()) {
            return ApiResponse.error("Form version not found");
        }

        Optional<Visit> visitOpt = visitRepository.findById(visitId);
        if (visitOpt.isEmpty()) {
            return ApiResponse.error("Visit not found");
        }

        Optional<VisitDepartment> visitDeptOpt = visitDepartmentRepository.findById(visitDepartmentId)
                .filter(vd -> vd.getVisit() != null && visitId.equals(vd.getVisit().getId()));
        if (visitDeptOpt.isEmpty()) {
            return ApiResponse.error("Visit department not found for this visit.");
        }

        VisitDepartment visitDept = visitDeptOpt.get();
        Visit visit = visitOpt.get();

        StandaloneFormAnswer answer = new StandaloneFormAnswer();
        answer.setFormVersion(versionOpt.get());
        answer.setAnswers(serializeJson(answers));
        answer.setStatus(status != null ? status : AnswerStatus.DRAFT);
        if (score != null) {
            answer.setScore(BigDecimal.valueOf(score));
        }
        answer.setSubmittedBy(workerId);
        if (status == AnswerStatus.FINAL) {
            answer.setSubmittedAt(LocalDateTime.now());
        }

        StandaloneFormAnswer savedAnswer = answerRepository.save(answer);

        // Link to the specific VisitDepartment (parent or child)
        visitDept.setAnswerId(savedAnswer.getId());
        VisitDepartment savedVisitDepartment = visitDepartmentRepository.save(visitDept);

        StandaloneFormAnswerDto mappedAnswerDto = mapper.toDto(savedAnswer);
        StandaloneFormAnswerDto answerDto = new StandaloneFormAnswerDto(
                mappedAnswerDto.id(),
                mappedAnswerDto.form(),
                mappedAnswerDto.formVersion(),
                mappedAnswerDto.answers(),
                mappedAnswerDto.score(),
                mappedAnswerDto.status(),
                visit.getPatient() != null ? visit.getPatient().getId() : null,
                visit.getId(),
                mappedAnswerDto.submittedBy(),
                mappedAnswerDto.submittedAt(),
                mappedAnswerDto.createdAt(),
                mappedAnswerDto.updatedAt()
        );
        VisitDepartmentDto visitDeptDto = visitDepartmentService.visitDepartmentToDto(savedVisitDepartment, Set.of(), authUser);

        return ApiResponse.success("Visit answer saved successfully", new VisitStandaloneAnswerDto(answerDto, visitDeptDto));
    }

    @Transactional
    public ApiResponse<StandaloneFormDto>  linkFormToDepartment(UUID departmentId, UUID formId) {
        Optional<Department> departmentOpt = departmentRepository.findById(departmentId);
        if (departmentOpt.isEmpty()) {
            return ApiResponse.error("Department not found");
        }

        Optional<StandaloneForm> formOpt = formRepository.findById(formId);
        if (formOpt.isEmpty() || formOpt.get().isDeleted()) {
            return ApiResponse.error("Form not found");
        }

        Optional<DepartmentStandaloneForm> existingLink = departmentStandaloneFormRepository.findByDepartmentIdAndStandaloneFormId(departmentId, formId);
        if (existingLink.isPresent()) {
            return getForm(formId);
        }

        DepartmentStandaloneForm link = new DepartmentStandaloneForm();
        link.setDepartment(departmentOpt.get());
        link.setStandaloneForm(formOpt.get());
        link.setDefault(false);
        departmentStandaloneFormRepository.save(link);

        return getForm(formId);
    }

    @Transactional(readOnly = true)
    public ApiResponse<DepartmentFormsResult> getDepartmentFormsWithDefault(UUID departmentId) {
        log.info("Fetching department forms for departmentId: {}", departmentId);

        List<DepartmentStandaloneForm> links = departmentStandaloneFormRepository.findByDepartmentId(departmentId);
        log.info("Found {} links for department {}", links.size(), departmentId);

        // Log each link details
        for (DepartmentStandaloneForm link : links) {
            StandaloneForm form = link.getStandaloneForm();
            log.debug("Link - formId: {}, formName: {}, isDeleted: {}, isDefault: {}, departmentId: {}",
                    form != null ? form.getId() : "null",
                    form != null ? form.getName() : "null",
                    form != null ? form.isDeleted() : "unknown",
                    link.isDefault(),
                    link.getDepartment() != null ? link.getDepartment().getId() : "null"
            );
        }

        if (links.isEmpty()) {
            log.warn("No forms linked to department: {}", departmentId);
            DepartmentFormsResult emptyResult = new DepartmentFormsResult(List.of(), null);
            return ApiResponse.success("No forms linked to this department", emptyResult);
        }

        // Check if any form is marked as default
        boolean hasDefault = links.stream().anyMatch(DepartmentStandaloneForm::isDefault);
        log.info("Has default form: {}", hasDefault);

        // If no default is set, mark the first one as default
        if (!hasDefault && !links.isEmpty()) {
            log.info("No default found, marking first link as default");
            DepartmentStandaloneForm firstLink = links.getFirst();
            log.debug("Setting default for formId: {}, formName: {}",
                    firstLink.getStandaloneForm().getId(),
                    firstLink.getStandaloneForm().getName()
            );
            firstLink.setDefault(true);
            departmentStandaloneFormRepository.save(firstLink);
            log.info("Saved default flag for formId: {}", firstLink.getStandaloneForm().getId());

            // Refresh the list to get updated default status
            links = departmentStandaloneFormRepository.findByDepartmentId(departmentId);
            log.info("Refreshed links, now have {} links", links.size());
        }

        List<DepartmentFormDto> dtos = links.stream()
                .map(link -> {
                    StandaloneForm form = link.getStandaloneForm();
                    log.debug("Processing link - formId: {}, formName: {}, isDeleted: {}, isDefault: {}",
                            form != null ? form.getId() : "null",
                            form != null ? form.getName() : "null",
                            form != null ? form.isDeleted() : "null",
                            link.isDefault()
                    );

                    if (form == null) {
                        log.warn("Link has null form reference: {}", link.getId());
                        return null;
                    }

                    if (form.isDeleted()) {
                        log.warn("Form is deleted, skipping: formId={}, formName={}", form.getId(), form.getName());
                        return null;
                    }

                    StandaloneFormVersion latest = versionRepository
                            .findTopByFormIdOrderByMajorVersionDescMinorVersionDesc(form.getId())
                            .orElse(null);
                    if (latest == null) {
                        log.warn("No version found for form: formId={}, formName={}", form.getId(), form.getName());
                    }

                    StandaloneFormDto formDto = mapToDto(form, latest);
                    DepartmentFormDto dto = new DepartmentFormDto(formDto, link.isDefault());
                    log.debug("Created DepartmentFormDto - formId: {}, isDefault: {}", formDto.id(), link.isDefault());
                    return dto;
                })
                .filter(Objects::nonNull)
                .toList();

        // Find the default form
        StandaloneFormDto defaultForm = dtos.stream()
                .filter(DepartmentFormDto::isDefault)
                .map(DepartmentFormDto::form)
                .findFirst()
                .orElse(null);

        log.info("Returning {} department forms for departmentId: {}", dtos.size(), departmentId);
        DepartmentFormsResult result = new DepartmentFormsResult(dtos, defaultForm);
        return ApiResponse.success("Department forms fetched successfully", result);
    }
    @Transactional
    public ApiResponse<Boolean> unlinkFormFromDepartment(UUID departmentId, UUID formId) {
        departmentStandaloneFormRepository.deleteByDepartmentIdAndStandaloneFormId(departmentId, formId);
        return ApiResponse.success("Form unlinked from department", true);
    }

    @Transactional
    public ApiResponse<StandaloneFormDto> setDefaultFormForDepartment(UUID departmentId, UUID formId) {
        Optional<DepartmentStandaloneForm> linkOpt = departmentStandaloneFormRepository.findByDepartmentIdAndStandaloneFormId(departmentId, formId);
        if (linkOpt.isEmpty()) {
            return ApiResponse.error("Form is not linked to this department");
        }

        departmentStandaloneFormRepository.clearDefaultByDepartmentId(departmentId);

        DepartmentStandaloneForm link = linkOpt.get();
        link.setDefault(true);
        departmentStandaloneFormRepository.save(link);

        return getForm(formId);
    }

    private String serializeJson(Object obj) {
        if (obj == null) return null;
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            return "{}";
        }
    }

    private StandaloneFormDto mapToDto(StandaloneForm form, StandaloneFormVersion version) {
        StandaloneFormDto baseDto = mapper.toDto(form);
        StandaloneFormVersionDto versionDto = version != null ? mapper.toDto(version) : null;
        return new StandaloneFormDto(
                baseDto.id(),
                baseDto.name(),
                baseDto.description(),
                baseDto.type(),
                baseDto.category(),
                baseDto.isTemplate(),
                baseDto.createdBy(),
                versionDto,
                baseDto.createdAt(),
                baseDto.updatedAt()
        );
    }
}
