package com.nexxserve.nexxclinic.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexxserve.nexxclinic.dto.out.ApiResponse;
import com.nexxserve.nexxclinic.dto.out.StandaloneFormAnswerDto;
import com.nexxserve.nexxclinic.dto.out.StandaloneFormDto;
import com.nexxserve.nexxclinic.dto.out.StandaloneFormVersionDto;
import com.nexxserve.nexxclinic.entity.StandaloneForm;
import com.nexxserve.nexxclinic.entity.StandaloneFormAnswer;
import com.nexxserve.nexxclinic.entity.StandaloneFormVersion;
import com.nexxserve.nexxclinic.graphql.input.StandaloneFormInput;
import com.nexxserve.nexxclinic.mappers.out.StandaloneFormMapper;
import com.nexxserve.nexxclinic.model.AnswerStatus;
import com.nexxserve.nexxclinic.model.FormStatus;
import com.nexxserve.nexxclinic.repository.StandaloneFormAnswerRepository;
import com.nexxserve.nexxclinic.repository.StandaloneFormRepository;
import com.nexxserve.nexxclinic.repository.StandaloneFormVersionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class StandaloneFormService {

    private final StandaloneFormRepository formRepository;
    private final StandaloneFormVersionRepository versionRepository;
    private final StandaloneFormAnswerRepository answerRepository;
    private final StandaloneFormMapper mapper;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public StandaloneFormService(StandaloneFormRepository formRepository,
                                 StandaloneFormVersionRepository versionRepository,
                                 StandaloneFormAnswerRepository answerRepository,
                                 StandaloneFormMapper mapper) {
        this.formRepository = formRepository;
        this.versionRepository = versionRepository;
        this.answerRepository = answerRepository;
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

        StandaloneFormVersion latest = versionRepository.findTopByFormIdOrderByMajorVersionDescMinorVersionDesc(id)
                .orElseThrow(() -> new RuntimeException("No version found for form"));

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

    public ApiResponse<List<StandaloneFormDto>> getForms(Boolean isTemplate, String category) {
        List<StandaloneForm> forms;
        if (isTemplate != null && category != null) {
            forms = formRepository.findByIsTemplateAndCategoryAndIsDeletedFalse(isTemplate, category);
        } else if (isTemplate != null) {
            forms = formRepository.findByIsTemplateAndIsDeletedFalse(isTemplate);
        } else if (category != null) {
            forms = formRepository.findByCategoryAndIsDeletedFalse(category);
        } else {
            forms = formRepository.findByIsDeletedFalse();
        }

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

    public ApiResponse<StandaloneFormAnswerDto> getAnswer(UUID id) {
        return answerRepository.findById(id)
                .map(a -> ApiResponse.success("Answer fetched successfully", mapper.toDto(a)))
                .orElse(ApiResponse.error("Answer not found"));
    }

    public List<StandaloneFormAnswerDto> getAnswers(UUID formId, UUID patientId) {
        List<StandaloneFormAnswer> answers;
        if (formId != null && patientId != null) {
            answers = answerRepository.findByFormVersionFormIdAndPatientId(formId, patientId);
        } else if (formId != null) {
            answers = answerRepository.findByFormVersionFormId(formId);
        } else if (patientId != null) {
            answers = answerRepository.findByPatientId(patientId);
        } else {
            answers = answerRepository.findAll();
        }
        return mapper.toAnswerDtoList(answers);
    }

    @Transactional
    public ApiResponse<Boolean> deleteAnswer(UUID id) {
        if (!answerRepository.existsById(id)) {
            return ApiResponse.error("Answer not found");
        }
        answerRepository.deleteById(id);
        return ApiResponse.success("Answer deleted successfully", true);
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
        StandaloneFormDto dto = mapper.toDto(form);
        StandaloneFormVersionDto versionDto = version != null ? mapper.toDto(version) : null;
        return new StandaloneFormDto(
                dto.id(),
                dto.name(),
                dto.description(),
                dto.type(),
                dto.category(),
                dto.isTemplate(),
                dto.createdBy(),
                versionDto,
                dto.createdAt(),
                dto.updatedAt()
        );
    }
}
