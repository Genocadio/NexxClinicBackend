package com.nexxserve.nexxclinic.service.billing;

import com.nexxserve.nexxclinic.entity.Visit;
import com.nexxserve.nexxclinic.entity.VisitBilling;
import com.nexxserve.nexxclinic.entity.VisitDepartmentProduct;
import com.nexxserve.nexxclinic.entity.billing.VisitBillingVersion;
import com.nexxserve.nexxclinic.model.VisitProductStatus;
import com.nexxserve.nexxclinic.repository.VisitBillingRepository;
import com.nexxserve.nexxclinic.repository.VisitBillingVersionRepository;
import com.nexxserve.nexxclinic.repository.VisitDepartmentProductRepository;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * Owns the immutable billing-version machinery: minting the next version for a
 * visit (with a unique-index collision retry), ordering containers by version
 * number, and detecting whether a visit has any billing at all.
 */
@Component
public class BillingVersionBuilder {

    private final VisitBillingVersionRepository visitBillingVersionRepository;
    private final VisitBillingRepository visitBillingRepository;
    private final VisitDepartmentProductRepository visitDepartmentProductRepository;

    public BillingVersionBuilder(
        VisitBillingVersionRepository visitBillingVersionRepository,
        VisitBillingRepository visitBillingRepository,
        VisitDepartmentProductRepository visitDepartmentProductRepository
    ) {
        this.visitBillingVersionRepository = visitBillingVersionRepository;
        this.visitBillingRepository = visitBillingRepository;
        this.visitDepartmentProductRepository = visitDepartmentProductRepository;
    }

    /**
     * Mints the next immutable billing version for the visit.
     *
     * <p>Retries once on a unique-index collision ({@code (visit_id, version)}
     * unique): a concurrent un-locked path could have inserted a version between
     * our read and save. Re-reads the max version and retries; if it still
     * collides, rethrows so the transaction fails loudly instead of writing a
     * duplicate.
     */
    public VisitBillingVersion createNextBillingVersion(Visit visit) {
        if (visit == null || visit.getId() == null) {
            throw new IllegalArgumentException("visit is required");
        }
        for (int attempt = 0; attempt < 2; attempt++) {
            VisitBillingVersion latest = visitBillingVersionRepository
                .findFirstByVisitIdOrderByVersionDesc(visit.getId())
                .orElse(null);

            VisitBillingVersion v = new VisitBillingVersion();
            v.setVisit(visit);
            v.setVersion(latest == null ? 1 : (latest.getVersion() + 1));
            v.setSupersedesVersionId(latest == null ? null : latest.getId());
            try {
                // saveAndFlush: the unique index on (visit_id, version) is only checked at
                // flush time — a plain save() defers the INSERT and the violation would
                // surface at commit, outside this catch, making the retry dead code.
                return visitBillingVersionRepository.saveAndFlush(v);
            } catch (org.springframework.dao.DataIntegrityViolationException ex) {
                if (attempt == 1) {
                    throw ex;
                }
            }
        }
        throw new IllegalStateException("Unable to allocate a billing version. Please retry.");
    }

    /**
     * Orders billing containers newest-version-first. The "latest" billing is the
     * one with the highest version number, not necessarily the most recent
     * {@code createdAt} (clock skew / backfill).
     */
    public List<VisitBilling> orderByVersionDesc(List<VisitBilling> billings) {
        if (billings == null || billings.isEmpty()) {
            return billings == null ? List.of() : billings;
        }
        return billings.stream()
            .sorted(
                Comparator.comparingInt(
                    (VisitBilling b) ->
                        b.getBillingVersion() == null
                            ? -1
                            : b.getBillingVersion().getVersion()
                ).reversed()
            )
            .toList();
    }

    /**
     * Whether the visit has any billing container at all (across all versions).
     */
    public boolean hasAnyExistingBilling(UUID visitId) {
        if (visitId == null) {
            return false;
        }
        return !visitBillingRepository.findByVisitIdOrderByCreatedAtDesc(visitId).isEmpty();
    }

    /**
     * Whether every non-deleted product of the visit is BILLED/EXEMPTED/PATIENT_SHARE_EXEMPTED. All rows
     * (including soft-deleted ones) are evaluated explicitly: a soft-deleted product
     * was removed from the current bill, so it never blocks completion.
     */
    public boolean isVisitFullyBilled(UUID visitId) {
        List<VisitDepartmentProduct> items =
            visitDepartmentProductRepository.findByVisitDepartmentVisitIdIncludingDeleted(visitId);
        if (items.isEmpty()) {
            return false;
        }
        return items.stream().allMatch(
            item ->
                item.isDeleted() ||
                item.getStatus() == VisitProductStatus.BILLED ||
                item.getStatus() == VisitProductStatus.EXEMPTED ||
                item.getStatus() == VisitProductStatus.PATIENT_SHARE_EXEMPTED
        );
    }
}
