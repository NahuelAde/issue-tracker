package com.nahuel.issuetracker.service;

import com.nahuel.issuetracker.entity.Project;
import com.nahuel.issuetracker.repository.IncidentRepository;
import com.nahuel.issuetracker.repository.ProjectRepository;
import com.nahuel.issuetracker.repository.SprintRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Business logic for projects. Enforces case-insensitive uniqueness of name and
 * code, and supports deactivation instead of physical deletion.
 *
 * <p>Validation failures are reported as {@link IllegalArgumentException} with a
 * Spanish message, which the views turn into a {@code Notification}.
 */
@Service
@Transactional
public class ProjectService {

    private final ProjectRepository repository;
    private final IncidentRepository incidentRepository;
    private final SprintRepository sprintRepository;

    public ProjectService(ProjectRepository repository, IncidentRepository incidentRepository,
            SprintRepository sprintRepository) {
        this.repository = repository;
        this.incidentRepository = incidentRepository;
        this.sprintRepository = sprintRepository;
    }

    /**
     * Physically deletes a project. Only allowed when it has no incidents and no
     * sprints, to avoid accidental mass deletion (deactivate it otherwise).
     */
    public void delete(Long id) {
        Project project = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("El proyecto no existe"));
        if (incidentRepository.countByProject(project) > 0) {
            throw new IllegalArgumentException(
                    "No se puede eliminar: el proyecto tiene incidencias. Elimínalas o desactiva el proyecto.");
        }
        if (sprintRepository.countByProject(project) > 0) {
            throw new IllegalArgumentException(
                    "No se puede eliminar: el proyecto tiene sprints. Elimínalos primero.");
        }
        repository.delete(project);
    }

    @Transactional(readOnly = true)
    public List<Project> findActiveProjects() {
        return repository.findByActiveTrueOrderByNameAsc();
    }

    @Transactional(readOnly = true)
    public List<Project> findAllProjects() {
        return repository.findAllByOrderByNameAsc();
    }

    @Transactional(readOnly = true)
    public Optional<Project> findById(Long id) {
        return repository.findById(id);
    }

    @Transactional(readOnly = true)
    public long count() {
        return repository.count();
    }

    /**
     * Creates or updates a project after validating uniqueness. Trims name and
     * code before saving.
     */
    public Project save(Project project) {
        normalize(project);
        validateUniqueness(project);
        return repository.save(project);
    }

    public Project setActive(Long id, boolean active) {
        Project project = repository.findById(id).orElseThrow(
                () -> new IllegalArgumentException("El proyecto no existe"));
        project.setActive(active);
        return repository.save(project);
    }

    private void normalize(Project project) {
        if (project.getName() != null) {
            project.setName(project.getName().trim());
        }
        if (project.getCode() != null) {
            project.setCode(project.getCode().trim());
        }
        if (project.getDescription() != null) {
            project.setDescription(project.getDescription().trim());
        }
    }

    private void validateUniqueness(Project project) {
        Long id = project.getId();
        boolean nameTaken = id == null
                ? repository.existsByNameIgnoreCase(project.getName())
                : repository.existsByNameIgnoreCaseAndIdNot(project.getName(), id);
        if (nameTaken) {
            throw new IllegalArgumentException(
                    "Ya existe un proyecto con el nombre \"" + project.getName() + "\"");
        }
        boolean codeTaken = id == null
                ? repository.existsByCodeIgnoreCase(project.getCode())
                : repository.existsByCodeIgnoreCaseAndIdNot(project.getCode(), id);
        if (codeTaken) {
            throw new IllegalArgumentException(
                    "Ya existe un proyecto con el código \"" + project.getCode() + "\"");
        }
    }
}
