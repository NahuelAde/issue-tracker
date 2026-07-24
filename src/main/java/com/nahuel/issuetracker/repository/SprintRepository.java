package com.nahuel.issuetracker.repository;

import com.nahuel.issuetracker.entity.Project;
import com.nahuel.issuetracker.entity.Sprint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface SprintRepository extends JpaRepository<Sprint, Long> {

    List<Sprint> findByProjectOrderByStartDateDescNameAsc(Project project);

    List<Sprint> findByProjectAndActiveTrueOrderByStartDateDescNameAsc(Project project);

    long countByProject(Project project);

    boolean existsByProjectAndNameIgnoreCase(Project project, String name);

    boolean existsByProjectAndNameIgnoreCaseAndIdNot(Project project, String name, Long id);

    /** The active sprint whose date range contains the given day (most recent one). */
    Optional<Sprint> findFirstByProjectAndActiveTrueAndStartDateLessThanEqualAndEndDateGreaterThanEqualOrderByStartDateDesc(
            Project project, LocalDate onOrBefore, LocalDate onOrAfter);

    /**
     * True if another sprint of the project overlaps the given date range.
     * Two ranges overlap when {@code startDate <= otherEnd} and {@code otherStart <= endDate}.
     * Pass a non-existent id (e.g. -1) when checking a not-yet-persisted sprint.
     */
    @Query("""
            select count(s) > 0 from Sprint s
            where s.project = :project
              and s.id <> :excludeId
              and s.startDate <= :endDate
              and s.endDate >= :startDate
            """)
    boolean existsOverlapping(@Param("project") Project project,
                              @Param("startDate") LocalDate startDate,
                              @Param("endDate") LocalDate endDate,
                              @Param("excludeId") Long excludeId);

    /**
     * True if the project already has another active sprint. Pass a non-existent id
     * (e.g. -1) when checking a not-yet-persisted sprint.
     */
    boolean existsByProjectAndActiveTrueAndIdNot(Project project, Long excludeId);
}