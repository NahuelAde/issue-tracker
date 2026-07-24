package com.nahuel.issuetracker.repository;

import com.nahuel.issuetracker.entity.Incident;
import com.nahuel.issuetracker.entity.Project;
import com.nahuel.issuetracker.entity.Sprint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface IncidentRepository
        extends JpaRepository<Incident, Long>, JpaSpecificationExecutor<Incident> {

    /**
     * Loads an incident together with its project, so the project can be read
     * outside the transaction (open-in-view is disabled).
     */
    @Query("select i from Incident i join fetch i.project left join fetch i.sprint where i.id = :id")
    Optional<Incident> findByIdWithProject(@Param("id") Long id);

    boolean existsByProjectAndCodeIgnoreCase(Project project, String code);

    boolean existsByProjectAndCodeIgnoreCaseAndIdNot(Project project, String code, Long id);

    long countByProject(Project project);

    long countBySprint(Sprint sprint);

    @Modifying
    @Query("update Incident i set i.sprint = null where i.sprint = :sprint")
    void clearSprint(@Param("sprint") Sprint sprint);

    @Query("""
            select distinct i.category from Incident i
            where i.project = :project and i.category is not null and i.category <> ''
            order by i.category""")
    List<String> findDistinctCategories(@Param("project") Project project);

    @Query("""
            select distinct i.assignee from Incident i
            where i.project = :project and i.assignee is not null and i.assignee <> ''
            order by i.assignee""")
    List<String> findDistinctAssignees(@Param("project") Project project);

    @Query("select i.code from Incident i where i.project = :project order by i.code")
    List<String> findCodesByProject(@Param("project") Project project);
}
