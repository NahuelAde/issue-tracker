package com.nahuel.issuetracker.service;

import com.nahuel.issuetracker.entity.Incident;
import com.nahuel.issuetracker.entity.IncidentEntry;
import com.nahuel.issuetracker.entity.Project;
import com.nahuel.issuetracker.enums.IncidentEntryType;
import com.nahuel.issuetracker.repository.IncidentEntryRepository;
import com.nahuel.issuetracker.utils.Formats;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
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

    /** Total hours per incident id for a whole project, in a single query (avoids N+1). */
    @Transactional(readOnly = true)
    public Map<Long, BigDecimal> hoursByProject(Project project) {
        Map<Long, BigDecimal> result = new HashMap<>();
        for (Object[] row : repository.sumHoursByProject(project)) {
            result.put((Long) row[0], (BigDecimal) row[1]);
        }
        return result;
    }

    /**
     * A per-incident multi-line summary of all its entries (date · type · excerpt),
     * oldest first, for the list tooltip. Single query to avoid N+1.
     */
    @Transactional(readOnly = true)
    public Map<Long, String> entriesTooltipByProject(Project project) {
        Map<Long, StringBuilder> builders = new LinkedHashMap<>();
        for (Object[] row : repository.findEntrySummariesByProject(project)) {
            Long incidentId = (Long) row[0];
            LocalDate date = (LocalDate) row[1];
            IncidentEntryType entryType = (IncidentEntryType) row[2];
            String description = (String) row[3];
            String header = Formats.date(date)
                    + " · " + (entryType == null ? "" : entryType.getLabel());
            String body = description == null ? "" : description.strip();
            StringBuilder sb = builders.computeIfAbsent(incidentId, k -> new StringBuilder());
            if (!sb.isEmpty()) {
                sb.append("\n\n");
            }
            sb.append(header);
            if (!body.isEmpty()) {
                sb.append("\n").append(body);
            }
        }
        Map<Long, String> result = new HashMap<>();
        builders.forEach((id, sb) -> result.put(id, sb.toString()));
        return result;
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
