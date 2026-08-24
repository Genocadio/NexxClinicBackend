package com.nexxserve.nexxclinic.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.meilisearch.sdk.Client;
import com.meilisearch.sdk.Config;
import com.meilisearch.sdk.Index;
import com.meilisearch.sdk.SearchRequest;
import com.meilisearch.sdk.exceptions.MeilisearchException;
import com.meilisearch.sdk.model.IndexStats;
import com.meilisearch.sdk.model.SearchResult;
import com.meilisearch.sdk.model.Searchable;
import com.meilisearch.sdk.model.Settings;
import com.meilisearch.sdk.model.TaskInfo;
import com.nexxserve.nexxclinic.config.MeilisearchProperties;
import com.nexxserve.nexxclinic.entity.Patient;
import com.nexxserve.nexxclinic.entity.Product;
import com.nexxserve.nexxclinic.entity.Worker;
import com.nexxserve.nexxclinic.model.ProductType;
import com.nexxserve.nexxclinic.model.RoleName;
import com.nexxserve.nexxclinic.repository.PatientInsuranceRepository;
import com.nexxserve.nexxclinic.repository.PatientRepository;
import com.nexxserve.nexxclinic.repository.ProductRepository;
import com.nexxserve.nexxclinic.repository.WorkerRepository;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Facade over Meilisearch for the {@code products}, {@code patients} and
 * {@code workers} indexes.
 *
 * <p>Every search returns only the matching document ids (plus the estimated
 * total hit count); the callers hydrate the full DTOs from the database, so the
 * GraphQL response shape stays identical whether the search ran in Meilisearch
 * or fell back to the database.
 */
@Service
public class MeilisearchIndexService {

    private static final Logger log = LoggerFactory.getLogger(MeilisearchIndexService.class);

    public static final String PRODUCTS_INDEX = "products";
    public static final String PATIENTS_INDEX = "patients";
    public static final String WORKERS_INDEX = "workers";

    private static final int SYNC_BATCH_SIZE = 1000;
    /** Hard Meilisearch per-request limit; worker search has no pagination, so cap at the max. */
    private static final int WORKER_SEARCH_LIMIT = 1000;

    private final MeilisearchProperties props;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final ProductRepository productRepository;
    private final PatientRepository patientRepository;
    private final PatientInsuranceRepository patientInsuranceRepository;
    private final WorkerRepository workerRepository;

    private volatile Client client;

    public MeilisearchIndexService(
            MeilisearchProperties props,
            ProductRepository productRepository,
            PatientRepository patientRepository,
            PatientInsuranceRepository patientInsuranceRepository,
            WorkerRepository workerRepository
    ) {
        this.props = props;
        this.productRepository = productRepository;
        this.patientRepository = patientRepository;
        this.patientInsuranceRepository = patientInsuranceRepository;
        this.workerRepository = workerRepository;
    }

    public boolean isEnabled() {
        return props.isEnabled();
    }

    // ─────────────────────────────────────────────────────────────
    // Client bootstrap
    // ─────────────────────────────────────────────────────────────

    private Client client() {
        Client current = client;
        if (current == null) {
            synchronized (this) {
                if (client == null) {
                    client = new Client(new Config(props.getUrl(), props.getApiKey()));
                }
                current = client;
            }
        }
        return current;
    }

    /**
     * Creates the three indexes (if missing) and applies their settings
     * (searchable/filterable attributes). Best-effort: failures are logged and
     * swallowed so startup never breaks because Meilisearch is down.
     */
    public void ensureIndexes() {
        if (!isEnabled()) {
            return;
        }
        try {
            ensureIndex(PRODUCTS_INDEX, new String[]{"name", "genericName", "code", "description"},
                    new String[]{"type"});
            ensureIndex(PATIENTS_INDEX,
                    new String[]{"fullName", "patientIdentifier", "firstName", "lastName", "primaryPhoneNumber", "alternativePhone", "nationalIdNumber", "passportNumber"},
                    new String[]{"insuranceProviderIds", "gender", "dateOfBirth", "createdAt"});
            ensureIndex(WORKERS_INDEX,
                    new String[]{"firstName", "lastName", "username", "email", "phoneNumber"},
                    new String[]{"roles", "active", "accountStatus", "departmentIds", "createdAt"});
        } catch (Exception e) {
            log.warn("Meilisearch index bootstrap failed: {}", e.getMessage());
        }
    }

