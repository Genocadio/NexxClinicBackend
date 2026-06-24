package com.nexxserve.nexxclinic.repository;

import com.nexxserve.nexxclinic.entity.StandaloneForm;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface StandaloneFormRepository extends JpaRepository<StandaloneForm, UUID>, JpaSpecificationExecutor<StandaloneForm> {
    List<StandaloneForm> findByIsTemplateAndIsDeletedFalse(boolean isTemplate);
    List<StandaloneForm> findByCategoryAndIsDeletedFalse(String category);
    List<StandaloneForm> findByIsTemplateAndCategoryAndIsDeletedFalse(boolean isTemplate, String category);
    List<StandaloneForm> findByIsDeletedFalse();
}
