package com.nexxserve.nexxclinic.service;

import com.nexxserve.nexxclinic.entity.PasswordHistoryEntry;
import com.nexxserve.nexxclinic.entity.Worker;
import com.nexxserve.nexxclinic.dto.out.ApiResponse;
import com.nexxserve.nexxclinic.repository.PasswordHistoryRepository;
import java.util.List;
import java.util.Locale;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PasswordPolicyService {

    private final PasswordEncoder passwordEncoder;
    private final PasswordHistoryRepository passwordHistoryRepository;
    private final int historyCount;

    public PasswordPolicyService(
            PasswordEncoder passwordEncoder,
            PasswordHistoryRepository passwordHistoryRepository,
            @Value("${app.password.history-count:5}") int historyCount
    ) {
        this.passwordEncoder = passwordEncoder;
        this.passwordHistoryRepository = passwordHistoryRepository;
        this.historyCount = historyCount;
    }

    @Transactional(readOnly = true)
    public ApiResponse validateNewPassword(Worker worker, String rawPassword) {
        if (worker == null) {
            return ApiResponse.error("User context is required for password validation.");
        }

        if (rawPassword == null || rawPassword.isBlank()) {
            return ApiResponse.error("Password is required.");
        }

        if (rawPassword.length() < 12) {
            return ApiResponse.error("Password must be at least 12 characters.");
        }

        if (rawPassword.chars().noneMatch(Character::isUpperCase)) {
            return ApiResponse.error("Password must include at least one uppercase letter.");
        }

        if (rawPassword.chars().noneMatch(Character::isLowerCase)) {
            return ApiResponse.error("Password must include at least one lowercase letter.");
        }

        if (rawPassword.chars().noneMatch(Character::isDigit)) {
            return ApiResponse.error("Password must include at least one digit.");
        }

        boolean hasSpecial = rawPassword.chars().anyMatch(ch -> !Character.isLetterOrDigit(ch));
        if (!hasSpecial) {
            return ApiResponse.error("Password must include at least one special character.");
        }

        if (rawPassword.chars().anyMatch(Character::isWhitespace)) {
            return ApiResponse.error("Password cannot contain whitespace.");
        }

        String lowered = rawPassword.toLowerCase(Locale.ROOT);
        if (worker.getFirstName() != null && !worker.getFirstName().isBlank()
                && lowered.contains(worker.getFirstName().toLowerCase(Locale.ROOT))) {
            return ApiResponse.error("Password cannot contain your first name.");
        }

        if (worker.getLastName() != null && !worker.getLastName().isBlank()
                && lowered.contains(worker.getLastName().toLowerCase(Locale.ROOT))) {
            return ApiResponse.error("Password cannot contain your last name.");
        }

        if (worker.getEmail() != null && !worker.getEmail().isBlank()) {
            String emailPrefix = worker.getEmail().split("@")[0].toLowerCase(Locale.ROOT);
            if (!emailPrefix.isBlank() && lowered.contains(emailPrefix)) {
                return ApiResponse.error("Password cannot contain your email prefix.");
            }
        }

        if (worker.getPasswordHash() != null && !worker.getPasswordHash().isBlank()
                && passwordEncoder.matches(rawPassword, worker.getPasswordHash())) {
            return ApiResponse.error("Password cannot be the same as your current password.");
        }

        if (worker.getId() == null) {
            return null;
        }

        List<PasswordHistoryEntry> history = passwordHistoryRepository.findTop10ByWorkerIdOrderByCreatedAtDesc(worker.getId());
        int checked = 0;
        for (PasswordHistoryEntry entry : history) {
            if (checked >= historyCount) {
                break;
            }
            checked++;
            if (passwordEncoder.matches(rawPassword, entry.getPasswordHash())) {
                return ApiResponse.error("Password was used recently. Choose a new one.");
            }
        }

        return null;
    }

    @Transactional
    public void saveToPasswordHistory(Worker worker, String encodedHash) {
        PasswordHistoryEntry entry = new PasswordHistoryEntry();
        entry.setWorker(worker);
        entry.setPasswordHash(encodedHash);
        passwordHistoryRepository.save(entry);
    }
}