    private void ensureIndex(String uid, String[] searchable, String[] filterable) throws Exception {
        // Always delete + recreate to guarantee the primary key is 'id'.
        // Meilisearch can't auto-detect the primary key when fields like
        // "notPaid" end with 'id', and the Java SDK's getPrimaryKey() reads
        // a cached field that is never populated after createIndex().
        try {
            client().index(uid).getStats(); // succeeds if index exists
            log.info("Deleting existing Meilisearch index '{}' for fresh creation…", uid);
            waitFor(client().index(uid), client().deleteIndex(uid));
            Thread.sleep(500); // small grace period for Meilisearch to finish deletion
        } catch (Exception e) {
            // Index doesn't exist yet — fine
        }
        TaskInfo createTask = client().createIndex(uid, "id");
        waitFor(client().index(uid), createTask);
        // Verify the primary key was actually set
        Index created = client().index(uid);
        created.fetchPrimaryKey();
        if (created.getPrimaryKey() == null) {
            log.warn("Meilisearch index '{}' created but primaryKey not confirmed; adding docs with explicit PK", uid);
        }
        // Apply searchable/filterable settings
        Settings settings = new Settings();
        settings.setSearchableAttributes(searchable);
        settings.setFilterableAttributes(filterable);
        TaskInfo task = created.updateSettings(settings);
        waitFor(created, task);
    }

    private void waitFor(Index index, TaskInfo task) {
        if (task == null) {
            return;
        }
        try {
            index.waitForTask(task.getTaskUid());
        } catch (MeilisearchException e) {
            log.debug("Meilisearch task {} not confirmed: {}", task.getTaskUid(), e.getMessage());
        }
    }

    // ─────────────────────────────────────────────────────────────
    // Search
    // ─────────────────────────────────────────────────────────────

    public record SearchHit(List<UUID> ids, long total) {
    }

    public SearchHit searchProducts(String query, ProductType type, int page, int size) {
        List<String> filters = new ArrayList<>();
        if (type != null) {
            filters.add("type = \"" + type.name() + "\"");
        }
        return search(PRODUCTS_INDEX, query, filters, page, size);
    }

    public SearchHit searchPatients(
            String query,
            String phoneNumber,
            UUID insuranceProviderId,
            Integer exactAge,
            Integer minAge,
            Integer maxAge,
            int page,
            int size
    ) {
        List<String> filters = new ArrayList<>();

        String q = query;
        if (phoneNumber != null && !phoneNumber.isBlank()) {
            q = (q == null || q.isBlank()) ? phoneNumber : q + " " + phoneNumber;
        }

        if (insuranceProviderId != null) {
            filters.add("insuranceProviderIds = \"" + insuranceProviderId + "\"");
        }

        // Age filters are translated into dateOfBirth ranges (same math the DB path uses).
        LocalDate today = LocalDate.now();
        long[] range = ageRange(exactAge, minAge, maxAge, today);
        if (range[0] != Long.MIN_VALUE) {
            filters.add("dateOfBirth >= " + range[0]);
        }
        if (range[1] != Long.MAX_VALUE) {
            filters.add("dateOfBirth <= " + range[1]);
        }

        return search(PATIENTS_INDEX, q, filters, page, size);
    }

    public SearchHit searchWorkers(String name, RoleName role, Boolean activeOnly, UUID departmentId) {
        List<String> filters = new ArrayList<>();
        if (role != null) {
            filters.add("roles = \"" + role.name() + "\"");
        }
        if (activeOnly != null) {
            filters.add("active = " + activeOnly);
        }
        if (departmentId != null) {
            filters.add("departmentIds = \"" + departmentId + "\"");
        }
        return search(WORKERS_INDEX, name, filters, 0, WORKER_SEARCH_LIMIT);
    }

