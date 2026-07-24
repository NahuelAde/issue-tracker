package com.nahuel.issuetracker.service;

import com.nahuel.issuetracker.entity.Incident;
import com.nahuel.issuetracker.entity.Project;
import com.nahuel.issuetracker.entity.Sprint;
import com.nahuel.issuetracker.enums.IncidentPriority;
import com.nahuel.issuetracker.enums.IncidentStatus;
import com.nahuel.issuetracker.enums.IncidentType;
import com.nahuel.issuetracker.repository.IncidentEntryRepository;
import com.nahuel.issuetracker.repository.IncidentRepository;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Business logic for incidents. Enforces code uniqueness within a project and
 * basic invariants, and provides filtered search for the list view.
 */
@Service
@Transactional
public class IncidentService {

    private final IncidentRepository repository;
    private final IncidentEntryRepository entryRepository;

    public IncidentService(IncidentRepository repository, IncidentEntryRepository entryRepository) {
        this.repository = repository;
        this.entryRepository = entryRepository;
    }

    /** Physically deletes an incident and all its entries. */
    public void delete(Long id) {
        repository.findById(id).ifPresent(incident -> {
            entryRepository.deleteByIncident(incident);
            repository.delete(incident);
        });
    }

    @Transactional(readOnly = true)
    public Optional<Incident> findById(Long id) {
        // Fetch the project eagerly; the detail view reads it outside the
        // transaction (open-in-view is disabled).
        return repository.findByIdWithProject(id);
    }

    @Transactional(readOnly = true)
    public long countByProject(Project project) {
        return repository.countByProject(project);
    }

    @Transactional(readOnly = true)
    public List<String> findDistinctCategories(Project project) {
        return repository.findDistinctCategories(project);
    }

    @Transactional(readOnly = true)
    public List<String> findDistinctAssignees(Project project) {
        return repository.findDistinctAssignees(project);
    }

    @Transactional(readOnly = true)
    public List<String> findCodes(Project project) {
        return repository.findCodesByProject(project);
    }

    /**
     * Returns the incidents of a project matching the given filters, ordered by
     * last update (newest first). Any null/blank filter is ignored.
     */
    @Transactional(readOnly = true)
    public List<Incident> search(Project project, String text, IncidentStatus status,
            IncidentPriority priority, String category, Sprint sprint, IncidentType type,
            String assignee, boolean onlyOpen) {
        Specification<Incident> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("project"), project));
            predicates.add(cb.isTrue(root.get("active")));
            if (onlyOpen) {
                predicates.add(cb.notEqual(root.get("status"), IncidentStatus.CLOSED));
            }
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (priority != null) {
                predicates.add(cb.equal(root.get("priority"), priority));
            }
            if (sprint != null) {
                predicates.add(cb.equal(root.get("sprint"), sprint));
            }
            if (type != null) {
                predicates.add(cb.equal(root.get("type"), type));
            }
            if (assignee != null && !assignee.isBlank()) {
                predicates.add(cb.equal(root.get("assignee"), assignee));
            }
            if (category != null && !category.isBlank()) {
                predicates.add(cb.equal(root.get("category"), category));
            }
            if (text != null && !text.isBlank()) {
                String like = "%" + text.trim().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("code")), like),
                        cb.like(cb.lower(root.get("title")), like),
                        cb.like(cb.lower(cb.coalesce(root.get("category"), "")), like),
                        cb.like(cb.lower(cb.coalesce(root.get("externalReference"), "")), like)));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        return repository.findAll(spec, Sort.by(Sort.Direction.DESC, "updatedAt"));
    }

    /**
     * Creates or updates an incident after validating code uniqueness within its
     * project and applying basic invariants.
     */
    public Incident save(Incident incident) {
        normalize(incident);
        validateUniqueness(incident);
        applyInvariants(incident);
        return repository.save(incident);
    }

    /** Closes the incident (status CLOSED, closedAt set by {@link #save}). */
    public Incident close(Incident incident) {
        incident.setStatus(IncidentStatus.CLOSED);
        return save(incident);
    }

    /** Reopens a closed incident, moving it back to IN_PROGRESS and clearing closedAt. */
    public Incident reopen(Incident incident) {
        incident.setStatus(IncidentStatus.IN_PROGRESS);
        incident.setClosedAt(null);
        return save(incident);
    }

    private void normalize(Incident incident) {
        if (incident.getCode() != null) {
            incident.setCode(incident.getCode().trim());
        }
        if (incident.getTitle() != null) {
            incident.setTitle(incident.getTitle().trim());
        }
        if (incident.getCategory() != null) {
            incident.setCategory(incident.getCategory().trim());
        }
        if (incident.getExternalReference() != null) {
            incident.setExternalReference(incident.getExternalReference().trim());
        }
        if (incident.getAssignee() == null || incident.getAssignee().isBlank()) {
            incident.setAssignee("Sin asignar");
        } else {
            incident.setAssignee(incident.getAssignee().trim());
        }
    }

    private void validateUniqueness(Incident incident) {
        Long id = incident.getId();
        boolean codeTaken = id == null
                ? repository.existsByProjectAndCodeIgnoreCase(incident.getProject(), incident.getCode())
                : repository.existsByProjectAndCodeIgnoreCaseAndIdNot(
                        incident.getProject(), incident.getCode(), id);
        if (codeTaken) {
            throw new IllegalArgumentException(
                    "Ya existe una incidencia con el código \"" + incident.getCode()
                            + "\" en este proyecto");
        }
    }

    /** Domain invariants, applied on every save so manual status changes stay consistent. */
    private void applyInvariants(Incident incident) {
        if (incident.isFinished()) {
            incident.setStarted(true);
        }
        if (!incident.isBlocked()) {
            incident.setBlockedReason(null);
        }
        // Commitment only makes sense when the incident belongs to a sprint.
        if (incident.getSprint() == null) {
            incident.setSprintCommitment(null);
        }
        // Closing sets closedAt once; any non-closed status clears it (reopened).
        if (incident.getStatus() == IncidentStatus.CLOSED) {
            if (incident.getClosedAt() == null) {
                incident.setClosedAt(LocalDateTime.now());
            }
        } else {
            incident.setClosedAt(null);
        }
    }
}
