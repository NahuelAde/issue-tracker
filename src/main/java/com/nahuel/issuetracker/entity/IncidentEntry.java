package com.nahuel.issuetracker.entity;

import com.nahuel.issuetracker.enums.Environment;
import com.nahuel.issuetracker.enums.IncidentEntryType;
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
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * A chronological entry of an incident (analysis, development, test, deployment,
 * etc.). The incident's total hours are the sum of its entries' {@link #hours}.
 */
@Entity
@Table(name = "incident_entry", indexes = {
        @Index(name = "idx_entry_incident", columnList = "incident_id"),
        @Index(name = "idx_entry_date", columnList = "entry_date")
})
public class IncidentEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "incident_id", nullable = false)
    private Incident incident;

    @NotNull(message = "La fecha es obligatoria")
    @Column(name = "entry_date", nullable = false)
    private LocalDate entryDate;

    @NotNull(message = "El tipo es obligatorio")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private IncidentEntryType entryType;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private Environment environment;

    @Size(max = 50)
    @Column(name = "backend_version", length = 50)
    private String backendVersion;

    @Size(max = 50)
    @Column(name = "frontend_version", length = 50)
    private String frontendVersion;

    @NotBlank(message = "La descripción es obligatoria")
    @Size(max = 4000)
    @Column(nullable = false, length = 4000)
    private String description;

    @DecimalMin(value = "0.0", message = "Las horas no pueden ser negativas")
    @Digits(integer = 6, fraction = 2, message = "Máximo dos decimales")
    @Column(precision = 8, scale = 2)
    private BigDecimal hours;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

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

    public Incident getIncident() {
        return incident;
    }

    public void setIncident(Incident incident) {
        this.incident = incident;
    }

    public LocalDate getEntryDate() {
        return entryDate;
    }

    public void setEntryDate(LocalDate entryDate) {
        this.entryDate = entryDate;
    }

    public IncidentEntryType getEntryType() {
        return entryType;
    }

    public void setEntryType(IncidentEntryType entryType) {
        this.entryType = entryType;
    }

    public Environment getEnvironment() {
        return environment;
    }

    public void setEnvironment(Environment environment) {
        this.environment = environment;
    }

    public String getBackendVersion() {
        return backendVersion;
    }

    public void setBackendVersion(String backendVersion) {
        this.backendVersion = backendVersion;
    }

    public String getFrontendVersion() {
        return frontendVersion;
    }

    public void setFrontendVersion(String frontendVersion) {
        this.frontendVersion = frontendVersion;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public BigDecimal getHours() {
        return hours;
    }

    public void setHours(BigDecimal hours) {
        this.hours = hours;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof IncidentEntry other)) {
            return false;
        }
        return id != null && id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
