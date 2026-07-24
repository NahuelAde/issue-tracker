package com.nahuel.issuetracker.repository;

import com.nahuel.issuetracker.entity.Incident;
import com.nahuel.issuetracker.entity.IncidentEntry;
import com.nahuel.issuetracker.entity.Project;
import com.nahuel.issuetracker.enums.IncidentEntryType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface IncidentEntryRepository extends JpaRepository<IncidentEntry, Long> {

    List<IncidentEntry> findByIncidentOrderByEntryDateDescCreatedAtDesc(Incident incident);

    void deleteByIncident(Incident incident);

    Optional<IncidentEntry> findFirstByIncidentAndEntryTypeOrderByEntryDateDescCreatedAtDesc(
            Incident incident, IncidentEntryType entryType);

    @Query("select coalesce(sum(e.hours), 0) from IncidentEntry e where e.incident = :incident")
    BigDecimal sumHoursByIncident(@Param("incident") Incident incident);

    @Query("""
            select e.incident.id, coalesce(sum(e.hours), 0)
            from IncidentEntry e
            where e.incident.project = :project
            group by e.incident.id""")
    List<Object[]> sumHoursByProject(@Param("project") Project project);

    /** All entries of a project (id, date, type, description) oldest first, for building tooltips. */
    @Query("""
            select e.incident.id, e.entryDate, e.entryType, e.description
            from IncidentEntry e
            where e.incident.project = :project
            order by e.incident.id, e.entryDate asc, e.createdAt asc""")
    List<Object[]> findEntrySummariesByProject(@Param("project") Project project);
}
