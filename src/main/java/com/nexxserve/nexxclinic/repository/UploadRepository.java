package com.nexxserve.nexxclinic.repository;

import com.nexxserve.nexxclinic.entity.Upload;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UploadRepository extends JpaRepository<Upload, UUID> {

    Optional<Upload> findByUrl(String url);
}