    private SearchHit search(String uid, String query, List<String> filters, int page, int size) {
        if (!isEnabled()) {
            return new SearchHit(List.of(), 0);
        }
        SearchRequest.SearchRequestBuilder builder = SearchRequest.builder()
                .q(query == null ? "" : query)
                .offset(page * size)
                .limit(size)
                .attributesToRetrieve(new String[]{"id"});
        if (!filters.isEmpty()) {
            builder.filter(filters.toArray(new String[0]));
        }
        try {
            Searchable searchable = client().index(uid).search(builder.build());
            if (!(searchable instanceof SearchResult result)) {
                return new SearchHit(List.of(), 0);
            }
            List<UUID> ids = new ArrayList<>();
            for (Map<String, Object> hit : result.getHits()) {
                Object id = hit.get("id");
                if (id != null) {
                    try {
                        ids.add(UUID.fromString(id.toString()));
                    } catch (IllegalArgumentException ignored) {
                        // skip malformed ids
                    }
                }
            }
            return new SearchHit(ids, result.getEstimatedTotalHits());
        } catch (MeilisearchException e) {
            // Surface the failure so callers can fall back to the database path.
            log.warn("Meilisearch search on {} failed: {}", uid, e.getMessage());
            throw new SearchUnavailableException(e);
        }
    }

    /** Thrown when Meilisearch is unreachable/errors; callers fall back to the DB. */
    public static class SearchUnavailableException extends RuntimeException {
        public SearchUnavailableException(Throwable cause) {
            super(cause);
        }
    }

    /**
     * Computes the dateOfBirth epoch-millis range for age filters.
     * Returns {lower, upper}; MIN_VALUE/MAX_VALUE mean "unbounded".
     */
    private long[] ageRange(Integer exactAge, Integer minAge, Integer maxAge, LocalDate today) {
        long lower = Long.MIN_VALUE;
        long upper = Long.MAX_VALUE;

        if (exactAge != null) {
            long dobUpper = toEpochDay(today.minusYears(exactAge));
            long dobLower = toEpochDay(today.minusYears(exactAge + 1L).plusDays(1));
            return new long[]{dobLower, dobUpper};
        }
        if (minAge != null) {
            upper = toEpochDay(today.minusYears(minAge));
        }
        if (maxAge != null) {
            lower = toEpochDay(today.minusYears(maxAge + 1L).plusDays(1));
        }
        return new long[]{lower, upper};
    }

    private long toEpochDay(LocalDate date) {
        return date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
    }

    // ─────────────────────────────────────────────────────────────
    // Document sync (single entity)
    // ─────────────────────────────────────────────────────────────

    public void indexProduct(Product product) {
        if (product == null) {
            return;
        }
        Map<String, Object> doc = new LinkedHashMap<>();
        doc.put("id", product.getId().toString());
        doc.put("name", product.getName());
        doc.put("genericName", product.getGenericName());
        doc.put("code", product.getCode());
        doc.put("description", product.getDescription());
        doc.put("type", product.getType() == null ? null : product.getType().name());
        doc.put("notPaid", product.isNotPaid());
        upsert(PRODUCTS_INDEX, doc);
    }

    public void indexPatient(Patient patient) {
        if (patient == null) {
            return;
        }
        Map<String, Object> doc = patientDocument(patient, insuranceProviderIdsFor(patient.getId()));
        upsert(PATIENTS_INDEX, doc);
    }

    public void indexPatient(UUID patientId) {
        patientRepository.findById(patientId).ifPresent(this::indexPatient);
    }

    public void indexWorker(Worker worker) {
        if (worker == null) {
            return;
        }
        Map<String, Object> doc = workerDocument(worker);
        upsert(WORKERS_INDEX, doc);
    }

    public void deletePatient(UUID id) {
        deleteById(PATIENTS_INDEX, id);
    }

