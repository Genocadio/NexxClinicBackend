package com.nexxserve.nexxclinic.service.billing;

import com.nexxserve.nexxclinic.auth.AuthenticatedUser;
import com.nexxserve.nexxclinic.dto.out.ApiResponse;
import com.nexxserve.nexxclinic.entity.ClinicProfile;
import com.nexxserve.nexxclinic.entity.DepartmentInsuranceBilling;
import com.nexxserve.nexxclinic.entity.Visit;
import com.nexxserve.nexxclinic.entity.Worker;
import com.nexxserve.nexxclinic.repository.ClinicProfileRepository;
import com.nexxserve.nexxclinic.repository.DepartmentInsuranceBillingRepository;
import com.nexxserve.nexxclinic.repository.VisitBillingItemRepository;
import com.nexxserve.nexxclinic.repository.VisitBillingVersionRepository;
import com.nexxserve.nexxclinic.repository.WorkerRepository;
import com.nexxserve.nexxclinic.config.SupabaseProperties;
import com.nexxserve.nexxclinic.service.FileStorageService;
import com.nexxserve.nexxclinic.service.InvoicePdfGenerator;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Generates invoice PDFs for a department insurance billing row.
 *
 * <p>The work is split into three short phases so the slow PDF rendering and the
 * Supabase HTTP upload NEVER hold a DB transaction/connection open:
 * <ol>
 *   <li><b>Snapshot</b> — a short read-only transaction authenticates, validates
 *       and loads every association the renderer touches (via
 *       {@code findByIdWithInvoiceData}) into an {@link InvoiceSnapshot}.</li>
 *   <li><b>Render + upload</b> — runs entirely OUTSIDE any transaction.</li>
 *   <li><b>Persist</b> — a short write transaction stores the object path.</li>
 * </ol>
 * Only {@link IOException} was caught before; a runtime failure inside the renderer
 * or the HTTP client now degrades to a clean error instead of an uncaught 500.
 */
@Component
public class InvoiceGenerator {

    private final DepartmentInsuranceBillingRepository departmentInsuranceBillingRepository;
    private final ClinicProfileRepository clinicProfileRepository;
    private final VisitBillingVersionRepository visitBillingVersionRepository;
    private final VisitBillingItemRepository visitBillingItemRepository;
    private final WorkerRepository workerRepository;
    private final BillingValidation billingValidation;
    private final BillingVersionBuilder billingVersionBuilder;
    private final BillingDataMapper billingDataMapper;
    private final FileStorageService storageService;
    private final SupabaseProperties storageProps;
    private final TransactionTemplate transactionTemplate;
    private final TransactionTemplate readOnlyTransactionTemplate;
    private final int signedUrlExpirySeconds;

    private static final Logger log = LoggerFactory.getLogger(InvoiceGenerator.class);

    public InvoiceGenerator(
        DepartmentInsuranceBillingRepository departmentInsuranceBillingRepository,
        ClinicProfileRepository clinicProfileRepository,
        VisitBillingVersionRepository visitBillingVersionRepository,
        VisitBillingItemRepository visitBillingItemRepository,
        WorkerRepository workerRepository,
        BillingValidation billingValidation,
        BillingVersionBuilder billingVersionBuilder,
        BillingDataMapper billingDataMapper,
        FileStorageService storageService,
        SupabaseProperties storageProps,
        PlatformTransactionManager transactionManager,
        @Value("${billing.invoice.signed-url-expiry-seconds:300}") int signedUrlExpirySeconds
    ) {
        this.departmentInsuranceBillingRepository = departmentInsuranceBillingRepository;
        this.clinicProfileRepository = clinicProfileRepository;
        this.visitBillingVersionRepository = visitBillingVersionRepository;
        this.visitBillingItemRepository = visitBillingItemRepository;
        this.workerRepository = workerRepository;
        this.billingValidation = billingValidation;
        this.billingVersionBuilder = billingVersionBuilder;
        this.billingDataMapper = billingDataMapper;
        this.storageService = storageService;
        this.storageProps = storageProps;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
        this.readOnlyTransactionTemplate = new TransactionTemplate(transactionManager);
        this.readOnlyTransactionTemplate.setReadOnly(true);
        this.signedUrlExpirySeconds = signedUrlExpirySeconds;
    }

