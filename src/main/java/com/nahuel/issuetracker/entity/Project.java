package com.nahuel.issuetracker.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

/**
 * A project groups incidents. Projects are never physically deleted; they are
 * deactivated instead ({@link #active}).
 *
 * <p>{@code name} and {@code code} must be unique ignoring case. The database
 * constraints below are case-sensitive safety nets; the case-insensitive rule is
 * enforced in {@code ProjectService}.
 */
@Entity
@Table(name = "project", uniqueConstraints = {
        @UniqueConstraint(name = "uk_project_name", columnNames = "name"),
        @UniqueConstraint(name = "uk_project_code", columnNames = "code")
})
public class Project {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "El nombre es obligatorio")
    @Size(max = 100, message = "El nombre no puede superar los 100 caracteres")
    @Column(nullable = false, length = 100)
    private String name;

    @NotBlank(message = "El código es obligatorio")
    @Size(max = 30, message = "El código no puede superar los 30 caracteres")
    @Column(nullable = false, length = 30)
    private String code;

    @Size(max = 1000, message = "La descripción no puede superar los 1000 caracteres")
    @Column(length = 1000)
    private String description;

    @Column(nullable = false)
    private boolean active = true;

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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    // equals/hashCode by id so ComboBox and Grid selection match freshly loaded
    // instances (see ComboBox docs: selection relies on equals/hashCode).
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Project other)) {
            return false;
        }
        return id != null && id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