    private Set<UUID> insuranceProviderIdsFor(UUID patientId) {
        return patientInsuranceRepository.findByPatientId(patientId)
                .stream()
                // Deactivated policies are no longer applicable, so they must not
                // match a provider filter in patient search.
                .filter(pi -> !pi.isDeactivated())
                .map(pi -> pi.getInsuranceProvider().getId())
                .collect(Collectors.toSet());
    }

    private Map<String, Object> patientDocument(Patient patient, Set<UUID> insuranceProviderIds) {
        Map<String, Object> doc = new LinkedHashMap<>();
        doc.put("id", patient.getId().toString());
        doc.put("patientIdentifier", patient.getPatientIdentifier());
        doc.put("firstName", patient.getFirstName());
        doc.put("lastName", patient.getLastName());
        doc.put("fullName", patient.getFullName());
        doc.put("primaryPhoneNumber", patient.getPrimaryPhoneNumber());
        doc.put("alternativePhone", patient.getAlternativePhone());
        doc.put("nationalIdNumber", patient.getNationalIdNumber());
        doc.put("passportNumber", patient.getPassportNumber());
        doc.put("dateOfBirth", patient.getDateOfBirth() == null ? null : toEpochDay(patient.getDateOfBirth()));
        doc.put("gender", patient.getGender() == null ? null : patient.getGender().name());
        doc.put("insuranceProviderIds", insuranceProviderIds == null
                ? List.of()
                : insuranceProviderIds.stream().map(UUID::toString).toList());
        doc.put("createdAt", patient.getCreatedAt() == null ? null : toEpochDay(patient.getCreatedAt().toLocalDate()));
        return doc;
    }

    private Map<String, Object> workerDocument(Worker worker) {
        Map<String, Object> doc = new LinkedHashMap<>();
        doc.put("id", worker.getId().toString());
        doc.put("firstName", worker.getFirstName());
        doc.put("lastName", worker.getLastName());
        doc.put("username", worker.getUsername());
        doc.put("email", worker.getEmail());
        doc.put("phoneNumber", worker.getPhoneNumber());
        doc.put("active", worker.isActive());
        doc.put("accountStatus", worker.getAccountStatus() == null ? null : worker.getAccountStatus().name());
        doc.put("roles", worker.getRoles() == null ? List.of() : worker.getRoles().stream().map(Enum::name).toList());
        List<String> departmentIds = new ArrayList<>();
        if (worker.getDepartments() != null) {
            worker.getDepartments().forEach(d -> {
                if (d != null && d.getId() != null) {
                    departmentIds.add(d.getId().toString());
                }
            });
        }
        doc.put("departmentIds", departmentIds);
        doc.put("createdAt", worker.getCreatedAt() == null ? null : toEpochDay(worker.getCreatedAt().toLocalDate()));
        return doc;
    }

    private void upsert(String uid, Map<String, Object> doc) {
        if (!isEnabled()) {
            return;
        }
        try {
            Index index = client().index(uid);
            // Pass primary key explicitly — Meilisearch can't auto-detect it
            // when fields like "notPaid" end with "id".
            index.addDocuments(json(List.of(doc)), "id");
        } catch (Exception e) {
            log.warn("Meilisearch upsert to {} failed: {}", uid, e.getMessage());
        }
    }

    private void deleteById(String uid, UUID id) {
        if (!isEnabled()) {
            return;
        }
        try {
            Index index = client().index(uid);
            index.deleteDocument(id.toString());
        } catch (Exception e) {
            log.warn("Meilisearch delete from {} failed: {}", uid, e.getMessage());
        }
    }

    // ─────────────────────────────────────────────────────────────
    // Bulk reindex
    // ─────────────────────────────────────────────────────────────

    /**
     * Rebuilds the given index (or all three when {@code uid} is null) from the
     * database. Used by the admin reindex mutation and on startup when an index
     * is empty.
     */
    public void reindex(String uid) {
        if (!isEnabled()) {
            return;
        }
        try {
            if (uid == null || PRODUCTS_INDEX.equals(uid)) {
                reindexProducts();
            }
            if (uid == null || PATIENTS_INDEX.equals(uid)) {
                reindexPatients();
            }
            if (uid == null || WORKERS_INDEX.equals(uid)) {
                reindexWorkers();
            }
        } catch (Exception e) {
            log.warn("Meilisearch reindex failed: {}", e.getMessage());
        }
    }

