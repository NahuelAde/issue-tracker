package com.nahuel.issuetracker.service;

import com.nahuel.issuetracker.entity.Project;
import com.nahuel.issuetracker.entity.Sprint;
import com.nahuel.issuetracker.repository.IncidentRepository;
import com.nahuel.issuetracker.repository.SprintRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Business logic for sprints (per project). Enforces name uniqueness within the
 * project, a valid date range, non-overlapping date ranges between sprints of the
 * same project, and at most one active sprint per project (so two sprints can never
 * be current/active at once; the next one starts only after the current is
 * deactivated). Sprints are deactivated, not deleted.
 */
@Service
@Transactional
public class SprintService {

    private final SprintRepository repository;
    private final IncidentRepository incidentRepository;

    public SprintService(SprintRepository repository, IncidentRepository incidentRepository) {
        this.repository = repository;
        this.incidentRepository = incidentRepository;
    }

    /** Deletes a sprint after unassigning it from any incidents that referenced it. */
    public void delete(Long id) {
        repository.findById(id).ifPresent(sprint -> {
            incidentRepository.clearSprint(sprint);
            repository.delete(sprint);
        });
    }

    @Transactional(readOnly = true)
    public List<Sprint> findByProject(Project project) {
        return repository.findByProjectOrderByStartDateDescNameAsc(project);
    }

    @Transactional(readOnly = true)
    public List<Sprint> findActiveByProject(Project project) {
        return repository.findByProjectAndActiveTrueOrderByStartDateDescNameAsc(project);
    }

    @Transactional(readOnly = true)
    public Optional<Sprint> findCurrent(Project project) {
        LocalDate today = LocalDate.now();
        return repository
                .findFirstByProjectAndActiveTrueAndStartDateLessThanEqualAndEndDateGreaterThanEqualOrderByStartDateDesc(
                        project, today, today);
    }

    @Transactional(readOnly = true)
    public Optional<Sprint> findById(Long id) {
        return repository.findById(id);
    }

    public Sprint save(Sprint sprint) {
        if (sprint.getName() != null) {
            sprint.setName(sprint.getName().trim());
        }
        validateDates(sprint);
        validateNoOverlap(sprint);
        validateSingleActive(sprint);
        validateUniqueName(sprint);
        return repository.save(sprint);
    }

    public Sprint setActive(Long id, boolean active) {
        Sprint sprint = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("El sprint no existe"));
        sprint.setActive(active);
        validateSingleActive(sprint);
        return repository.save(sprint);
    }

    private void validateDates(Sprint sprint) {
        if (sprint.getStartDate() != null && sprint.getEndDate() != null
                && sprint.getEndDate().isBefore(sprint.getStartDate())) {
            throw new IllegalArgumentException(
                    "La fecha de fin no puede ser anterior a la de inicio");
        }
    }

    private void validateNoOverlap(Sprint sprint) {
        if (sprint.getStartDate() == null || sprint.getEndDate() == null) {
            return;
        }
        Long excludeId = sprint.getId() == null ? -1L : sprint.getId();
        if (repository.existsOverlapping(sprint.getProject(),
                sprint.getStartDate(), sprint.getEndDate(), excludeId)) {
            throw new IllegalArgumentException(
                    "Las fechas se solapan con otro sprint del proyecto");
        }
    }

    private void validateSingleActive(Sprint sprint) {
        if (!sprint.isActive()) {
            return;
        }
        Long excludeId = sprint.getId() == null ? -1L : sprint.getId();
        if (repository.existsByProjectAndActiveTrueAndIdNot(sprint.getProject(), excludeId)) {
            throw new IllegalArgumentException(
                    "Ya hay un sprint activo en el proyecto; desactívalo antes de activar otro");
        }
    }

    private void validateUniqueName(Sprint sprint) {
        Long id = sprint.getId();
        boolean taken = id == null
                ? repository.existsByProjectAndNameIgnoreCase(sprint.getProject(), sprint.getName())
                : repository.existsByProjectAndNameIgnoreCaseAndIdNot(
                        sprint.getProject(), sprint.getName(), id);
        if (taken) {
            throw new IllegalArgumentException(
                    "Ya existe un sprint con el nombre \"" + sprint.getName() + "\" en este proyecto");
        }
    }
}