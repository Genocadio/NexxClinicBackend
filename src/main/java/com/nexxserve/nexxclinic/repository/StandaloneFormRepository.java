package com.nexxserve.nexxclinic.repository;

import com.nexxserve.nexxclinic.entity.StandaloneForm;
import org.springframework.data.jpa.domain.Specification;
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

    static Specification<StandaloneForm> filter(Boolean isTemplate, String category, String name) {
        return (root, query, cb) -> {
            var predicates = new java.util.ArrayList<jakarta.persistence.criteria.Predicate>();
            predicates.add(cb.isFalse(root.get("isDeleted")));

            if (isTemplate != null) {
                if (isTemplate) {
                    predicates.add(cb.isTrue(root.get("isTemplate")));
                } else {
                    predicates.add(cb.isFalse(root.get("isTemplate")));
                }
            }
            if (category != null && !category.trim().isEmpty()) {
                predicates.add(cb.equal(cb.lower(root.get("category")), category.trim().toLowerCase()));
            }
            if (name != null && !name.trim().isEmpty()) {
                predicates.add(cb.like(cb.lower(root.get("name")), "%" + name.trim().toLowerCase() + "%"));
            }

            return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };
    }
}