    private void reindexProducts() throws Exception {
        Index index = client().index(PRODUCTS_INDEX);
        List<Map<String, Object>> docs = new ArrayList<>();
        for (Product product : productRepository.findAll()) {
            Map<String, Object> doc = new LinkedHashMap<>();
            doc.put("id", product.getId().toString());
            doc.put("name", product.getName());
            doc.put("genericName", product.getGenericName());
            doc.put("code", product.getCode());
            doc.put("description", product.getDescription());
            doc.put("type", product.getType() == null ? null : product.getType().name());
            doc.put("notPaid", product.isNotPaid());
            docs.add(doc);
        }
        replaceAll(index, docs);
    }

    private void reindexPatients() throws Exception {
        Index index = client().index(PATIENTS_INDEX);
        List<Map<String, Object>> docs = new ArrayList<>();
        for (Patient patient : patientRepository.findAll()) {
            docs.add(patientDocument(patient, insuranceProviderIdsFor(patient.getId())));
        }
        replaceAll(index, docs);
    }

    private void reindexWorkers() throws Exception {
        Index index = client().index(WORKERS_INDEX);
        List<Map<String, Object>> docs = new ArrayList<>();
        for (Worker worker : workerRepository.findAll()) {
            docs.add(workerDocument(worker));
        }
        replaceAll(index, docs);
    }

    private void replaceAll(Index index, List<Map<String, Object>> docs) throws Exception {
        TaskInfo wipe = index.deleteAllDocuments();
        waitFor(index, wipe);
        for (int i = 0; i < docs.size(); i += SYNC_BATCH_SIZE) {
            List<Map<String, Object>> batch = docs.subList(i, Math.min(i + SYNC_BATCH_SIZE, docs.size()));
            // Pass primary key explicitly — Meilisearch can't auto-detect it
            // when fields like "notPaid" end with "id".
            TaskInfo task = index.addDocuments(json(batch), "id");
            waitFor(index, task);
        }
    }

    /**
     * True when the index exists and holds at least one document (used to decide
     * whether a startup reindex is needed).
     */
    public boolean hasDocuments(String uid) {
        if (!isEnabled()) {
            return false;
        }
        try {
            IndexStats stats = client().index(uid).getStats();
            return stats != null && stats.getNumberOfDocuments() > 0;
        } catch (Exception e) {
            log.warn("Meilisearch stats for {} failed: {}", uid, e.getMessage());
            return false;
        }
    }

    /**
     * Returns the number of documents currently in the given Meilisearch index,
     * or -1 if the index doesn't exist or Meilisearch is unreachable.
     */
    public long getDocumentCount(String uid) {
        if (!isEnabled()) {
            return -1;
        }
        try {
            IndexStats stats = client().index(uid).getStats();
            return stats != null ? stats.getNumberOfDocuments() : -1;
        } catch (Exception e) {
            log.warn("Meilisearch stats for {} failed: {}", uid, e.getMessage());
            return -1;
        }
    }

    /**
     * Returns the number of rows in the database for the entity behind the given
     * Meilisearch index, or -1 if the index is unknown.
     */
    public long getDbCount(String uid) {
        try {
            if (PRODUCTS_INDEX.equals(uid)) {
                return productRepository.count();
            } else if (PATIENTS_INDEX.equals(uid)) {
                return patientRepository.count();
            } else if (WORKERS_INDEX.equals(uid)) {
                return workerRepository.count();
            }
        } catch (Exception e) {
            log.warn("DB count for {} failed: {}", uid, e.getMessage());
            return -1;
        }
        return -1;
    }

    private String json(List<Map<String, Object>> docs) {
        try {
            return objectMapper.writeValueAsString(docs);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to serialize Meilisearch documents", e);
        }
    }
}
