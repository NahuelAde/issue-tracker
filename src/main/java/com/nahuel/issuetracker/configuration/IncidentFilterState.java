package com.nahuel.issuetracker.configuration;

import com.nahuel.issuetracker.enums.IncidentPriority;
import com.nahuel.issuetracker.enums.IncidentType;
import com.vaadin.flow.spring.annotation.VaadinSessionScope;
import org.springframework.stereotype.Component;

import java.io.Serializable;

/**
 * Keeps the incident list filters for the duration of the session, so they are
 * preserved when navigating to an incident detail and back. The sprint is stored
 * by id to stay valid across reloads.
 */
@Component
@VaadinSessionScope
public class IncidentFilterState implements Serializable {

    private String searchText;
    /** "OPEN" (agregado "Abiertas"), el nombre de un {@code IncidentStatus}, o null (sin
     * filtrar). Sustituye a los antiguos campos separados {@code status} y {@code onlyOpen}. */
    private String statusFilter = "OPEN";
    private IncidentPriority priority;
    private String category;
    private Long sprintId;
    private IncidentType type;
    private String assignee;
    private Long excludeSprintId;
    private String developmentPhase;

    public String getSearchText() {
        return searchText;
    }

    public void setSearchText(String searchText) {
        this.searchText = searchText;
    }

    public String getStatusFilter() {
        return statusFilter;
    }

    public void setStatusFilter(String statusFilter) {
        this.statusFilter = statusFilter;
    }

    public IncidentPriority getPriority() {
        return priority;
    }

    public void setPriority(IncidentPriority priority) {
        this.priority = priority;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public Long getSprintId() {
        return sprintId;
    }

    public void setSprintId(Long sprintId) {
        this.sprintId = sprintId;
    }

    public IncidentType getType() {
        return type;
    }

    public void setType(IncidentType type) {
        this.type = type;
    }

    public String getAssignee() {
        return assignee;
    }

    public void setAssignee(String assignee) {
        this.assignee = assignee;
    }

    public Long getExcludeSprintId() {
        return excludeSprintId;
    }

    public void setExcludeSprintId(Long excludeSprintId) {
        this.excludeSprintId = excludeSprintId;
    }

    public String getDevelopmentPhase() {
        return developmentPhase;
    }

    public void setDevelopmentPhase(String developmentPhase) {
        this.developmentPhase = developmentPhase;
    }
}