    public ApiResponse generateInvoice(
        UUID departmentInsuranceBillingId,
        AuthenticatedUser authUser
    ) {
        if (departmentInsuranceBillingId == null) {
            return ApiResponse.error(
                "departmentInsuranceBillingId is required."
            );
        }

        // Phase 1 — short read-only tx: authenticate, validate, snapshot the data the
        // PDF needs. All lazy associations are eagerly fetched inside the tx.
        InvoiceSnapshot snapshot = readOnlyTransactionTemplate.execute(
            status -> loadInvoiceSnapshot(departmentInsuranceBillingId, authUser)
        );
        if (snapshot == null) {
            return ApiResponse.error("Unable to load invoice data. Please try again.");
        }
        if (snapshot.error() != null) {
            return snapshot.error();
        }

        DepartmentInsuranceBilling billing = snapshot.billing();

        // Invoice already stored — return a fresh download URL (pure IO, no DB tx held).
        if (hasText(billing.getInvoiceUrl())) {
            try {
                String url = resolveDownloadUrl(billing.getInvoiceUrl());
                return ApiResponse.success(
                    "Invoice already exists.",
                    Map.of("signedUrl", url)
                );
            } catch (Exception e) {
                return ApiResponse.error(
                    "Invoice exists but could not generate download URL."
                );
            }
        }

        // Phase 2 — PDF render + upload, entirely OUTSIDE any DB transaction.
        String objectPath;
        try {
            objectPath = generateInvoicePdfFile(snapshot);
        } catch (Exception e) {
            log.error(
                "Invoice generation failed for billing {}: {}",
                departmentInsuranceBillingId,
                e.getMessage(),
                e
            );
            return ApiResponse.error("Failed to generate or upload invoice.");
        }

        // Phase 3 — short write tx: persist the object path so getInvoice can sign it.
        // If the persist fails (or the billing row vanished mid-flight), the uploaded
        // file would be an orphan with no DB reference — clean it up best-effort.
        boolean persisted;
        try {
            persisted = persistInvoiceUrl(departmentInsuranceBillingId, objectPath);
        } catch (Exception e) {
            log.error(
                "Invoice uploaded for billing {} but object path could not be persisted: {}",
                departmentInsuranceBillingId,
                e.getMessage(),
                e
            );
            cleanupOrphanedInvoice(departmentInsuranceBillingId, objectPath);
            return ApiResponse.error(
                "Invoice was generated but could not be saved. Please try again."
            );
        }
        if (!persisted) {
            // The billing row vanished, or a concurrent edit minted a newer version
            // and made it stale, between the snapshot and persist. Do not report
            // success for a file nothing authoritative references.
            log.warn(
                "Invoice {} for billing {} could not be persisted — billing row missing or no longer the latest version; cleaning up upload.",
                objectPath,
                departmentInsuranceBillingId
            );
            cleanupOrphanedInvoice(departmentInsuranceBillingId, objectPath);
            return ApiResponse.error(
                "Invoice could not be saved because the billing record is no longer available or current. Please refresh and try again."
            );
        }

        try {
            String url = resolveDownloadUrl(objectPath);
            return ApiResponse.success(
                "Invoice generated successfully.",
                Map.of("signedUrl", url)
            );
        } catch (Exception e) {
            return ApiResponse.error(
                "Invoice generated but could not create download URL."
            );
        }
    }

