package com.nexxserve.nexxclinic.service;

import com.nexxserve.nexxclinic.entity.AnswerStatus;
import com.nexxserve.nexxclinic.entity.DepartmentForm;
import com.nexxserve.nexxclinic.entity.DepartmentFormAnswer;
import com.nexxserve.nexxclinic.entity.DepartmentFormVersion;
import com.nexxserve.nexxclinic.entity.FormStatus;
import com.nexxserve.nexxclinic.repository.DepartmentFormAnswerRepository;
import com.nexxserve.nexxclinic.repository.DepartmentFormRepository;
import com.nexxserve.nexxclinic.repository.DepartmentFormVersionRepository;
import com.nexxserve.nexxclinic.model.ApiResponse;
import com.nexxserve.nexxclinic.model.ResponseStatus;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DepartmentFormService {

    @Autowired
    private DepartmentFormRepository formRepository;

    @Autowired
    private DepartmentFormVersionRepository versionRepository;

    @Autowired
    private DepartmentFormAnswerRepository answerRepository;

    // Create a new form (starts as DRAFT with version 1.0)
    @Transactional
    public ApiResponse createForm(UUID departmentId, String title, String description, Integer schemaVersion, String formData) {
        try {
            DepartmentForm form = new DepartmentForm();
            form.setTitle(title);
            form.setDescription(description);
            form.setCurrentSchemaVersion(schemaVersion != null ? schemaVersion : 1);
            form.setFormData(formData != null ? formData : "{}");
            form.setStatus(FormStatus.DRAFT);
            form.setCurrentVersionNumber("1.0");

            DepartmentForm savedForm = formRepository.save(form);

            // Create initial version
            DepartmentFormVersion version = new DepartmentFormVersion();
            version.setForm(savedForm);
            version.setVersionNumber("1.0");
            version.setStatus(FormStatus.DRAFT);
            version.setFormData(formData != null ? formData : "{}");
            versionRepository.save(version);

            return ApiResponse.success("Form created.", formToMap(savedForm));
        } catch (Exception e) {
            return ApiResponse.error("Failed to create form: " + e.getMessage(), "FORM_CREATE_ERROR");
        }
    }

    // Update a form with versioning logic
    // If DRAFT: update current version, no new version
    // If FINAL: create new version and set to DRAFT
    @Transactional
    public ApiResponse updateForm(UUID formId, String title, String description, String formData) {
        try {
            Optional<DepartmentForm> formOpt = formRepository.findById(formId);
            if (formOpt.isEmpty()) {
                return ApiResponse.error("Form not found", "NOT_FOUND");
            }

            DepartmentForm form = formOpt.get();

            if (form.getStatus() == FormStatus.DRAFT) {
                // Update current version data, keep same version number
                form.setTitle(title != null ? title : form.getTitle());
                form.setDescription(description != null ? description : form.getDescription());
                form.setFormData(formData != null ? formData : form.getFormData());
                DepartmentForm updated = formRepository.save(form);

                // Update the latest version's data
                Optional<DepartmentFormVersion> latestVersion = versionRepository.findLatestVersionByFormId(formId);
                if (latestVersion.isPresent()) {
                    DepartmentFormVersion version = latestVersion.get();
                    version.setFormData(formData != null ? formData : version.getFormData());
                    versionRepository.save(version);
                }

                return ApiResponse.success("Form updated.", formToMap(updated));
            } else {
                // Status is FINAL - create new version
                String nextVersion = incrementVersion(form.getCurrentVersionNumber());

                // Create new version as DRAFT
                DepartmentFormVersion newVersion = new DepartmentFormVersion();
                newVersion.setForm(form);
                newVersion.setVersionNumber(nextVersion);
                newVersion.setStatus(FormStatus.DRAFT);
                newVersion.setFormData(formData != null ? formData : form.getFormData());
                versionRepository.save(newVersion);

                // Update form to point to new version and set to DRAFT
                form.setCurrentVersionNumber(nextVersion);
                form.setStatus(FormStatus.DRAFT);
                form.setTitle(title != null ? title : form.getTitle());
                form.setDescription(description != null ? description : form.getDescription());
                form.setFormData(formData != null ? formData : form.getFormData());
                DepartmentForm updated = formRepository.save(form);

                return ApiResponse.success("Form updated with new version.", formToMap(updated));
            }
        } catch (Exception e) {
            return ApiResponse.error("Failed to update form: " + e.getMessage(), "FORM_UPDATE_ERROR");
        }
    }

    // Finalize a form (lock current version)
    @Transactional
    public ApiResponse finalizeForm(UUID formId) {
        try {
            Optional<DepartmentForm> formOpt = formRepository.findById(formId);
            if (formOpt.isEmpty()) {
                return ApiResponse.error("Form not found", "NOT_FOUND");
            }

            DepartmentForm form = formOpt.get();
            form.setStatus(FormStatus.FINAL);
            DepartmentForm updated = formRepository.save(form);

            // Update the current version to FINAL
            Optional<DepartmentFormVersion> currentVersion = versionRepository.findByFormIdAndVersionNumber(
                formId,
                form.getCurrentVersionNumber()
            );
            if (currentVersion.isPresent()) {
                DepartmentFormVersion version = currentVersion.get();
                version.setStatus(FormStatus.FINAL);
                versionRepository.save(version);
            }

            return ApiResponse.success("Form finalized.", formToMap(updated));
        } catch (Exception e) {
            return ApiResponse.error("Failed to finalize form: " + e.getMessage(), "FORM_FINALIZE_ERROR");
        }
    }

    // Get all forms for a department
    public ApiResponse getFormsByDepartment(UUID departmentId) {
        try {
            List<DepartmentForm> forms = formRepository.findByDepartmentIdOrderByCreatedAtDesc(departmentId);
            List<Map<String, Object>> formsData = forms.stream()
                .map(this::formToMap)
                .toList();
            return ApiResponse.success("Forms retrieved.", formsData);
        } catch (Exception e) {
            return ApiResponse.error("Failed to retrieve forms: " + e.getMessage(), "FORM_RETRIEVE_ERROR");
        }
    }

    // Get single form with full details
    public ApiResponse getForm(UUID formId, UUID departmentId) {
        try {
            Optional<DepartmentForm> formOpt = formRepository.findByIdAndDepartmentId(formId, departmentId);
            if (formOpt.isEmpty()) {
                return ApiResponse.error("Form not found", "NOT_FOUND");
            }
            return ApiResponse.success("Form retrieved.", formToMap(formOpt.get()));
        } catch (Exception e) {
            return ApiResponse.error("Failed to retrieve form: " + e.getMessage(), "FORM_RETRIEVE_ERROR");
        }
    }

    // Get all versions of a form
    public ApiResponse getFormVersions(UUID formId) {
        try {
            List<DepartmentFormVersion> versions = versionRepository.findByFormIdOrderByCreatedAtDesc(formId);
            List<Map<String, Object>> versionsData = versions.stream()
                .map(this::versionToMap)
                .toList();
            return ApiResponse.success("Form versions retrieved.", versionsData);
        } catch (Exception e) {
            return ApiResponse.error("Failed to retrieve versions: " + e.getMessage(), "VERSION_RETRIEVE_ERROR");
        }
    }

    // Get specific version
    public ApiResponse getFormVersion(UUID versionId) {
        try {
            Optional<DepartmentFormVersion> versionOpt = versionRepository.findById(versionId);
            if (versionOpt.isEmpty()) {
                return ApiResponse.error("Version not found", "NOT_FOUND");
            }
            return ApiResponse.success("Form version retrieved.", versionToMap(versionOpt.get()));
        } catch (Exception e) {
            return ApiResponse.error("Failed to retrieve version: " + e.getMessage(), "VERSION_RETRIEVE_ERROR");
        }
    }

    // Get latest form version
    public ApiResponse getLatestFormVersion(UUID formId) {
        try {
            Optional<DepartmentFormVersion> versionOpt = versionRepository.findLatestVersionByFormId(formId);
            if (versionOpt.isEmpty()) {
                return ApiResponse.error("No versions found for form", "NOT_FOUND");
            }
            return ApiResponse.success("Latest form version retrieved.", versionToMap(versionOpt.get()));
        } catch (Exception e) {
            return ApiResponse.error("Failed to retrieve latest version: " + e.getMessage(), "VERSION_RETRIEVE_ERROR");
        }
    }

    // Get latest FINAL version for answers
    private Optional<DepartmentFormVersion> getLatestFinalVersion(UUID formId) {
        return versionRepository.findLatestFinalVersionByFormId(formId, FormStatus.FINAL);
    }

    // Upsert form answer (create or update)
    @Transactional
    public ApiResponse upsertFormAnswer(
        String consultationId,
        String visitId,
        String patientId,
        UUID departmentId,
        UUID formId,
        String answers,
        AnswerStatus status
    ) {
        try {
            // Get latest FINAL version of form
            Optional<DepartmentFormVersion> latestVersion = getLatestFinalVersion(formId);
            if (latestVersion.isEmpty()) {
                return ApiResponse.error("No final version available for this form", "NO_FINAL_VERSION");
            }

            DepartmentFormVersion version = latestVersion.get();
            Optional<DepartmentFormAnswer> existingAnswer = answerRepository.findByConsultationIdAndFormId(
                consultationId,
                formId
            );

            DepartmentFormAnswer answer;
            if (existingAnswer.isPresent()) {
                answer = existingAnswer.get();
                answer.setAnswers(answers != null ? answers : answer.getAnswers());
                answer.setStatus(status != null ? status : answer.getStatus());
                if (status == AnswerStatus.FINAL) {
                    answer.setSubmittedAt(LocalDateTime.now());
                }
            } else {
                answer = new DepartmentFormAnswer();
                answer.setConsultationId(consultationId);
                answer.setVisitId(visitId);
                answer.setPatientId(patientId);
                answer.setFormVersion(version);
                answer.setAnswers(answers != null ? answers : "{}");
                answer.setStatus(status != null ? status : AnswerStatus.DRAFT);
                if (status == AnswerStatus.FINAL) {
                    answer.setSubmittedAt(LocalDateTime.now());
                }
            }

            DepartmentFormAnswer saved = answerRepository.save(answer);
            return ApiResponse.success("Form answer saved.", answerToMap(saved));
        } catch (Exception e) {
            return ApiResponse.error("Failed to save answer: " + e.getMessage(), "ANSWER_SAVE_ERROR");
        }
    }

    // Get answers for consultation
    public ApiResponse getConsultationAnswers(String consultationId, UUID departmentId, UUID formId) {
        try {
            Optional<DepartmentFormAnswer> answer = answerRepository.findByConsultationIdAndFormId(
                consultationId,
                formId
            );
            if (answer.isEmpty()) {
                return ApiResponse.success("No answers found.", null);
            }
            return ApiResponse.success("Answers retrieved.", answerToMap(answer.get()));
        } catch (Exception e) {
            return ApiResponse.error("Failed to retrieve answers: " + e.getMessage(), "ANSWER_RETRIEVE_ERROR");
        }
    }

    // Helper method to increment version number
    private String incrementVersion(String currentVersion) {
        try {
            String[] parts = currentVersion.split("\\.");
            if (parts.length > 0) {
                int majorVersion = Integer.parseInt(parts[0]) + 1;
                return majorVersion + ".0";
            }
            return "2.0";
        } catch (Exception e) {
            return "2.0";
        }
    }

    // Convert form to map
    private Map<String, Object> formToMap(DepartmentForm form) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", form.getId());
        map.put("departmentId", form.getDepartment() != null ? form.getDepartment().getId() : null);
        map.put("title", form.getTitle());
        map.put("description", form.getDescription());
        map.put("status", form.getStatus());
        map.put("currentVersionNumber", form.getCurrentVersionNumber());
        map.put("currentSchemaVersion", form.getCurrentSchemaVersion());
        map.put("formData", form.getFormData());
        map.put("createdAt", form.getCreatedAt());
        map.put("updatedAt", form.getUpdatedAt());
        return map;
    }

    // Convert version to map
    private Map<String, Object> versionToMap(DepartmentFormVersion version) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", version.getId());
        map.put("formId", version.getForm() != null ? version.getForm().getId() : null);
        map.put("versionNumber", version.getVersionNumber());
        map.put("status", version.getStatus());
        map.put("formData", version.getFormData());
        map.put("createdAt", version.getCreatedAt());
        return map;
    }

    // Convert answer to map
    private Map<String, Object> answerToMap(DepartmentFormAnswer answer) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", answer.getId());
        map.put("consultationId", answer.getConsultationId());
        map.put("visitId", answer.getVisitId());
        map.put("patientId", answer.getPatientId());
        map.put("departmentId", answer.getDepartment() != null ? answer.getDepartment().getId() : null);
        map.put("formId", answer.getForm() != null ? answer.getForm().getId() : null);
        map.put("formSchemaVersion", answer.getFormVersion() != null ? answer.getFormVersion().getVersionNumber() : null);
        map.put("status", answer.getStatus());
        map.put("answers", answer.getAnswers());
        map.put("submittedAt", answer.getSubmittedAt());
        map.put("updatedAt", answer.getUpdatedAt());
        map.put("createdAt", answer.getCreatedAt());
        return map;
    }
}
