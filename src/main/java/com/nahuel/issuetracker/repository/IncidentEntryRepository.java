package com.nahuel.issuetracker.repository;

import com.nahuel.issuetracker.entity.Incident;
import com.nahuel.issuetracker.entity.IncidentEntry;
import com.nahuel.issuetracker.enums.IncidentEntryType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface IncidentEntryRepository extends JpaRepository<IncidentEntry, Long> {

    List<IncidentEntry> findByIncidentOrderByEntryDateDescCreatedAtDesc(Incident incident);

    void deleteByIncident(Incident incident);

    Optional<IncidentEntry> findFirstByIncidentAndEntryTypeOrderByEntryDateDescCreatedAtDesc(
            Incident incident, IncidentEntryType entryType);

    @Query("select coalesce(sum(e.hours), 0) from IncidentEntry e where e.incident = :incident")
    BigDecimal sumHoursByIncident(@Param("incident") Incident incident);

    /** Entry details for displayed incidents, including their hours. */
    @Query("""
            select e.incident.id, e.entryDate, e.entryType, e.description, e.hours
            from IncidentEntry e
            where e.incident.id in :incidentIds
            order by e.incident.id, e.entryDate asc, e.createdAt asc""")
    List<Object[]> findSummariesByIncidentIds(@Param("incidentIds") Collection<Long> incidentIds);
}