    /**
     * Phase-1 helper (runs inside a short read-only transaction): resolves the acting
     * user, runs the unread-notes gate and the latest-version / fully-billed guards,
     * and snapshots everything {@link InvoicePdfGenerator} needs. Returns an
     * {@link InvoiceSnapshot} carrying either a clean error or the loaded data.
     */
    private InvoiceSnapshot loadInvoiceSnapshot(
        UUID departmentInsuranceBillingId,
        AuthenticatedUser authUser
    ) {
        Worker actingUser = resolveWorker(authUser);
        if (actingUser == null) {
            // F3/F4 fix: fail closed (see billOrEditVisitInternal).
            return InvoiceSnapshot.error(
                "Authentication is required to generate an invoice."
            );
        }
        UUID visitId = resolveVisitIdForDepartmentInsuranceBilling(
            departmentInsuranceBillingId
        );
        if (visitId != null) {
            long unreadNotes = billingValidation.countUnreadNotesForVisit(visitId, actingUser);
            if (unreadNotes > 0) {
                return InvoiceSnapshot.error(
                    "You have unread notes. Please read them before generating an invoice."
                );
            }
        }
        Optional<DepartmentInsuranceBilling> billingOptional =
            departmentInsuranceBillingRepository.findByIdWithInvoiceData(
                departmentInsuranceBillingId
            );
        if (billingOptional.isEmpty()) {
            return InvoiceSnapshot.error("Department insurance billing not found.");
        }

        DepartmentInsuranceBilling billing = billingOptional.get();
        Visit visit = billing
            .getVisitDepartmentBilling()
            .getVisitBilling()
            .getVisit();
        if (visit == null) {
            return InvoiceSnapshot.error("Visit not found for billing.");
        }

        // Flow I: invoices are only ever generated for the LATEST billing version.
        // Old versions have their invoiceUrl cleared by editBillVisit (B2 fix); this
        // guard prevents regenerating a fresh PDF from stale data via an old row id.
        if (!isLatestBillingVersion(visit.getId(), billing)) {
            return InvoiceSnapshot.error(
                "Invoices can only be generated for the latest billing version. Use the current billing data."
            );
        }

        if (!billingVersionBuilder.isVisitFullyBilled(visit.getId())) {
            return InvoiceSnapshot.error(
                "Invoice can only be generated after all visit products are billed."
            );
        }

        ClinicProfile clinicProfile = clinicProfileRepository
            .findFirstByOrderByCreatedAtAsc()
            .orElse(null);

        List<Map<String, Object>> items = visitBillingItemRepository
            .findByDepartmentInsuranceBillingIdWithProduct(billing.getId())
            .stream()
            .map(billingDataMapper::visitBillingItemToMap)
            .toList();

        return InvoiceSnapshot.ready(billing, clinicProfile, items);
    }

    /**
     * Phase-2 helper (runs OUTSIDE any DB transaction): renders the invoice PDF to a
     * temp file and uploads it to Supabase Storage. Returns the uploaded object path.
     * Persisting the path is the caller's job ({@link #persistInvoiceUrl}).
     */
    private String generateInvoicePdfFile(
        InvoiceSnapshot snapshot
    ) throws IOException {
        DepartmentInsuranceBilling billing = snapshot.billing();
        ClinicProfile clinicProfile = snapshot.clinicProfile();

        // Render PDF to a temp file
        Path tempFile = Files.createTempFile("invoice-", ".pdf");
        try {
            InvoicePdfGenerator.createInvoicePdf(
                tempFile,
                billing,
                snapshot.items(),
                clinicProfile
            );

            // Upload to Supabase Storage  data/{invoices}/{clinicName?}/invoice-{id}.pdf
            byte[] pdfBytes = Files.readAllBytes(tempFile);
            String clinicName = (clinicProfile != null)
                ? clinicProfile.getName()
                : null;
            String objectPath = buildInvoiceObjectPath(
                clinicName, billing.getId().toString()
            );
            storageService.upload(pdfBytes, invoiceBucket(), objectPath, "application/pdf");
            return objectPath;
        } finally {
            Files.deleteIfExists(tempFile);
        }
    }

    /**
     * Phase-3 helper (runs in its own short write transaction): persists the uploaded
     * object path on the billing row so {@code getInvoice} can sign it later. Kept
     * separate from the render/upload IO so the write never holds a DB connection
     * during the slow PDF/HTTP work. Returns {@code false} when the billing row no
     * longer exists (the caller must not report success).
     */
    private boolean persistInvoiceUrl(
        UUID departmentInsuranceBillingId,
        String objectPath
    ) {
        return transactionTemplate.execute(status -> {
            Optional<DepartmentInsuranceBilling> billingOptional =
                departmentInsuranceBillingRepository.findById(departmentInsuranceBillingId);
            if (billingOptional.isEmpty()) {
                return false;
            }
            DepartmentInsuranceBilling billing = billingOptional.get();
            // TOCTOU guard: the Phase-1 snapshot validated the latest-version rule, but
            // a concurrent editBillVisit may have minted a newer version (and nulled
            // this row's invoice URL) while the PDF was rendered/uploaded. Re-verify
            // inside the persist transaction so stale data never receives a fresh
            // invoice URL — otherwise the B2 "stale invoices are cleared on edit"
            // invariant would be defeated. The caller cleans up the now-orphaned
            // upload and does not report success.
            Visit visit = resolveVisit(billing);
            if (
                visit == null ||
                visit.getId() == null ||
                !isLatestBillingVersion(visit.getId(), billing)
            ) {
                return false;
            }
            billing.setInvoiceUrl(objectPath);
            departmentInsuranceBillingRepository.save(billing);
            return true;
        });
    }

