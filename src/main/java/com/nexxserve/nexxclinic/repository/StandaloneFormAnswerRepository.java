package com.nexxserve.nexxclinic.repository;

import com.nexxserve.nexxclinic.entity.StandaloneFormAnswer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface StandaloneFormAnswerRepository extends JpaRepository<StandaloneFormAnswer, UUID> {
    List<StandaloneFormAnswer> findByFormVersionFormId(UUID formId);
    boolean existsByFormVersionId(UUID formVersionId);
}
