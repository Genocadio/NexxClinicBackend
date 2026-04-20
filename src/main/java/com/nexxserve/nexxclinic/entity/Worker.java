package com.nexxserve.nexxclinic.entity;

import com.nexxserve.nexxclinic.model.AccountStatus;
import com.nexxserve.nexxclinic.model.Gender;
import com.nexxserve.nexxclinic.model.RoleName;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "workers")
public class Worker {

    public static final int DEFAULT_MAX_ACTIVE_SESSIONS = 4;

    @Id
    @GeneratedValue
    private UUID id;

    @Column(nullable = false)
    private String firstName;

    @Column
    private String lastName;

    @Enumerated(EnumType.STRING)
    private Gender gender;

    private LocalDate dateOfBirth;
    private String profilePhotoUrl;

    @Column(unique = true)
    private String email;

    @Column(unique = true)
    private String phoneNumber;

    @Column(unique = true)
    private String username;

    @Embedded
    private WorkerDocument workerDocProfile;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id")
    private Department department;

    @Column(length = 120)
    private String passwordHash;

    @Column(nullable = false)
    private boolean autoReset;

    private Integer resetPeriodDays;
    private LocalDateTime lastPasswordChange;
    private LocalDateTime nextResetDate;

    @Column(nullable = false)
    private boolean mustChangeOnNextLogin;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AccountStatus accountStatus;

    @Column(nullable = false)
    private boolean active;

    @Column(nullable = false)
    private Integer maxActiveSessions;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "worker_roles", joinColumns = @JoinColumn(name = "worker_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "role_name")
    private Set<RoleName> roles = new HashSet<>();

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
        if (this.maxActiveSessions == null || this.maxActiveSessions < 1) {
            this.maxActiveSessions = DEFAULT_MAX_ACTIVE_SESSIONS;
        }
    }

    @PreUpdate
    void onUpdate() {
        if (this.maxActiveSessions == null || this.maxActiveSessions < 1) {
            this.maxActiveSessions = DEFAULT_MAX_ACTIVE_SESSIONS;
        }
        this.updatedAt = LocalDateTime.now();
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public Gender getGender() {
        return gender;
    }

    public void setGender(Gender gender) {
        this.gender = gender;
    }

    public LocalDate getDateOfBirth() {
        return dateOfBirth;
    }

    public void setDateOfBirth(LocalDate dateOfBirth) {
        this.dateOfBirth = dateOfBirth;
    }

    public String getProfilePhotoUrl() {
        return profilePhotoUrl;
    }

    public void setProfilePhotoUrl(String profilePhotoUrl) {
        this.profilePhotoUrl = profilePhotoUrl;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public WorkerDocument getWorkerDocProfile() {
        return workerDocProfile;
    }

    public void setWorkerDocProfile(WorkerDocument workerDocProfile) {
        this.workerDocProfile = workerDocProfile;
    }

    public Department getDepartment() {
        return department;
    }

    public void setDepartment(Department department) {
        this.department = department;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public boolean isAutoReset() {
        return autoReset;
    }

    public void setAutoReset(boolean autoReset) {
        this.autoReset = autoReset;
    }

    public Integer getResetPeriodDays() {
        return resetPeriodDays;
    }

    public void setResetPeriodDays(Integer resetPeriodDays) {
        this.resetPeriodDays = resetPeriodDays;
    }

    public LocalDateTime getLastPasswordChange() {
        return lastPasswordChange;
    }

    public void setLastPasswordChange(LocalDateTime lastPasswordChange) {
        this.lastPasswordChange = lastPasswordChange;
    }

    public LocalDateTime getNextResetDate() {
        return nextResetDate;
    }

    public void setNextResetDate(LocalDateTime nextResetDate) {
        this.nextResetDate = nextResetDate;
    }

    public boolean isMustChangeOnNextLogin() {
        return mustChangeOnNextLogin;
    }

    public void setMustChangeOnNextLogin(boolean mustChangeOnNextLogin) {
        this.mustChangeOnNextLogin = mustChangeOnNextLogin;
    }

    public AccountStatus getAccountStatus() {
        return accountStatus;
    }

    public void setAccountStatus(AccountStatus accountStatus) {
        this.accountStatus = accountStatus;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public Integer getMaxActiveSessions() {
        return maxActiveSessions;
    }

    public void setMaxActiveSessions(Integer maxActiveSessions) {
        this.maxActiveSessions = maxActiveSessions;
    }

    public Set<RoleName> getRoles() {
        return roles;
    }

    public void setRoles(Set<RoleName> roles) {
        this.roles = roles;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
