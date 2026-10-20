package com.nahuel.issuetracker.service;

import com.nahuel.issuetracker.entity.Incident;
import com.nahuel.issuetracker.entity.IncidentEntry;
import com.nahuel.issuetracker.enums.IncidentEntryType;
import com.nahuel.issuetracker.repository.IncidentEntryRepository;
import com.nahuel.issuetracker.utils.Formats;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Business logic for incident entries: listing, hours totals and last PRE/PRO
 * deployments. Total hours are always computed from entries, never stored.
 */
@Service
@Transactional
public class IncidentEntryService {

    public record Overview(Map<Long, BigDecimal> hoursByIncident,
                           Map<Long, List<String>> tooltipsByIncident) {
    }

    private final IncidentEntryRepository repository;

    public IncidentEntryService(IncidentEntryRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<IncidentEntry> findByIncident(Incident incident) {
        return repository.findByIncidentOrderByEntryDateDescCreatedAtDesc(incident);
    }

    @Transactional(readOnly = true)
    public Optional<IncidentEntry> findById(Long id) {
        return repository.findById(id);
    }

    @Transactional(readOnly = true)
    public BigDecimal getTotalHours(Incident incident) {
        return repository.sumHoursByIncident(incident);
    }

    @Transactional(readOnly = true)
    public Optional<IncidentEntry> getLastDeployment(Incident incident, IncidentEntryType type) {
        return repository.findFirstByIncidentAndEntryTypeOrderByEntryDateDescCreatedAtDesc(incident, type);
    }

    /** Loads hours and tooltip entries for displayed incidents in a single query. */
    @Transactional(readOnly = true)
    public Overview overviewByIncidents(List<Incident> incidents) {
        if (incidents.isEmpty()) {
            return new Overview(Map.of(), Map.of());
        }

        Map<Long, BigDecimal> hoursByIncident = new HashMap<>();
        Map<Long, List<String>> tooltipsByIncident = new LinkedHashMap<>();
        List<Long> incidentIds = incidents.stream().map(Incident::getId).toList();
        for (Object[] row : repository.findSummariesByIncidentIds(incidentIds)) {
            Long incidentId = (Long) row[0];
            LocalDate date = (LocalDate) row[1];
            IncidentEntryType entryType = (IncidentEntryType) row[2];
            String description = (String) row[3];
            BigDecimal hours = (BigDecimal) row[4];
            hoursByIncident.merge(incidentId, hours == null ? BigDecimal.ZERO : hours, BigDecimal::add);
            String header = Formats.date(date)
                    + " · " + (entryType == null ? "" : entryType.getLabel());
            String body = description == null ? "" : description.strip();
            String entryText = body.isEmpty() ? header : header + "\n" + body;
            tooltipsByIncident.computeIfAbsent(incidentId, k -> new ArrayList<>()).add(entryText);
        }
        return new Overview(hoursByIncident, tooltipsByIncident);
    }

    public IncidentEntry save(IncidentEntry entry) {
        if (entry.getEntryDate() == null) {
            entry.setEntryDate(LocalDate.now());
        }
        if (entry.getHours() != null && entry.getHours().signum() < 0) {
            throw new IllegalArgumentException("Las horas no pueden ser negativas");
        }
        if (entry.getBackendVersion() != null) {
            entry.setBackendVersion(entry.getBackendVersion().trim());
        }
        if (entry.getFrontendVersion() != null) {
            entry.setFrontendVersion(entry.getFrontendVersion().trim());
        }
        if (entry.getDescription() != null) {
            entry.setDescription(entry.getDescription().trim());
        }
        return repository.save(entry);
    }

    public void delete(Long id) {
        repository.deleteById(id);
    }
}