    /**
     * Best-effort removal of an uploaded invoice file that no DB row references
     * (persist failed or the billing row vanished mid-flight). Never throws — the
     * orphan is logged and left for manual cleanup if Supabase is unreachable.
     */
    private void cleanupOrphanedInvoice(
        UUID departmentInsuranceBillingId,
        String objectPath
    ) {
        if (objectPath == null || objectPath.isBlank()) {
            return;
        }
        try {
            storageService.delete(invoiceBucket(), objectPath);
            log.info(
                "Cleaned up orphaned invoice upload {} for billing {}.",
                objectPath,
                departmentInsuranceBillingId
            );
        } catch (Exception e) {
            log.warn(
                "Could not clean up orphaned invoice upload {} for billing {}: {}",
                objectPath,
                departmentInsuranceBillingId,
                e.getMessage()
            );
        }
    }

    private UUID resolveVisitIdForDepartmentInsuranceBilling(UUID departmentInsuranceBillingId) {
        if (departmentInsuranceBillingId == null) {
            return null;
        }
        return departmentInsuranceBillingRepository
            .findByIdWithDepartmentBillingAndVisit(departmentInsuranceBillingId)
            .map(this::resolveVisit)
            .map(Visit::getId)
            .orElse(null);
    }

    /**
     * Null-safe navigation from an insurance billing row to its owning visit.
     * Runs inside an open transaction, so the lazy associations load fine.
     */
    private Visit resolveVisit(DepartmentInsuranceBilling billing) {
        if (billing == null || billing.getVisitDepartmentBilling() == null
                || billing.getVisitDepartmentBilling().getVisitBilling() == null) {
            return null;
        }
        return billing.getVisitDepartmentBilling().getVisitBilling().getVisit();
    }

    /**
     * Whether the given insurance billing row still belongs to the LATEST billing
     * version of its visit. Shared by the Phase-1 guard and the Phase-3 persist
     * re-check so both phases apply the identical rule. Legacy rows with no version
     * are considered current only when the visit has no version rows at all.
     */
    private boolean isLatestBillingVersion(
        UUID visitId,
        DepartmentInsuranceBilling billing
    ) {
        if (visitId == null || billing == null) {
            return false;
        }
        Optional<com.nexxserve.nexxclinic.entity.billing.VisitBillingVersion> latestVersionOpt =
            visitBillingVersionRepository.findFirstByVisitIdOrderByVersionDesc(visitId);
        UUID latestVersionId = latestVersionOpt
            .map(com.nexxserve.nexxclinic.entity.billing.VisitBillingVersion::getId)
            .orElse(null);
        UUID invoiceVersionId = billing.getBillingVersion() == null
            ? null
            : billing.getBillingVersion().getId();
        return latestVersionId == null
            ? invoiceVersionId == null
            : latestVersionId.equals(invoiceVersionId);
    }

    private Worker resolveWorker(AuthenticatedUser authUser) {
        if (authUser == null || authUser.userId() == null) {
            return null;
        }
        return workerRepository.findById(authUser.userId()).orElse(null);
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private String invoiceBucket() {
        String bucket = storageProps.getBucketInvoices();
        return (bucket == null || bucket.isBlank()) ? "data" : bucket;
    }

    private String buildInvoiceObjectPath(String clinicName, String billingId) {
        String filename = "invoice-" + billingId + ".pdf";
        if (clinicName != null && !clinicName.isBlank()) {
            String safe = clinicName.trim().replaceAll("[^a-zA-Z0-9_\\-]", "_");
            return "invoices/" + safe + "/" + filename;
        }
        return "invoices/" + filename;
    }

    private String resolveDownloadUrl(String objectPath) throws IOException {
        return storageService.getSignedUrl(invoiceBucket(), objectPath, signedUrlExpirySeconds);
    }

    /**
     * Immutable snapshot of everything the invoice PDF renderer needs, captured inside
     * a short read-only transaction. All lazy associations on {@code billing} are
     * eagerly fetched, so the entity can be safely rendered outside any transaction.
     */
    private record InvoiceSnapshot(
        ApiResponse<?> error,
        DepartmentInsuranceBilling billing,
        ClinicProfile clinicProfile,
        List<Map<String, Object>> items
    ) {
        static InvoiceSnapshot error(String message) {
            return new InvoiceSnapshot(ApiResponse.error(message), null, null, List.of());
        }

        static InvoiceSnapshot ready(
            DepartmentInsuranceBilling billing,
            ClinicProfile clinicProfile,
            List<Map<String, Object>> items
        ) {
            return new InvoiceSnapshot(null, billing, clinicProfile, items);
        }
    }
}
