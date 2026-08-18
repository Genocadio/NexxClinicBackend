package com.nexxserve.nexxclinic.repository;

import com.nexxserve.nexxclinic.entity.ProductInsuranceCoverage;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductInsuranceCoverageRepository extends JpaRepository<ProductInsuranceCoverage, UUID> {

    List<ProductInsuranceCoverage> findByProductId(UUID productId);

    Optional<ProductInsuranceCoverage> findByProductIdAndInsuranceProviderId(UUID productId, UUID insuranceProviderId);

    List<ProductInsuranceCoverage> findByProductIdInAndInsuranceProviderIdIn(Collection<UUID> productIds, Collection<UUID> insuranceProviderIds);

    void deleteByProductId(UUID productId);

    boolean existsByProductIdAndInsuranceProviderId(UUID productId, UUID insuranceProviderId);

    boolean existsByProductIdAndInsuranceProviderIdAndIdNot(UUID productId, UUID insuranceProviderId, UUID id);
}
