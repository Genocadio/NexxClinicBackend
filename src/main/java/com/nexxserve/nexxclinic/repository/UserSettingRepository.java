package com.nexxserve.nexxclinic.repository;

import com.nexxserve.nexxclinic.entity.UserSetting;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserSettingRepository extends JpaRepository<UserSetting, UUID> {
    Optional<UserSetting> findByWorkerId(UUID workerId);
}
