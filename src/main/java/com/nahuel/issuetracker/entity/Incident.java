package com.nahuel.issuetracker.entity;

import com.nahuel.issuetracker.enums.IncidentPriority;
import com.nahuel.issuetracker.enums.IncidentStatus;
import com.nahuel.issuetracker.enums.IncidentType;
import com.nahuel.issuetracker.enums.SprintCommitment;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.hibernate.annotations.ColumnDefault;

import java.time.LocalDateTime;

/**
 * An incident belonging to a {@link Project}. The {@code code} is unique within
 * its project. Incidents are never physically deleted.
 *
 * <p>Total hours are not stored here; they are computed from the incident's
 * entries (added in a later increment). Likewise, close/reopen handling of
 * {@link #closedAt} is added in a later increment.
 */
@Entity
@Table(name = "incident",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_incident_project_code", columnNames = {"project_id", "code"}),
        indexes = {
                @Index(name = "idx_incident_project", columnList = "project_id"),
                @Index(name = "idx_incident_status", columnList = "status"),
                @Index(name = "idx_incident_updated", columnList = "updated_at"),
                @Index(name = "idx_incident_sprint", columnList = "sprint_id")
        })
public class Incident {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @NotBlank(message = "El código es obligatorio")
    @Size(max = 50, message = "El código no puede superar los 50 caracteres")
    @Column(nullable = false, length = 50)
    private String code;

    @NotBlank(message = "El título es obligatorio")
    @Size(max = 255, message = "El título no puede superar los 255 caracteres")
    @Column(nullable = false, length = 255)
    private String title;

    @Size(max = 100)
    @Column(length = 100)
    private String category;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private IncidentType type;

    @Size(max = 100)
    @Column(length = 100)
    private String assignee;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sprint_id")
    private Sprint sprint;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private SprintCommitment sprintCommitment;

    @NotNull(message = "El estado es obligatorio")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private IncidentStatus status = IncidentStatus.PENDING;

    @NotNull(message = "La prioridad es obligatoria")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private IncidentPriority priority = IncidentPriority.MEDIUM;

    @Size(max = 4000)
    @Column(length = 4000)
    private String resolution;

    @Size(max = 4000)
    @Column(length = 4000)
    private String tests;

    @Size(max = 255)
    @Column(length = 255)
    private String externalReference;

    @Column(nullable = false)
    private boolean blocked;

    @Size(max = 1000)
    @Column(length = 1000)
    private String blockedReason;

    @Column(nullable = false)
    private boolean started;

    @Column(nullable = false)
    private boolean finished;

    @Column(nullable = false)
    private boolean testedLocal;

    @Column(nullable = false)
    private boolean testedPre;

    @Column(nullable = false)
    private boolean frontendAffected;

    @Column(nullable = false)
    private boolean backendAffected;

    @Column(nullable = false)
    private boolean configurationAffected;

    @Column(nullable = false)
    private boolean databaseAffected;

    @Column(nullable = false)
    @ColumnDefault("false")
    private boolean testCasesDone;

    @Column(nullable = false)
    @ColumnDefault("false")
    private boolean testEvidenceDone;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    private LocalDateTime closedAt;

    @Column(nullable = false)
    private boolean active = true;

    @PrePersist
    void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = createdAt;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public Project getProject() {
        return project;
    }

    public void setProject(Project project) {
        this.project = project;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public IncidentType getType() {
        return type;
    }

    public void setType(IncidentType type) {
        this.type = type;
    }

    public String getAssignee() {
        return assignee;
    }

    public void setAssignee(String assignee) {
        this.assignee = assignee;
    }

    public Sprint getSprint() {
        return sprint;
    }

    public void setSprint(Sprint sprint) {
        this.sprint = sprint;
    }

    public SprintCommitment getSprintCommitment() {
        return sprintCommitment;
    }

    public void setSprintCommitment(SprintCommitment sprintCommitment) {
        this.sprintCommitment = sprintCommitment;
    }

    public IncidentStatus getStatus() {
        return status;
    }

    public void setStatus(IncidentStatus status) {
        this.status = status;
    }

    public IncidentPriority getPriority() {
        return priority;
    }

    public void setPriority(IncidentPriority priority) {
        this.priority = priority;
    }

    public String getResolution() {
        return resolution;
    }

    public void setResolution(String resolution) {
        this.resolution = resolution;
    }

    public String getTests() {
        return tests;
    }

    public void setTests(String tests) {
        this.tests = tests;
    }

    public String getExternalReference() {
        return externalReference;
    }

    public void setExternalReference(String externalReference) {
        this.externalReference = externalReference;
    }

    public boolean isBlocked() {
        return blocked;
    }

    public void setBlocked(boolean blocked) {
        this.blocked = blocked;
    }

    public String getBlockedReason() {
        return blockedReason;
    }

    public void setBlockedReason(String blockedReason) {
        this.blockedReason = blockedReason;
    }

    public boolean isStarted() {
        return started;
    }

    public void setStarted(boolean started) {
        this.started = started;
    }

    public boolean isFinished() {
        return finished;
    }

    public void setFinished(boolean finished) {
        this.finished = finished;
    }

    public boolean isTestedLocal() {
        return testedLocal;
    }

    public void setTestedLocal(boolean testedLocal) {
        this.testedLocal = testedLocal;
    }

    public boolean isTestedPre() {
        return testedPre;
    }

    public void setTestedPre(boolean testedPre) {
        this.testedPre = testedPre;
    }

    public boolean isFrontendAffected() {
        return frontendAffected;
    }

    public void setFrontendAffected(boolean frontendAffected) {
        this.frontendAffected = frontendAffected;
    }

    public boolean isBackendAffected() {
        return backendAffected;
    }

    public void setBackendAffected(boolean backendAffected) {
        this.backendAffected = backendAffected;
    }

    public boolean isConfigurationAffected() {
        return configurationAffected;
    }

    public void setConfigurationAffected(boolean configurationAffected) {
        this.configurationAffected = configurationAffected;
    }

    public boolean isDatabaseAffected() {
        return databaseAffected;
    }

    public void setDatabaseAffected(boolean databaseAffected) {
        this.databaseAffected = databaseAffected;
    }

    public boolean isTestCasesDone() {
        return testCasesDone;
    }

    public void setTestCasesDone(boolean testCasesDone) {
        this.testCasesDone = testCasesDone;
    }

    public boolean isTestEvidenceDone() {
        return testEvidenceDone;
    }

    public void setTestEvidenceDone(boolean testEvidenceDone) {
        this.testEvidenceDone = testEvidenceDone;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public LocalDateTime getClosedAt() {
        return closedAt;
    }

    public void setClosedAt(LocalDateTime closedAt) {
        this.closedAt = closedAt;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Incident other)) {
            return false;
        }
        return id != null && id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
