package com.nexxserve.nexxclinic.service;

import com.nexxserve.nexxclinic.auth.AuthenticatedUser;
import com.nexxserve.nexxclinic.auth.AccessTokenInfo;
import com.nexxserve.nexxclinic.auth.TokenBundle;
import com.nexxserve.nexxclinic.entity.Department;
import com.nexxserve.nexxclinic.entity.Product;
import com.nexxserve.nexxclinic.entity.ProductInsuranceCoverage;
import com.nexxserve.nexxclinic.entity.Worker;
import com.nexxserve.nexxclinic.entity.WorkerDocument;
import com.nexxserve.nexxclinic.graphql.input.ActivateUserInput;
import com.nexxserve.nexxclinic.graphql.input.AdminCreateUserInput;
import com.nexxserve.nexxclinic.graphql.input.AdminUpdateUserInput;
import com.nexxserve.nexxclinic.graphql.input.AdminSetUserSessionLimitInput;
import com.nexxserve.nexxclinic.graphql.input.AdminTriggerPasswordResetInput;
import com.nexxserve.nexxclinic.graphql.input.ChangeMyPasswordInput;
import com.nexxserve.nexxclinic.graphql.input.DeactivateUserInput;
import com.nexxserve.nexxclinic.graphql.input.LoginInput;
import com.nexxserve.nexxclinic.graphql.input.LogoutInput;
import com.nexxserve.nexxclinic.graphql.input.RefreshTokenInput;
import com.nexxserve.nexxclinic.graphql.input.RefreshSessionInput;
import com.nexxserve.nexxclinic.graphql.input.SelfRegisterInput;
import com.nexxserve.nexxclinic.graphql.input.SetInitialPasswordInput;
import com.nexxserve.nexxclinic.graphql.input.UpdateMyProfileInput;
import com.nexxserve.nexxclinic.model.AccountStatus;
import com.nexxserve.nexxclinic.dto.out.ApiResponse;
import com.nexxserve.nexxclinic.model.RoleName;
import com.nexxserve.nexxclinic.model.ResponseStatus;
import com.nexxserve.nexxclinic.repository.DepartmentInsurancePolicyRepository;
import com.nexxserve.nexxclinic.repository.DepartmentProfileProductRepository;
import com.nexxserve.nexxclinic.repository.DepartmentProfileRepository;
import com.nexxserve.nexxclinic.repository.DepartmentRepository;
import com.nexxserve.nexxclinic.repository.ProductInsuranceCoverageRepository;
import com.nexxserve.nexxclinic.repository.WorkerRepository;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WorkerService {

    private static final Logger logger = LoggerFactory.getLogger(WorkerService.class);
    private static final int DEFAULT_RESET_DAYS = 90;

    private final WorkerRepository workerRepository;
    private final DepartmentRepository departmentRepository;
    private final DepartmentInsurancePolicyRepository departmentInsurancePolicyRepository;
    private final DepartmentProfileRepository departmentProfileRepository;
    private final DepartmentProfileProductRepository departmentProfileProductRepository;
    private final ProductInsuranceCoverageRepository productInsuranceCoverageRepository;
    private final PasswordEncoder passwordEncoder;
    private final SessionTokenService sessionTokenService;
    private final PasswordPolicyService passwordPolicyService;
    private final AdminAuditService adminAuditService;
    private final MeilisearchIndexService meilisearchIndexService;

    public WorkerService(
            WorkerRepository workerRepository,
            DepartmentRepository departmentRepository,
            DepartmentInsurancePolicyRepository departmentInsurancePolicyRepository,
            DepartmentProfileRepository departmentProfileRepository,
            DepartmentProfileProductRepository departmentProfileProductRepository,
            ProductInsuranceCoverageRepository productInsuranceCoverageRepository,
            PasswordEncoder passwordEncoder,
            SessionTokenService sessionTokenService,
            PasswordPolicyService passwordPolicyService,
            AdminAuditService adminAuditService,
            MeilisearchIndexService meilisearchIndexService
    ) {
        this.workerRepository = workerRepository;
        this.departmentRepository = departmentRepository;
        this.departmentInsurancePolicyRepository = departmentInsurancePolicyRepository;
        this.departmentProfileRepository = departmentProfileRepository;
        this.departmentProfileProductRepository = departmentProfileProductRepository;
        this.productInsuranceCoverageRepository = productInsuranceCoverageRepository;
        this.passwordEncoder = passwordEncoder;
        this.sessionTokenService = sessionTokenService;
        this.passwordPolicyService = passwordPolicyService;
        this.adminAuditService = adminAuditService;
        this.meilisearchIndexService = meilisearchIndexService;
    }

    @Transactional
    public ApiResponse selfRegister(SelfRegisterInput input) {
        ApiResponse validationResult = validateInputBasics(input.firstName(), input.lastName(), input.email(), input.phoneNumber(), input.username());
        if (validationResult != null) {
            return validationResult;
        }

        if (input.password() == null || input.password().isBlank()) {
            return ApiResponse.error("Password is required for self registration.");
        }

        boolean firstUser = workerRepository.count() == 0;
        Worker worker = new Worker();
        applyCommonProfileFields(worker, input.firstName(), input.lastName(), input.gender(), input.dateOfBirth(),
                input.profilePhotoUrl(), input.email(), input.phoneNumber(), input.username(),
                mapDocument(input.workerDocProfile()));

        ApiResponse departmentValidationError = assignDepartments(worker, input.departmentIds());
        if (departmentValidationError != null) {
            return departmentValidationError;
        }

        ApiResponse passwordPolicyError = passwordPolicyService.validateNewPassword(worker, input.password());
        if (passwordPolicyError != null) {
            return passwordPolicyError;
        }

        String encodedPassword = passwordEncoder.encode(input.password());
        worker.setPasswordHash(encodedPassword);
        worker.setLastPasswordChange(LocalDateTime.now());

        if (firstUser) {
            worker.setRoles(Set.of(RoleName.ADMIN));
            worker.setAccountStatus(AccountStatus.ACTIVE);
            worker.setActive(true);
            worker.setAutoReset(false);
            worker.setResetPeriodDays(null);
            worker.setNextResetDate(null);
            worker.setMustChangeOnNextLogin(false);
        } else {
            worker.setRoles(Set.of());
            worker.setAccountStatus(AccountStatus.PENDING);
            worker.setActive(false);
            worker.setAutoReset(true);
            worker.setResetPeriodDays(DEFAULT_RESET_DAYS);
            worker.setNextResetDate(LocalDateTime.now().plusDays(DEFAULT_RESET_DAYS));
            worker.setMustChangeOnNextLogin(false);
        }

        Worker saved;
        try {
            saved = workerRepository.save(worker);
        } catch (DataIntegrityViolationException ex) {
            return mapPersistenceError(ex, "Unable to register user due to invalid or duplicate data.");
        }
        meilisearchIndexService.indexWorker(saved);
        try {
            passwordPolicyService.saveToPasswordHistory(saved, encodedPassword);
        } catch (Exception e) {
            // The password history row is advisory (enforces "recently used" policy).
            // A failure here must NOT roll back the successful registration/login.
            logger.warn("Could not persist password history for user {}: {}", saved.getId(), e.getMessage());
        }

        String message = firstUser
                ? "First user registered as ACTIVE ADMIN with non-expiring password policy."
                : "Registration submitted. Await admin activation before login.";
        return ApiResponse.success(message, workerToMap(saved));
    }

    @Transactional
    public ApiResponse adminCreateUser(AdminCreateUserInput input, AuthenticatedUser adminUser) {
        ApiResponse validationResult = validateInputBasics(input.firstName(), input.lastName(), input.email(), input.phoneNumber(), input.username());
        if (validationResult != null) {
            return validationResult;
        }

        if (input.roles() == null || input.roles().isEmpty()) {
            return ApiResponse.error("Admin-created users must include at least one role.");
        }

        Worker worker = new Worker();
        applyCommonProfileFields(worker, input.firstName(), input.lastName(), input.gender(), input.dateOfBirth(),
                input.profilePhotoUrl(), input.email(), input.phoneNumber(), input.username(),
                mapDocument(input.workerDocProfile()));

        ApiResponse departmentValidationError = assignDepartments(worker, input.departmentIds());
        if (departmentValidationError != null) {
            return departmentValidationError;
        }

        worker.setRoles(input.roles());
        worker.setAccountStatus(AccountStatus.ACTIVE);
        worker.setActive(true);
        worker.setAutoReset(true);
        worker.setResetPeriodDays(DEFAULT_RESET_DAYS);
        worker.setMustChangeOnNextLogin(true);
        worker.setPasswordHash(null);
        worker.setLastPasswordChange(null);
        worker.setNextResetDate(null);

        Worker saved;
        try {
            saved = workerRepository.save(worker);
        } catch (DataIntegrityViolationException ex) {
            return mapPersistenceError(ex, "Unable to create user due to invalid or duplicate data.");
        }
        meilisearchIndexService.indexWorker(saved);

        adminAuditService.logAdminAction(
                adminUser,
                saved.getId(),
                "ADMIN_CREATE_USER",
                "{\"roles\":\"" + saved.getRoles() + "\",\"active\":true}"
        );

        return ApiResponse.success("User created and activated. Password will be set on first login attempt.", workerToMap(saved));
    }

    @Transactional
    public ApiResponse activateUser(ActivateUserInput input, AuthenticatedUser adminUser) {
        if (input.userId() == null) {
            return ApiResponse.error("userId is required.");
        }

        if (input.roles() == null || input.roles().isEmpty()) {
            return ApiResponse.error("At least one role is required when activating a user.");
        }

        Optional<Worker> workerOptional = workerRepository.findById(input.userId());
        if (workerOptional.isEmpty()) {
            return ApiResponse.error("User not found.");
        }

        Worker worker = workerOptional.get();
        worker.setRoles(input.roles());
        worker.setAccountStatus(AccountStatus.ACTIVE);
        worker.setActive(true);
        Worker saved;
        try {
            saved = workerRepository.save(worker);
        } catch (DataIntegrityViolationException ex) {
            return mapPersistenceError(ex, "Unable to activate user due to invalid or duplicate data.");
        }
        meilisearchIndexService.indexWorker(saved);

        adminAuditService.logAdminAction(
            adminUser,
            worker.getId(),
            "ACTIVATE_USER_ASSIGN_ROLES",
            "{\"roles\":\"" + worker.getRoles() + "\",\"status\":\"ACTIVE\"}"
        );

        return ApiResponse.success("User activated and roles assigned.", workerToMap(worker));
    }

    @Transactional
    public ApiResponse login(LoginInput input) {
        if (input.identifier() == null || input.identifier().isBlank() || input.password() == null || input.password().isBlank()) {
            logger.warn("Login attempt with missing identifier or password");
            return ApiResponse.error("identifier and password are required.");
        }

        logger.info("Login attempt for identifier: {}", input.identifier());

        Optional<Worker> workerOptional = findByIdentifier(input.identifier());
        if (workerOptional.isEmpty()) {
            logger.warn("Login failed: User not found for identifier: {}", input.identifier());
            return ApiResponse.error("Invalid credentials.");
        }

        Worker worker = workerOptional.get();
        if (!worker.isActive() || worker.getAccountStatus() != AccountStatus.ACTIVE) {
            logger.warn("Login denied: Account not active for user: {} (status: {})", worker.getId(), worker.getAccountStatus());
            return ApiResponse.error("Your account is not activated yet.");
        }

        if (worker.getPasswordHash() == null || worker.getPasswordHash().isBlank()) {
            logger.info("Initial password setup required for user: {}", worker.getId());
            // S8 fix: Map.of throws NullPointerException on any null value. Build the
            // response map null-safely so a partially-populated worker can never 500.
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("passwordSetupRequired", true);
            data.put("identifier", input.identifier());
            data.put("userId", worker.getId());
            data.put("user", workerToMap(worker));
            return new ApiResponse<>(
                    com.nexxserve.nexxclinic.model.ResponseStatus.PARTIAL_SUCCESS,
                    "Password not set. Complete initial password setup.",
                    data
            );
        }

        if (!passwordEncoder.matches(input.password(), worker.getPasswordHash())) {
            logger.warn("Login failed: Invalid password for user: {}", worker.getId());
            return ApiResponse.error("Invalid credentials.");
        }

        TokenBundle bundle = sessionTokenService.issueSession(worker);
        logger.info("User successfully logged in: {} (ID: {}) with roles: {}", worker.getUsername(), worker.getId(), worker.getRoles());
        return ApiResponse.success("Login successful.", buildSessionData(worker, bundle));
    }

    @Transactional
    public ApiResponse setInitialPassword(SetInitialPasswordInput input) {
        if (input.identifier() == null || input.identifier().isBlank() || input.newPassword() == null || input.newPassword().isBlank()) {
            return ApiResponse.error("identifier and newPassword are required.");
        }

        Optional<Worker> workerOptional = findByIdentifier(input.identifier());
        if (workerOptional.isEmpty()) {
            return ApiResponse.error("User not found.");
        }

        Worker worker = workerOptional.get();
        if (!worker.isActive() || worker.getAccountStatus() != AccountStatus.ACTIVE) {
            return ApiResponse.error("Only active users can set initial password.");
        }

        if (worker.getPasswordHash() != null && !worker.getPasswordHash().isBlank()) {
            return ApiResponse.error("Initial password already set. Use normal login.");
        }

        ApiResponse passwordPolicyError = passwordPolicyService.validateNewPassword(worker, input.newPassword());
        if (passwordPolicyError != null) {
            return passwordPolicyError;
        }

        String encodedPassword = passwordEncoder.encode(input.newPassword());
        worker.setPasswordHash(encodedPassword);
        worker.setLastPasswordChange(LocalDateTime.now());
        worker.setMustChangeOnNextLogin(false);
        if (worker.isAutoReset() && worker.getResetPeriodDays() != null) {
            worker.setNextResetDate(LocalDateTime.now().plusDays(worker.getResetPeriodDays()));
        }

        Worker saved;
        try {
            saved = workerRepository.save(worker);
        } catch (DataIntegrityViolationException ex) {
            return mapPersistenceError(ex, "Unable to set initial password due to invalid data.");
        }
        try {
            passwordPolicyService.saveToPasswordHistory(saved, encodedPassword);
        } catch (Exception e) {
            logger.warn("Could not persist password history for user {}: {}", saved.getId(), e.getMessage());
        }
        try {
            sessionTokenService.issueSession(saved);
        } catch (Exception e) {
            // Password was saved; only the auto-login failed. Return success with the
            // flag set so the client logs in normally instead of showing a 500.
            logger.warn("Could not issue session after initial password setup for user {}: {}", saved.getId(), e.getMessage());
        }

        return ApiResponse.success("Initial password set successfully.", true);
    }

    @Transactional
    public ApiResponse refreshSession(RefreshSessionInput input) {
        if (input == null || input.refreshToken() == null || input.refreshToken().isBlank()) {
            return ApiResponse.error("refreshToken is required.");
        }

        Optional<TokenBundle> rotated = sessionTokenService.rotateRefreshToken(input.refreshToken());
        if (rotated.isEmpty()) {
            return ApiResponse.error("Invalid or expired refresh token.");
        }

        // S8 fix: Map.of throws NullPointerException on any null value. Build the
        // token map null-safely so a malformed token bundle can never 500.
        Map<String, Object> sessionData = new LinkedHashMap<>();
        sessionData.put("accessToken", rotated.get().accessToken());
        sessionData.put("refreshToken", rotated.get().refreshToken());
        return ApiResponse.success("Session refreshed.", sessionData);
    }

    @Transactional
    public ApiResponse refreshToken(RefreshTokenInput input) {
        if (input == null || input.refreshToken() == null || input.refreshToken().isBlank()) {
            logger.warn("Token refresh attempt with missing refresh token");
            return ApiResponse.error("refreshToken is required.");
        }

        logger.debug("Attempting to refresh access token");
        return refreshSession(new RefreshSessionInput(input.refreshToken()));
    }

    @Transactional
    public ApiResponse deactivateUser(DeactivateUserInput input, AuthenticatedUser adminUser) {
        if (input == null || input.userId() == null) {
            return ApiResponse.error("userId is required.");
        }

        Optional<Worker> workerOptional = workerRepository.findById(input.userId());
        if (workerOptional.isEmpty()) {
            return ApiResponse.error("User not found.");
        }

        Worker worker = workerOptional.get();
        worker.setAccountStatus(AccountStatus.DISABLED);
        worker.setActive(false);
        Worker saved;
        try {
            saved = workerRepository.save(worker);
        } catch (DataIntegrityViolationException ex) {
            return mapPersistenceError(ex, "Unable to deactivate user due to invalid data.");
        }
        meilisearchIndexService.indexWorker(saved);

        boolean revokeSessions = input.revokeSessions() == null || Boolean.TRUE.equals(input.revokeSessions());
        if (revokeSessions) {
            sessionTokenService.revokeAllRefreshTokensForUser(worker.getId(), "ADMIN_DEACTIVATE_USER");
        }

        adminAuditService.logAdminAction(
                adminUser,
                worker.getId(),
                "DEACTIVATE_USER",
                "{\"status\":\"DISABLED\",\"active\":false,\"revokeSessions\":" + revokeSessions + "}"
        );

        return ApiResponse.success("User account deactivated successfully.", workerToMap(worker));
    }

    @Transactional
    public ApiResponse logout(LogoutInput input, AuthenticatedUser authUser, AccessTokenInfo accessTokenInfo) {
        boolean anyRevoked = false;
        String refreshToken = input == null ? null : input.refreshToken();
        boolean revokeAll = input != null && Boolean.TRUE.equals(input.revokeAllSessions());

        if (authUser != null && accessTokenInfo != null) {
            logger.info("User logging out: {} (ID: {})", authUser.principal(), authUser.userId());
            sessionTokenService.revokeAccessTokenByJti(
                    accessTokenInfo.tokenId(),
                    sessionTokenService.toLocalDateTime(accessTokenInfo),
                    authUser.userId(),
                    "LOGOUT"
            );
            anyRevoked = true;
        }

        if (revokeAll && authUser != null) {
            logger.info("Revoking all sessions for user: {} (ID: {})", authUser.principal(), authUser.userId());
            sessionTokenService.revokeAllRefreshTokensForUser(authUser.userId(), "LOGOUT_ALL");
            anyRevoked = true;
        } else if (refreshToken != null && !refreshToken.isBlank()) {
            logger.debug("Revoking specific refresh token");
            anyRevoked = sessionTokenService.revokeRefreshToken(refreshToken, "LOGOUT") || anyRevoked;
        }

        if (!anyRevoked) {
            logger.warn("No active session token could be revoked for logout request");
            return ApiResponse.partialSuccess("No active session token could be revoked.", List.of());
        }

        return ApiResponse.success("Session revoked successfully.", true);
    }

    @Transactional(readOnly = true)
    public ApiResponse me(AuthenticatedUser authUser) {
        Optional<Worker> workerOptional = workerRepository.findById(authUser.userId());
        if (workerOptional.isEmpty()) {
            return ApiResponse.error("Authenticated user no longer exists.");
        }

        return ApiResponse.success("Authenticated user profile.", workerToMap(workerOptional.get()));
    }

    @Transactional(readOnly = true)
    public ApiResponse listUsers() {
        List<Map<String, Object>> users = workerRepository.findAll().stream().map(this::workerToMap).toList();
        return ApiResponse.success("Users fetched.", users);
    }

    @Transactional(readOnly = true)
    public ApiResponse searchWorkers(String name, RoleName role, Boolean activeOnly, UUID departmentId) {
        // Meilisearch first (typo-tolerant, ranked); the DB query below is the fallback.
        if (meilisearchIndexService.isEnabled()) {
            try {
                MeilisearchIndexService.SearchHit hit = meilisearchIndexService.searchWorkers(
                        blankToNull(name),
                        role,
                        activeOnly,
                        departmentId
                );
                List<Map<String, Object>> users = loadWorkersByHits(hit.ids());
                return ApiResponse.success("Workers fetched.", users);
            } catch (MeilisearchIndexService.SearchUnavailableException e) {
                logger.warn("Meilisearch unavailable for workers, falling back to DB: {}", e.getMessage());
            }
        }

        String normalizedName = blankToNull(name);
        List<Map<String, Object>> users = workerRepository.searchWorkers(normalizedName, role, activeOnly, departmentId)
                .stream()
                .map(this::workerToMap)
                .toList();
        return ApiResponse.success("Workers fetched.", users);
    }

    /** Hydrates worker maps from Meilisearch hit ids, preserving hit order. */
    private List<Map<String, Object>> loadWorkersByHits(List<UUID> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        Map<UUID, Worker> byId = new HashMap<>();
        workerRepository.findAllById(ids).forEach(w -> byId.put(w.getId(), w));
        List<Map<String, Object>> users = new ArrayList<>();
        for (UUID id : ids) {
            Worker worker = byId.get(id);
            if (worker != null) {
                users.add(workerToMap(worker));
            }
        }
        return users;
    }

    @Transactional(readOnly = true)
    public ApiResponse adminAuditLogs() {
        return ApiResponse.success("Admin audit logs fetched.", adminAuditService.latestAuditLogs());
    }

    @Transactional
    public ApiResponse updateMyProfile(UpdateMyProfileInput input, AuthenticatedUser authUser) {
        if (authUser == null) {
            return ApiResponse.unauthenticated("Authentication is required for this operation.");
        }

        Optional<Worker> workerOptional = workerRepository.findById(authUser.userId());
        if (workerOptional.isEmpty()) {
            return ApiResponse.error("Authenticated user no longer exists.");
        }

        Worker worker = workerOptional.get();

        if (input.firstName() != null) {
            if (input.firstName().isBlank()) {
                return ApiResponse.error("firstName cannot be blank.");
            }
            worker.setFirstName(input.firstName().trim());
        }

        if (input.lastName() != null) {
            if (input.lastName().isBlank()) {
                return ApiResponse.error("lastName cannot be blank.");
            }
            worker.setLastName(input.lastName().trim());
        }

        if (input.gender() != null) {
            worker.setGender(input.gender());
        }

        if (input.dateOfBirth() != null) {
            worker.setDateOfBirth(input.dateOfBirth());
        }

        if (input.profilePhotoUrl() != null) {
            worker.setProfilePhotoUrl(blankToNull(input.profilePhotoUrl()));
        }

        if (input.email() != null) {
            String normalizedEmail = blankToNull(input.email());
            if (normalizedEmail != null
                    && workerRepository.existsByEmailIgnoreCaseAndIdNot(normalizedEmail, worker.getId())) {
                return ApiResponse.error("Email already exists.");
            }
            worker.setEmail(normalizedEmail);
        }

        if (input.phoneNumber() != null) {
            String normalizedPhone = blankToNull(input.phoneNumber());
            if (normalizedPhone != null
                    && workerRepository.existsByPhoneNumberAndIdNot(normalizedPhone, worker.getId())) {
                return ApiResponse.error("Phone number already exists.");
            }
            worker.setPhoneNumber(normalizedPhone);
        }

        if (input.username() != null) {
            String normalizedUsername = blankToNull(input.username());
            if (normalizedUsername != null
                    && workerRepository.existsByUsernameIgnoreCaseAndIdNot(normalizedUsername, worker.getId())) {
                return ApiResponse.error("Username already exists.");
            }
            worker.setUsername(normalizedUsername);
        }

        if (input.departmentIds() != null) {
            ApiResponse departmentValidationError = assignDepartments(worker, input.departmentIds());
            if (departmentValidationError != null) {
                return departmentValidationError;
            }
        }

        if ((worker.getEmail() == null || worker.getEmail().isBlank())
                && (worker.getPhoneNumber() == null || worker.getPhoneNumber().isBlank())) {
            return ApiResponse.error("At least one contact method is required: email or phoneNumber.");
        }

        if (input.workerDocProfile() != null) {
            worker.setWorkerDocProfile(mapDocument(input.workerDocProfile()));
        }

        Worker saved;
        try {
            saved = workerRepository.save(worker);
        } catch (DataIntegrityViolationException ex) {
            return mapPersistenceError(ex, "Unable to update profile due to invalid or duplicate data.");
        }
        meilisearchIndexService.indexWorker(saved);
        return ApiResponse.success("Profile updated successfully.", workerToMap(saved));
    }

    @Transactional
    public ApiResponse adminUpdateUser(UUID userId, AdminUpdateUserInput input, AuthenticatedUser adminUser) {
        if (userId == null || input == null) {
            return ApiResponse.error("userId and input are required.");
        }

        Optional<Worker> workerOptional = workerRepository.findById(userId);
        if (workerOptional.isEmpty()) {
            return ApiResponse.error("User not found.");
        }

        Worker worker = workerOptional.get();

        if (input.firstName() != null) {
            if (input.firstName().isBlank()) {
                return ApiResponse.error("firstName cannot be blank.");
            }
            worker.setFirstName(input.firstName().trim());
        }

        if (input.lastName() != null) {
            if (input.lastName().isBlank()) {
                return ApiResponse.error("lastName cannot be blank.");
            }
            worker.setLastName(input.lastName().trim());
        }

        if (input.gender() != null) {
            worker.setGender(input.gender());
        }

        if (input.dateOfBirth() != null) {
            worker.setDateOfBirth(input.dateOfBirth());
        }

        if (input.profilePhotoUrl() != null) {
            worker.setProfilePhotoUrl(blankToNull(input.profilePhotoUrl()));
        }

        if (input.email() != null) {
            String normalizedEmail = blankToNull(input.email());
            if (normalizedEmail != null
                    && workerRepository.existsByEmailIgnoreCaseAndIdNot(normalizedEmail, worker.getId())) {
                return ApiResponse.error("Email already exists.");
            }
            worker.setEmail(normalizedEmail);
        }

        if (input.phoneNumber() != null) {
            String normalizedPhone = blankToNull(input.phoneNumber());
            if (normalizedPhone != null
                    && workerRepository.existsByPhoneNumberAndIdNot(normalizedPhone, worker.getId())) {
                return ApiResponse.error("Phone number already exists.");
            }
            worker.setPhoneNumber(normalizedPhone);
        }

        if (input.username() != null) {
            String normalizedUsername = blankToNull(input.username());
            if (normalizedUsername != null
                    && workerRepository.existsByUsernameIgnoreCaseAndIdNot(normalizedUsername, worker.getId())) {
                return ApiResponse.error("Username already exists.");
            }
            worker.setUsername(normalizedUsername);
        }

        if (input.departmentIds() != null) {
            ApiResponse departmentValidationError = assignDepartments(worker, input.departmentIds());
            if (departmentValidationError != null) {
                return departmentValidationError;
            }
        }

        if (input.roles() != null) {
            if (input.roles().isEmpty()) {
                return ApiResponse.error("roles cannot be empty.");
            }
            worker.setRoles(input.roles());
        }

        if ((worker.getEmail() == null || worker.getEmail().isBlank())
                && (worker.getPhoneNumber() == null || worker.getPhoneNumber().isBlank())) {
            return ApiResponse.error("At least one contact method is required: email or phoneNumber.");
        }

        if (input.workerDocProfile() != null) {
            worker.setWorkerDocProfile(mapDocument(input.workerDocProfile()));
        }

        Worker saved;
        try {
            saved = workerRepository.save(worker);
        } catch (DataIntegrityViolationException ex) {
            return mapPersistenceError(ex, "Unable to update user due to invalid or duplicate data.");
        }
        meilisearchIndexService.indexWorker(saved);

        adminAuditService.logAdminAction(
                adminUser,
                saved.getId(),
                "ADMIN_UPDATE_USER",
                "{\"roles\":\"" + saved.getRoles() + "\",\"departmentCount\":" + saved.getDepartments().size() + "}"
        );

        return ApiResponse.success("User updated successfully.", workerToMap(saved));
    }

    @Transactional
    public ApiResponse changeMyPassword(
            ChangeMyPasswordInput input,
            AuthenticatedUser authUser,
            AccessTokenInfo accessTokenInfo
    ) {
        if (authUser == null) {
            logger.warn("Attempt to change password without authentication");
            return ApiResponse.unauthenticated("Authentication is required for this operation.");
        }

        if (input == null || input.currentPassword() == null || input.currentPassword().isBlank()
                || input.newPassword() == null || input.newPassword().isBlank()) {
            logger.warn("Password change attempt with missing fields from user: {}", authUser.userId());
            return ApiResponse.error("currentPassword and newPassword are required.");
        }

        Optional<Worker> workerOptional = workerRepository.findById(authUser.userId());
        if (workerOptional.isEmpty()) {
            logger.error("Authenticated user no longer exists: {}", authUser.userId());
            return ApiResponse.error("Authenticated user no longer exists.");
        }

        Worker worker = workerOptional.get();
        if (worker.getPasswordHash() == null || worker.getPasswordHash().isBlank()) {
            logger.warn("Password change attempted but no existing password set for user: {}", authUser.userId());
            return ApiResponse.error("No existing password set. Use initial password setup.");
        }

        if (!passwordEncoder.matches(input.currentPassword(), worker.getPasswordHash())) {
            logger.warn("Password change failed: Current password incorrect for user: {}", authUser.userId());
            return ApiResponse.error("Current password is incorrect.");
        }

        ApiResponse passwordPolicyError = passwordPolicyService.validateNewPassword(worker, input.newPassword());
        if (passwordPolicyError != null) {
            logger.warn("Password change failed: Policy validation failed for user: {}", authUser.userId());
            return passwordPolicyError;
        }

        String encodedPassword = passwordEncoder.encode(input.newPassword());
        worker.setPasswordHash(encodedPassword);
        worker.setLastPasswordChange(LocalDateTime.now());
        worker.setMustChangeOnNextLogin(false);
        if (worker.isAutoReset() && worker.getResetPeriodDays() != null) {
            worker.setNextResetDate(LocalDateTime.now().plusDays(worker.getResetPeriodDays()));
        }

        Worker saved;
        try {
            saved = workerRepository.save(worker);
        } catch (DataIntegrityViolationException ex) {
            return mapPersistenceError(ex, "Unable to change password due to invalid data.");
        }
        try {
            passwordPolicyService.saveToPasswordHistory(saved, encodedPassword);
        } catch (Exception e) {
            logger.warn("Could not persist password history for user {}: {}", saved.getId(), e.getMessage());
        }

        try {
            sessionTokenService.revokeAllRefreshTokensForUser(saved.getId(), "PASSWORD_CHANGED");
            if (accessTokenInfo != null) {
                sessionTokenService.revokeAccessTokenByJti(
                        accessTokenInfo.tokenId(),
                        sessionTokenService.toLocalDateTime(accessTokenInfo),
                        saved.getId(),
                        "PASSWORD_CHANGED"
                );
            }
        } catch (Exception e) {
            // Token revocation is best-effort; the password change itself is committed.
            logger.warn("Could not revoke old tokens after password change for user {}: {}", saved.getId(), e.getMessage());
        }

        logger.info("Password changed successfully for user: {} (ID: {})", authUser.principal(), authUser.userId());
        try {
            sessionTokenService.issueSession(saved);
        } catch (Exception e) {
            logger.warn("Could not issue session after password change for user {}: {}", saved.getId(), e.getMessage());
        }
        return ApiResponse.success("Password changed successfully.", true);
    }

    @Transactional
    public ApiResponse adminTriggerPasswordReset(AdminTriggerPasswordResetInput input, AuthenticatedUser adminUser) {
        if (input == null || input.userId() == null) {
            return ApiResponse.error("userId is required.");
        }

        Optional<Worker> workerOptional = workerRepository.findById(input.userId());
        if (workerOptional.isEmpty()) {
            return ApiResponse.error("User not found.");
        }

        Worker worker = workerOptional.get();
        worker.setMustChangeOnNextLogin(true);
        if (worker.isAutoReset() && worker.getResetPeriodDays() != null) {
            worker.setNextResetDate(LocalDateTime.now());
        }
        workerRepository.save(worker);

        boolean revokeSessions = input.revokeSessions() == null || Boolean.TRUE.equals(input.revokeSessions());
        if (revokeSessions) {
            sessionTokenService.revokeAllRefreshTokensForUser(worker.getId(), "ADMIN_TRIGGERED_PASSWORD_RESET");
        }

        adminAuditService.logAdminAction(
                adminUser,
                worker.getId(),
                "ADMIN_TRIGGER_PASSWORD_RESET",
                "{\"revokeSessions\":" + revokeSessions + ",\"mustChangeOnNextLogin\":true}"
        );

        return ApiResponse.success("Password reset policy triggered successfully.", true);
    }

    @Transactional
    public ApiResponse adminSetUserSessionLimit(AdminSetUserSessionLimitInput input, AuthenticatedUser adminUser) {
        if (input == null || input.userId() == null || input.maxActiveSessions() == null) {
            return ApiResponse.error("userId and maxActiveSessions are required.");
        }

        if (input.maxActiveSessions() < 1) {
            return ApiResponse.error("maxActiveSessions must be at least 1.");
        }

        Optional<Worker> workerOptional = workerRepository.findById(input.userId());
        if (workerOptional.isEmpty()) {
            return ApiResponse.error("User not found.");
        }

        Worker worker = workerOptional.get();
        worker.setMaxActiveSessions(input.maxActiveSessions());
        Worker saved = workerRepository.save(worker);

        int activeSessions = sessionTokenService.countActiveRefreshSessionsForUser(saved.getId());
        if (activeSessions > input.maxActiveSessions()) {
            sessionTokenService.revokeOldestRefreshTokensForUserToLimit(saved.getId(), input.maxActiveSessions(), "ADMIN_SESSION_LIMIT_UPDATED");
            activeSessions = sessionTokenService.countActiveRefreshSessionsForUser(saved.getId());
        }

        adminAuditService.logAdminAction(
                adminUser,
                saved.getId(),
                "ADMIN_SET_USER_SESSION_LIMIT",
                "{\"maxActiveSessions\":" + saved.getMaxActiveSessions() + ",\"activeSessions\":" + activeSessions + "}"
        );

        return ApiResponse.success("User session limit updated successfully.", true);
    }

    private ApiResponse validateInputBasics(String firstName, String lastName, String email, String phoneNumber, String username) {
        if (firstName == null || firstName.isBlank()) {
            return ApiResponse.error("firstName is required.");
        }

        if ((email == null || email.isBlank()) && (phoneNumber == null || phoneNumber.isBlank())) {
            return ApiResponse.error("At least one contact method is required: email or phoneNumber.");
        }

        if (email != null && !email.isBlank() && workerRepository.existsByEmailIgnoreCase(email)) {
            return ApiResponse.error("Email already exists.");
        }

        if (phoneNumber != null && !phoneNumber.isBlank() && workerRepository.existsByPhoneNumber(phoneNumber)) {
            return ApiResponse.error("Phone number already exists.");
        }

        if (username != null && !username.isBlank() && workerRepository.existsByUsernameIgnoreCase(username)) {
            return ApiResponse.error("Username already exists.");
        }

        return null;
    }

    private Optional<Worker> findByIdentifier(String identifier) {
        // findFirst* variants: legacy duplicate rows (pre-unique-index data) used to make
        // login throw IncorrectResultSizeDataAccessException -> 500. LIMIT 1, oldest wins.
        Optional<Worker> byEmail = workerRepository.findFirstByEmailIgnoreCaseOrderByCreatedAtAsc(identifier);
        if (byEmail.isPresent()) {
            return byEmail;
        }

        Optional<Worker> byUsername = workerRepository.findFirstByUsernameIgnoreCaseOrderByCreatedAtAsc(identifier);
        if (byUsername.isPresent()) {
            return byUsername;
        }

        return workerRepository.findFirstByPhoneNumberOrderByCreatedAtAsc(identifier);
    }

    private void applyCommonProfileFields(
            Worker worker,
            String firstName,
            String lastName,
            com.nexxserve.nexxclinic.model.Gender gender,
            java.time.LocalDate dateOfBirth,
            String profilePhotoUrl,
            String email,
            String phoneNumber,
            String username,
            WorkerDocument workerDocument
    ) {
        worker.setFirstName(firstName.trim());
        worker.setLastName(blankToNull(lastName));
        worker.setGender(gender);
        worker.setDateOfBirth(dateOfBirth);
        worker.setProfilePhotoUrl(blankToNull(profilePhotoUrl));
        worker.setEmail(blankToNull(email));
        worker.setPhoneNumber(blankToNull(phoneNumber));
        worker.setUsername(blankToNull(username));
        worker.setWorkerDocProfile(workerDocument);
    }

    private WorkerDocument mapDocument(SelfRegisterInput.WorkerDocumentInput input) {
        if (input == null) {
            return null;
        }

        WorkerDocument doc = new WorkerDocument();
        doc.setType(input.type());
        doc.setTitle(input.title());
        doc.setRequired(Boolean.TRUE.equals(input.isRequired()));
        doc.setDocumentUrl(input.documentUrl());
        doc.setDocumentNumber(input.documentNumber());
        doc.setHasExpiration(Boolean.TRUE.equals(input.hasExpiration()));
        doc.setExpirationDate(input.expirationDate());
        doc.setIssuedBy(input.issuedBy());
        doc.setIssuedDate(input.issuedDate());
        doc.setNotes(input.notes());
        return doc;
    }

    private WorkerDocument mapDocument(AdminCreateUserInput.WorkerDocumentInput input) {
        if (input == null) {
            return null;
        }

        WorkerDocument doc = new WorkerDocument();
        doc.setType(input.type());
        doc.setTitle(input.title());
        doc.setRequired(Boolean.TRUE.equals(input.isRequired()));
        doc.setDocumentUrl(input.documentUrl());
        doc.setDocumentNumber(input.documentNumber());
        doc.setHasExpiration(Boolean.TRUE.equals(input.hasExpiration()));
        doc.setExpirationDate(input.expirationDate());
        doc.setIssuedBy(input.issuedBy());
        doc.setIssuedDate(input.issuedDate());
        doc.setNotes(input.notes());
        return doc;
    }

    private WorkerDocument mapDocument(UpdateMyProfileInput.WorkerDocumentInput input) {
        if (input == null) {
            return null;
        }

        WorkerDocument doc = new WorkerDocument();
        doc.setType(input.type());
        doc.setTitle(input.title());
        doc.setRequired(Boolean.TRUE.equals(input.isRequired()));
        doc.setDocumentUrl(input.documentUrl());
        doc.setDocumentNumber(input.documentNumber());
        doc.setHasExpiration(Boolean.TRUE.equals(input.hasExpiration()));
        doc.setExpirationDate(input.expirationDate());
        doc.setIssuedBy(input.issuedBy());
        doc.setIssuedDate(input.issuedDate());
        doc.setNotes(input.notes());
        return doc;
    }

    private WorkerDocument mapDocument(AdminUpdateUserInput.WorkerDocumentInput input) {
        if (input == null) {
            return null;
        }

        WorkerDocument doc = new WorkerDocument();
        doc.setType(input.type());
        doc.setTitle(input.title());
        doc.setRequired(Boolean.TRUE.equals(input.isRequired()));
        doc.setDocumentUrl(input.documentUrl());
        doc.setDocumentNumber(input.documentNumber());
        doc.setHasExpiration(Boolean.TRUE.equals(input.hasExpiration()));
        doc.setExpirationDate(input.expirationDate());
        doc.setIssuedBy(input.issuedBy());
        doc.setIssuedDate(input.issuedDate());
        doc.setNotes(input.notes());
        return doc;
    }

    private ApiResponse assignDepartments(Worker worker, List<UUID> departmentIds) {
        if (departmentIds == null) {
            return null;
        }
        Set<Department> departments = new java.util.LinkedHashSet<>();
        for (UUID id : departmentIds) {
            Optional<Department> deptOpt = departmentRepository.findById(id);
            if (deptOpt.isEmpty()) {
                return ApiResponse.error("Department not found.");
            }
            departments.add(deptOpt.get());
        }
        worker.setDepartments(departments);
        return null;
    }

    private Map<String, Object> workerToMap(Worker worker) {
        Map<String, Object> data = new HashMap<>();
        data.put("id", worker.getId());
        data.put("firstName", worker.getFirstName());
        data.put("lastName", worker.getLastName());
        data.put("gender", worker.getGender());
        data.put("dateOfBirth", worker.getDateOfBirth());
        data.put("profilePhotoUrl", worker.getProfilePhotoUrl());
        data.put("email", worker.getEmail());
        data.put("phoneNumber", worker.getPhoneNumber());
        data.put("username", worker.getUsername());
        data.put("accountStatus", worker.getAccountStatus());
        data.put("active", worker.isActive());
        data.put("roles", worker.getRoles());
        data.put("autoReset", worker.isAutoReset());
        data.put("resetPeriodDays", worker.getResetPeriodDays());
        data.put("lastPasswordChange", worker.getLastPasswordChange());
        data.put("nextResetDate", worker.getNextResetDate());
        data.put("mustChangeOnNextLogin", worker.isMustChangeOnNextLogin());
        data.put("departments", worker.getDepartments().stream().map(this::departmentToMap).toList());
        data.put("department", departmentToMap(worker.getDepartment()));
        data.put("maxActiveSessions", sessionTokenService.resolveEffectiveMaxSessions(worker));
        data.put("activeSessions", sessionTokenService.countActiveRefreshSessionsForUser(worker.getId()));
        data.put("createdAt", worker.getCreatedAt());
        data.put("updatedAt", worker.getUpdatedAt());
        return data;
    }

    private Map<String, Object> departmentToMap(Department department) {
        if (department == null) {
            return null;
        }

        Map<String, Object> data = new HashMap<>();
        data.put("id", department.getId());
        data.put("name", department.getName());
        data.put("insurancePolicyMode", department.getInsurancePolicyMode());
        data.put(
                "insurancePolicies",
                departmentInsurancePolicyRepository.findByDepartmentId(department.getId())
                        .stream()
                        .map(policy -> insuranceProviderToMap(policy.getInsuranceProvider()))
                        .toList()
        );
        data.put(
                "profiles",
                departmentProfileRepository.findByDepartmentId(department.getId())
                        .stream()
                        .map(profile -> {
                            Map<String, Object> profileMap = new HashMap<>();
                            profileMap.put("id", profile.getId());
                            profileMap.put("name", profile.getName());
                            profileMap.put("isDefault", profile.isDefault());
                            profileMap.put(
                                    "products",
                                    departmentProfileProductRepository.findByProfileId(profile.getId())
                                            .stream()
                                            .map(link -> productToMap(link.getProduct()))
                                            .toList()
                            );
                            return profileMap;
                        })
                        .toList()
        );
        data.put("nursing", department.isNursing());
        data.put("supportRequests", department.isSupportRequests());
        data.put("requestsProducts", department.isRequestsProducts());
        data.put("createdAt", department.getCreatedAt());
        data.put("updatedAt", department.getUpdatedAt());
        return data;
    }

    private Map<String, Object> insuranceProviderToMap(com.nexxserve.nexxclinic.entity.InsuranceProvider insuranceProvider) {
        Map<String, Object> data = new HashMap<>();
        data.put("id", insuranceProvider.getId());
        data.put("insuranceName", insuranceProvider.getInsuranceName());
        data.put("acronym", insuranceProvider.getAcronym());
        data.put("defaultCoveragePercentage", insuranceProvider.getDefaultCoveragePercentage());
        data.put("supportedByClinic", insuranceProvider.isSupportedByClinic());
        data.put("iconUrl", insuranceProvider.getIconUrl());
        data.put("createdAt", insuranceProvider.getCreatedAt());
        data.put("updatedAt", insuranceProvider.getUpdatedAt());
        return data;
    }

    private Map<String, Object> productToMap(Product product) {
        Map<String, Object> data = new HashMap<>();
        data.put("id", product.getId());
        data.put("name", product.getName());
        data.put("genericName", product.getGenericName());
        data.put("code", product.getCode());
        data.put("description", product.getDescription());
        data.put("type", product.getType());
        data.put("unit", product.getUnit());
        data.put("metadata", product.getMetadata());
        data.put("privateRhicPrice", product.getPrivateRhicPrice());
        data.put("clinicPrice", product.getClinicPrice());
        data.put(
                "insuranceCoverages",
                productInsuranceCoverageRepository.findByProductId(product.getId())
                        .stream()
                        .map(this::productCoverageToMap)
                        .toList()
        );
        data.put("createdAt", product.getCreatedAt());
        data.put("updatedAt", product.getUpdatedAt());
        return data;
    }

    private Map<String, Object> productCoverageToMap(ProductInsuranceCoverage coverage) {
        Map<String, Object> data = new HashMap<>();
        data.put("id", coverage.getId());
        data.put("insuranceProvider", insuranceProviderToMap(coverage.getInsuranceProvider()));
        data.put("cost", coverage.getCost());
        data.put("covered", coverage.isCovered());
        data.put("requireMedicalAdvisor", coverage.isRequireMedicalAdvisor());
        data.put("mustPrescribedBy", coverage.getMustPrescribedBy());
        data.put("drugAdministrationFrequency", coverage.getDrugAdministrationFrequency());
        data.put("authorizationRequestReasons", coverage.getAuthorizationRequestReasons());
        data.put("createdAt", coverage.getCreatedAt());
        data.put("updatedAt", coverage.getUpdatedAt());
        return data;
    }

    private String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private Map<String, Object> buildSessionData(Worker worker, TokenBundle bundle) {
        Map<String, Object> data = new HashMap<>();
        data.put("accessToken", bundle.accessToken());
        data.put("refreshToken", bundle.refreshToken());
        data.put("user", workerToMap(worker));
        data.put("userId", worker.getId());
        data.put("roles", worker.getRoles());
        data.put("mustChangeOnNextLogin", worker.isMustChangeOnNextLogin());
        data.put("maxActiveSessions", sessionTokenService.resolveEffectiveMaxSessions(worker));
        data.put("activeSessions", sessionTokenService.countActiveRefreshSessionsForUser(worker.getId()));
        return data;
    }

    private ApiResponse mapPersistenceError(DataIntegrityViolationException ex, String defaultMessage) {
        String loweredMessage = ex.getMessage() == null ? "" : ex.getMessage().toLowerCase();
        if (loweredMessage.contains("workers_email_key") || loweredMessage.contains("email")) {
            return ApiResponse.error("Email already exists.");
        }
        if (loweredMessage.contains("workers_phone_number_key") || loweredMessage.contains("phone")) {
            return ApiResponse.error("Phone number already exists.");
        }
        if (loweredMessage.contains("workers_username_key") || loweredMessage.contains("username")) {
            return ApiResponse.error("Username already exists.");
        }
        logger.error("Persistence error while saving worker", ex);
        return ApiResponse.error(defaultMessage);
    }
}
