package com.nexxserve.nexxclinic.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexxserve.nexxclinic.auth.AuthenticatedUser;
import com.nexxserve.nexxclinic.entity.ConsultationAnswer;
import com.nexxserve.nexxclinic.entity.Department;
import com.nexxserve.nexxclinic.entity.DepartmentForm;
import com.nexxserve.nexxclinic.entity.DepartmentFormVersion;
import com.nexxserve.nexxclinic.entity.Worker;
import com.nexxserve.nexxclinic.graphql.input.ConsultationAnswersInput;
import com.nexxserve.nexxclinic.graphql.input.ConditionalRenderingInput;
import com.nexxserve.nexxclinic.graphql.input.FormActionInput;
import com.nexxserve.nexxclinic.graphql.input.FormFieldInput;
import com.nexxserve.nexxclinic.graphql.input.FormInput;
import com.nexxserve.nexxclinic.graphql.input.FormSectionInput;
import com.nexxserve.nexxclinic.graphql.input.LabRecordConfigInput;
import com.nexxserve.nexxclinic.graphql.input.LabRecordRowInput;
import com.nexxserve.nexxclinic.graphql.input.TableConfigInput;
import com.nexxserve.nexxclinic.model.AnswerStatus;
import com.nexxserve.nexxclinic.model.ApiResponse;
import com.nexxserve.nexxclinic.model.FormStatus;
import com.nexxserve.nexxclinic.repository.ConsultationAnswerRepository;
import com.nexxserve.nexxclinic.repository.DepartmentFormRepository;
import com.nexxserve.nexxclinic.repository.DepartmentFormVersionRepository;
import com.nexxserve.nexxclinic.repository.DepartmentRepository;
import com.nexxserve.nexxclinic.repository.WorkerRepository;
import com.nexxserve.nexxclinic.entity.VisitDepartment;
import com.nexxserve.nexxclinic.repository.VisitDepartmentRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DepartmentFormService {

    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {
    };

    private final DepartmentRepository departmentRepository;
    private final DepartmentFormRepository departmentFormRepository;
    private final DepartmentFormVersionRepository departmentFormVersionRepository;
    private final ConsultationAnswerRepository consultationAnswerRepository;
    private final WorkerRepository workerRepository;
    private final VisitDepartmentRepository visitDepartmentRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public DepartmentFormService(
            DepartmentRepository departmentRepository,
            DepartmentFormRepository departmentFormRepository,
            DepartmentFormVersionRepository departmentFormVersionRepository,
            ConsultationAnswerRepository consultationAnswerRepository,
            WorkerRepository workerRepository,
            VisitDepartmentRepository visitDepartmentRepository
    ) {
        this.departmentRepository = departmentRepository;
        this.departmentFormRepository = departmentFormRepository;
        this.departmentFormVersionRepository = departmentFormVersionRepository;
        this.consultationAnswerRepository = consultationAnswerRepository;
        this.workerRepository = workerRepository;
        this.visitDepartmentRepository = visitDepartmentRepository;
    }

    @Transactional
    public ApiResponse createForm(UUID departmentId, FormInput input) {
        if (departmentId == null || input == null) {
            return ApiResponse.error("departmentId and input are required.", "VALIDATION_ERROR");
        }

        Optional<Department> departmentOptional = departmentRepository.findById(departmentId);
        if (departmentOptional.isEmpty()) {
            return ApiResponse.error("Department not found.", "NOT_FOUND");
        }

        String title = requiredTrim(input.title());
        if (title == null) {
            return ApiResponse.error("title is required.", "VALIDATION_ERROR");
        }

        String serializedFormData = serializeFormData(input);
        if (serializedFormData == null) {
            return ApiResponse.error("Form definition is invalid.", "VALIDATION_ERROR");
        }

        DepartmentForm form = new DepartmentForm();
        form.setDepartment(departmentOptional.get());
        form.setTitle(title);
        form.setDescription(blankToNull(input.description()));
        form.setStatus(FormStatus.DRAFT);
        form.setCurrentVersionNumber("1.0.0");
        form.setFormData(serializedFormData);

        DepartmentForm saved = departmentFormRepository.save(form);

        DepartmentFormVersion version = new DepartmentFormVersion();
        version.setForm(saved);
        version.setVersionNumber(saved.getCurrentVersionNumber());
        version.setStatus(FormStatus.DRAFT);
        version.setTitle(saved.getTitle());
        version.setDescription(saved.getDescription());
        version.setFormData(saved.getFormData());
        departmentFormVersionRepository.save(version);

        return ApiResponse.success("Form created.", formToMap(saved));
    }

    @Transactional
    public ApiResponse updateForm(UUID departmentId, UUID formId, FormInput input) {
        if (departmentId == null || formId == null || input == null) {
            return ApiResponse.error("departmentId, formId and input are required.", "VALIDATION_ERROR");
        }

        Optional<DepartmentForm> formOptional = departmentFormRepository.findByIdAndDepartmentId(formId, departmentId);
        if (formOptional.isEmpty()) {
            return ApiResponse.error("Form not found.", "NOT_FOUND");
        }

        DepartmentForm form = formOptional.get();
        String title = requiredTrim(input.title());
        if (title == null) {
            return ApiResponse.error("title is required.", "VALIDATION_ERROR");
        }

        String serializedFormData = serializeFormData(input);
        if (serializedFormData == null) {
            return ApiResponse.error("Form definition is invalid.", "VALIDATION_ERROR");
        }

        form.setTitle(title);
        form.setDescription(blankToNull(input.description()));

        if (form.getStatus() == FormStatus.FINAL) {
            String nextVersion = nextMajorVersion(form.getCurrentVersionNumber());
            form.setCurrentVersionNumber(nextVersion);
            form.setStatus(FormStatus.DRAFT);
            form.setFormData(serializedFormData);

            DepartmentFormVersion version = new DepartmentFormVersion();
            version.setForm(form);
            version.setVersionNumber(nextVersion);
            version.setStatus(FormStatus.DRAFT);
            version.setTitle(form.getTitle());
            version.setDescription(form.getDescription());
            version.setFormData(serializedFormData);
            departmentFormVersionRepository.save(version);
        } else {
            form.setFormData(serializedFormData);

            Optional<DepartmentFormVersion> existingVersionOptional = departmentFormVersionRepository
                    .findByFormIdAndVersionNumber(form.getId(), form.getCurrentVersionNumber());

            DepartmentFormVersion version = existingVersionOptional.orElseGet(DepartmentFormVersion::new);
            version.setForm(form);
            version.setVersionNumber(form.getCurrentVersionNumber());
            version.setStatus(FormStatus.DRAFT);
            version.setTitle(form.getTitle());
            version.setDescription(form.getDescription());
            version.setFormData(form.getFormData());
            departmentFormVersionRepository.save(version);
        }

        DepartmentForm saved = departmentFormRepository.save(form);
        return ApiResponse.success("Form updated.", formToMap(saved));
    }

    @Transactional
    public ApiResponse finalizeForm(UUID departmentId, UUID formId) {
        if (departmentId == null || formId == null) {
            return ApiResponse.error("departmentId and formId are required.", "VALIDATION_ERROR");
        }

        Optional<DepartmentForm> formOptional = departmentFormRepository.findByIdAndDepartmentId(formId, departmentId);
        if (formOptional.isEmpty()) {
            return ApiResponse.error("Form not found.", "NOT_FOUND");
        }

        DepartmentForm form = formOptional.get();
        if (form.getStatus() == FormStatus.FINAL) {
            return ApiResponse.success("Form already finalized.", formToMap(form));
        }

        form.setStatus(FormStatus.FINAL);

        Optional<DepartmentFormVersion> currentVersionOptional = departmentFormVersionRepository
                .findByFormIdAndVersionNumber(form.getId(), form.getCurrentVersionNumber());

        DepartmentFormVersion version = currentVersionOptional.orElseGet(DepartmentFormVersion::new);
        version.setForm(form);
        version.setVersionNumber(form.getCurrentVersionNumber());
        version.setStatus(FormStatus.FINAL);
        version.setTitle(form.getTitle());
        version.setDescription(form.getDescription());
        version.setFormData(form.getFormData());
        departmentFormVersionRepository.save(version);

        DepartmentForm saved = departmentFormRepository.save(form);
        return ApiResponse.success("Form finalized.", formToMap(saved));
    }

    @Transactional(readOnly = true)
    public ApiResponse getForms(UUID departmentId) {
        if (departmentId == null) {
            return ApiResponse.error("departmentId is required.", "VALIDATION_ERROR");
        }

        if (!departmentRepository.existsById(departmentId)) {
            return ApiResponse.error("Department not found.", "NOT_FOUND");
        }

        List<Map<String, Object>> forms = departmentFormRepository.findByDepartmentIdOrderByUpdatedAtDesc(departmentId)
                .stream()
                .map(this::formToMap)
                .toList();

        return ApiResponse.success("Forms fetched.", forms);
    }

    @Transactional(readOnly = true)
    public ApiResponse getForm(UUID departmentId, UUID formId) {
        if (departmentId == null || formId == null) {
            return ApiResponse.error("departmentId and formId are required.", "VALIDATION_ERROR");
        }

        Optional<DepartmentForm> formOptional = departmentFormRepository.findByIdAndDepartmentId(formId, departmentId);
        if (formOptional.isEmpty()) {
            return ApiResponse.error("Form not found.", "NOT_FOUND");
        }

        return ApiResponse.success("Form fetched.", formToMap(formOptional.get()));
    }

    @Transactional(readOnly = true)
    public ApiResponse getLatestForm(UUID departmentId) {
        if (departmentId == null) {
            return ApiResponse.error("departmentId is required.", "VALIDATION_ERROR");
        }

        if (!departmentRepository.existsById(departmentId)) {
            return ApiResponse.error("Department not found.", "NOT_FOUND");
        }

        Optional<DepartmentForm> latest = departmentFormRepository.findTopByDepartmentIdOrderByUpdatedAtDesc(departmentId);
        if (latest.isEmpty()) {
            return ApiResponse.error("No forms found for this department.", "NOT_FOUND");
        }

        return ApiResponse.success("Latest form fetched.", formToMap(latest.get()));
    }

    @Transactional(readOnly = true)
    public ApiResponse getFormVersionHistory(UUID departmentId, UUID formId) {
        if (departmentId == null || formId == null) {
            return ApiResponse.error("departmentId and formId are required.", "VALIDATION_ERROR");
        }

        Optional<DepartmentForm> formOptional = departmentFormRepository.findByIdAndDepartmentId(formId, departmentId);
        if (formOptional.isEmpty()) {
            return ApiResponse.error("Form not found.", "NOT_FOUND");
        }

        List<Map<String, Object>> versions = departmentFormVersionRepository.findByFormIdOrderByCreatedAtDesc(formId)
                .stream()
                .map(this::formVersionToMap)
                .toList();

        return ApiResponse.success("Form version history fetched.", versions);
    }

    @Transactional(readOnly = true)
    public ApiResponse getFormVersion(UUID departmentId, UUID formId, String versionNumber) {
        if (departmentId == null || formId == null || versionNumber == null || versionNumber.isBlank()) {
            return ApiResponse.error("departmentId, formId and versionNumber are required.", "VALIDATION_ERROR");
        }

        Optional<DepartmentForm> formOptional = departmentFormRepository.findByIdAndDepartmentId(formId, departmentId);
        if (formOptional.isEmpty()) {
            return ApiResponse.error("Form not found.", "NOT_FOUND");
        }

        Optional<DepartmentFormVersion> versionOptional = departmentFormVersionRepository
                .findByFormIdAndVersionNumber(formId, versionNumber.trim());

        if (versionOptional.isEmpty()) {
            return ApiResponse.error("Form version not found.", "NOT_FOUND");
        }

        return ApiResponse.success("Form version fetched.", formVersionToMap(versionOptional.get()));
    }

    @Transactional
    public ApiResponse upsertConsultationAnswers(ConsultationAnswersInput input, AuthenticatedUser authUser) {
        if (input == null) {
            return ApiResponse.error("input is required.", "VALIDATION_ERROR");
        }

        if (input.consultationId() == null
                || input.visitId() == null
                || input.patientId() == null
                || input.departmentId() == null
                || input.formId() == null) {
            return ApiResponse.error("consultationId, visitId, patientId, departmentId and formId are required.", "VALIDATION_ERROR");
        }

        Optional<DepartmentForm> formOptional = departmentFormRepository.findByIdAndDepartmentId(input.formId(), input.departmentId());
        if (formOptional.isEmpty()) {
            return ApiResponse.error("Form not found for department.", "NOT_FOUND");
        }

        DepartmentForm form = formOptional.get();

        String targetVersion = blankToNull(input.formVersion());
        if (targetVersion == null) {
            targetVersion = resolveLatestUsableFormVersion(form);
        }

        Optional<DepartmentFormVersion> versionOptional = departmentFormVersionRepository.findByFormIdAndVersionNumber(form.getId(), targetVersion);
        if (versionOptional.isEmpty()) {
            return ApiResponse.error("Requested form version does not exist.", "NOT_FOUND");
        }

        if (input.status() == AnswerStatus.FINAL && versionOptional.get().getStatus() != FormStatus.FINAL) {
            return ApiResponse.error("Cannot submit FINAL answers against a non-finalized form version.", "VALIDATION_ERROR");
        }

        if (!isValidJson(input.answers())) {
            return ApiResponse.error("answers must be valid JSON.", "VALIDATION_ERROR");
        }

        Optional<ConsultationAnswer> existingAnswerOptional = consultationAnswerRepository
            .findByConsultationIdAndFormIdAndFormVersion(input.consultationId(), input.formId(), targetVersion);
        ConsultationAnswer answer = existingAnswerOptional.orElseGet(ConsultationAnswer::new);

        AnswerStatus resolvedStatus = input.status();
        if (resolvedStatus == null) {
            resolvedStatus = existingAnswerOptional.map(ConsultationAnswer::getStatus).orElse(AnswerStatus.DRAFT);
        }

        String resolvedAnswers = input.answers();
        if (resolvedAnswers == null || resolvedAnswers.isBlank()) {
            if (existingAnswerOptional.isEmpty()) {
                return ApiResponse.error("answers is required when creating a new consultation answer.", "VALIDATION_ERROR");
            }
            resolvedAnswers = existingAnswerOptional.get().getAnswers();
        }

        answer.setConsultationId(input.consultationId());
        answer.setVisitId(input.visitId());
        answer.setPatientId(input.patientId());
        answer.setDepartment(form.getDepartment());
        answer.setForm(form);
        answer.setFormVersion(targetVersion);
        answer.setStatus(resolvedStatus);
        answer.setAnswers(resolvedAnswers.trim());

        if (resolvedStatus == AnswerStatus.FINAL && answer.getSubmittedAt() == null) {
            answer.setSubmittedAt(LocalDateTime.now());
        }

        if (authUser != null && authUser.userId() != null) {
            Optional<Worker> workerOptional = workerRepository.findById(authUser.userId());
            workerOptional.ifPresent(answer::setSubmittedBy);
        }

        ConsultationAnswer saved = consultationAnswerRepository.save(answer);
        return ApiResponse.success("Consultation answers upserted.", List.of(consultationAnswerToMap(saved)));
    }

    @Transactional(readOnly = true)
    public ApiResponse getConsultationAnswers(UUID visitDepartmentId, UUID visitId, UUID departmentId) {
        UUID resolvedVisitId = null;
        UUID resolvedDepartmentId = null;

        if (visitDepartmentId != null) {
            Optional<VisitDepartment> visitDepartmentOptional = visitDepartmentRepository.findById(visitDepartmentId);
            if (visitDepartmentOptional.isEmpty()) {
                return ApiResponse.error("Visit department not found.", "NOT_FOUND");
            }
            VisitDepartment visitDepartment = visitDepartmentOptional.get();
            resolvedVisitId = visitDepartment.getVisit().getId();
            resolvedDepartmentId = visitDepartment.getDepartment().getId();
        } else {
            if (visitId == null || departmentId == null) {
                return ApiResponse.error("Either visitDepartmentId or both visitId and departmentId are required.", "VALIDATION_ERROR");
            }
            resolvedVisitId = visitId;
            resolvedDepartmentId = departmentId;
        }

        if (!departmentRepository.existsById(resolvedDepartmentId)) {
            return ApiResponse.error("Department not found.", "NOT_FOUND");
        }

        List<ConsultationAnswer> answers = consultationAnswerRepository
                .findByVisitIdAndDepartmentIdOrderByUpdatedAtDesc(resolvedVisitId, resolvedDepartmentId);

        if (answers.isEmpty()) {
            // No answers found, get the latest form for this department and return it with null answers
            Optional<DepartmentForm> latestForm = departmentFormRepository.findTopByDepartmentIdOrderByUpdatedAtDesc(resolvedDepartmentId);
            if (latestForm.isEmpty()) {
                return ApiResponse.success("No consultation answers found. No forms available for this department.", null);
            }
            // Return latest form as dedicated form with null answers
            Map<String, Object> dedicatedForm = formToMap(latestForm.get());
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("dedicatedForm", dedicatedForm);
            payload.put("answers", null);
            return ApiResponse.success("No consultation answers found. Returning latest form.", payload);
        }

        List<Map<String, Object>> payload = answers.stream()
                .map(this::consultationAnswerToMap)
                .toList();

        return ApiResponse.success("Consultation answers fetched.", payload);
    }

    private String resolveLatestUsableFormVersion(DepartmentForm form) {
        Optional<DepartmentFormVersion> latestFinalVersion = departmentFormVersionRepository
                .findTopByFormIdAndStatusOrderByCreatedAtDesc(form.getId(), FormStatus.FINAL);

        if (latestFinalVersion.isPresent()) {
            return latestFinalVersion.get().getVersionNumber();
        }

        return form.getCurrentVersionNumber();
    }

    private Map<String, Object> formToMap(DepartmentForm form) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("id", form.getId());
        data.put("departmentId", form.getDepartment().getId());
        data.put("title", form.getTitle());
        data.put("description", form.getDescription());
        data.put("status", form.getStatus());
        data.put("version", form.getCurrentVersionNumber());
        data.put("createdAt", form.getCreatedAt());
        data.put("updatedAt", form.getUpdatedAt());

        Map<String, Object> formData = parseJsonMap(form.getFormData());
        data.put("sections", mapList(formData.get("sections")));
        data.put("fields", mapList(formData.get("fields")));
        data.put("actions", mapList(formData.get("actions")));

        return data;
    }

    private Map<String, Object> formVersionToMap(DepartmentFormVersion version) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("id", version.getId());
        data.put("formId", version.getForm().getId());
        data.put("departmentId", version.getForm().getDepartment().getId());
        data.put("title", version.getTitle());
        data.put("description", version.getDescription());
        data.put("status", version.getStatus());
        data.put("version", version.getVersionNumber());
        data.put("createdAt", version.getCreatedAt());
        data.put("updatedAt", version.getUpdatedAt());

        Map<String, Object> formData = parseJsonMap(version.getFormData());
        data.put("sections", mapList(formData.get("sections")));
        data.put("fields", mapList(formData.get("fields")));
        data.put("actions", mapList(formData.get("actions")));

        return data;
    }

    private Map<String, Object> consultationAnswerToMap(ConsultationAnswer answer) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("id", answer.getId());
        data.put("consultationId", answer.getConsultationId());
        data.put("visitId", answer.getVisitId());
        data.put("patientId", answer.getPatientId());
        data.put("departmentId", answer.getDepartment().getId());
        data.put("status", answer.getStatus());
        data.put("answers", answer.getAnswers());
        data.put("submittedAt", answer.getSubmittedAt());
        data.put("updatedAt", answer.getUpdatedAt());
        data.put("dedicatedForm", dedicatedFormToMap(answer));
        return data;
    }

    private Map<String, Object> dedicatedFormToMap(ConsultationAnswer answer) {
        Optional<DepartmentFormVersion> answerVersionOptional = departmentFormVersionRepository
                .findByFormIdAndVersionNumber(answer.getForm().getId(), answer.getFormVersion());

        if (answerVersionOptional.isPresent()) {
            DepartmentFormVersion version = answerVersionOptional.get();
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("id", version.getForm().getId());
            data.put("departmentId", version.getForm().getDepartment().getId());
            data.put("title", version.getTitle());
            data.put("description", version.getDescription());
            data.put("status", version.getStatus());
            data.put("version", version.getVersionNumber());
            data.put("createdAt", version.getCreatedAt());
            data.put("updatedAt", version.getUpdatedAt());

            Map<String, Object> formData = parseJsonMap(version.getFormData());
            data.put("sections", mapList(formData.get("sections")));
            data.put("fields", mapList(formData.get("fields")));
            data.put("actions", mapList(formData.get("actions")));
            return data;
        }

        return formToMap(answer.getForm());
    }

    private String serializeFormData(FormInput input) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("fields", normalizeFields(input.fields()));
        payload.put("sections", normalizeSections(input.sections()));
        payload.put("actions", normalizeActions(input.actions()));

        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException ex) {
            return null;
        }
    }

    private List<Map<String, Object>> normalizeFields(List<FormFieldInput> fields) {
        if (fields == null) {
            return List.of();
        }

        List<Map<String, Object>> normalized = new ArrayList<>();
        for (FormFieldInput field : fields) {
            if (field == null) {
                continue;
            }
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", field.id());
            row.put("label", field.label());
            row.put("type", field.type());
            row.put("placeholder", field.placeholder());
            row.put("required", bool(field.required()));
            row.put("order", field.order() == null ? 0 : field.order());
            row.put("hideLabel", bool(field.hideLabel()));
            row.put("boldLabel", bool(field.boldLabel()));
            row.put("italicLabel", bool(field.italicLabel()));
            row.put("underlineLabel", bool(field.underlineLabel()));
            row.put("centerLabel", bool(field.centerLabel()));
            row.put("options", field.options() == null ? List.of() : field.options());
            row.put("tableConfig", normalizeTableConfig(field.tableConfig()));
            row.put("labRecordConfig", normalizeLabRecordConfig(field.labRecordConfig()));
            row.put("conditionalRendering", normalizeConditionalRendering(field.conditionalRendering()));
            normalized.add(row);
        }
        return normalized;
    }

    private List<Map<String, Object>> normalizeSections(List<FormSectionInput> sections) {
        if (sections == null) {
            return List.of();
        }

        List<Map<String, Object>> normalized = new ArrayList<>();
        for (FormSectionInput section : sections) {
            if (section == null) {
                continue;
            }
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", section.id());
            row.put("title", section.title());
            row.put("boldTitle", bool(section.boldTitle()));
            row.put("italicTitle", bool(section.italicTitle()));
            row.put("underlineTitle", bool(section.underlineTitle()));
            row.put("centerTitle", bool(section.centerTitle()));
            row.put("columns", section.columns() == null ? 1 : section.columns());
            row.put("order", section.order() == null ? 0 : section.order());
            row.put("fields", normalizeFields(section.fields()));
            normalized.add(row);
        }
        return normalized;
    }

    private List<Map<String, Object>> normalizeActions(List<FormActionInput> actions) {
        if (actions == null) {
            return List.of();
        }

        List<Map<String, Object>> normalized = new ArrayList<>();
        for (FormActionInput action : actions) {
            if (action == null) {
                continue;
            }
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", action.id());
            row.put("name", action.name());
            row.put("type", action.type());
            row.put("quantity", action.quantity() == null ? 1 : Math.max(action.quantity(), 1));
            row.put("price", action.price() == null ? BigDecimal.ZERO : action.price());
            row.put("isQuantifiable", bool(action.isQuantifiable()));
            row.put("backendId", action.backendId());
            normalized.add(row);
        }
        return normalized;
    }

    private Map<String, Object> normalizeTableConfig(TableConfigInput config) {
        if (config == null) {
            return null;
        }

        Map<String, Object> row = new LinkedHashMap<>();
        row.put("mode", config.mode());
        row.put("rows", config.rows());
        row.put("columns", config.columns());
        row.put("headerPlacement", config.headerPlacement());
        row.put("columnHeaders", config.columnHeaders() == null ? List.of() : config.columnHeaders());
        row.put("rowHeaders", config.rowHeaders() == null ? List.of() : config.rowHeaders());
        return row;
    }

    private Map<String, Object> normalizeLabRecordConfig(LabRecordConfigInput config) {
        if (config == null) {
            return null;
        }

        Map<String, Object> row = new LinkedHashMap<>();
        row.put("layout", config.layout());
        row.put("rows", config.rows() == null ? List.of() : config.rows().stream()
                .filter(java.util.Objects::nonNull)
                .map(this::normalizeLabRecordRow)
                .toList());
        return row;
    }

    private Map<String, Object> normalizeLabRecordRow(LabRecordRowInput rowInput) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", rowInput.id());
        row.put("name", rowInput.name());
        row.put("unitMode", rowInput.unitMode());
        row.put("unitOptions", rowInput.unitOptions() == null ? List.of() : rowInput.unitOptions());
        row.put("defaultUnit", rowInput.defaultUnit());
        row.put("resultOptions", rowInput.resultOptions() == null ? List.of() : rowInput.resultOptions());
        return row;
    }

    private Map<String, Object> normalizeConditionalRendering(ConditionalRenderingInput conditionalRendering) {
        if (conditionalRendering == null) {
            return null;
        }

        Map<String, Object> row = new LinkedHashMap<>();
        row.put("dependsOn", conditionalRendering.dependsOn());
        row.put("condition", conditionalRendering.condition());
        row.put("value", conditionalRendering.value());
        row.put("itemType", conditionalRendering.itemType());
        return row;
    }

    private Map<String, Object> parseJsonMap(String json) {
        String raw = (json == null || json.isBlank()) ? "{}" : json;
        try {
            return objectMapper.readValue(raw, MAP_TYPE);
        } catch (JsonProcessingException ex) {
            return Map.of();
        }
    }

    private List<Map<String, Object>> mapList(Object value) {
        if (!(value instanceof List<?> list)) {
            return List.of();
        }

        List<Map<String, Object>> mapped = new ArrayList<>();
        for (Object item : list) {
            if (item instanceof Map<?, ?> itemMap) {
                Map<String, Object> row = new LinkedHashMap<>();
                for (Map.Entry<?, ?> entry : itemMap.entrySet()) {
                    row.put(String.valueOf(entry.getKey()), entry.getValue());
                }
                mapped.add(row);
            }
        }
        return mapped;
    }

    private String nextMajorVersion(String current) {
        if (current == null || current.isBlank()) {
            return "2.0.0";
        }
        String[] parts = current.trim().split("\\.");
        try {
            int major = Integer.parseInt(parts[0]);
            return (major + 1) + ".0.0";
        } catch (NumberFormatException ex) {
            return "2.0.0";
        }
    }

    private boolean isValidJson(String json) {
        try {
            objectMapper.readTree(json);
            return true;
        } catch (JsonProcessingException ex) {
            return false;
        }
    }

    private String requiredTrim(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private boolean bool(Boolean value) {
        return value != null && value;
    }
}
