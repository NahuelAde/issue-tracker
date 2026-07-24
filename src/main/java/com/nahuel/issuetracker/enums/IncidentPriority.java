package com.nahuel.issuetracker.enums;

public enum IncidentPriority {

    LOW("Baja"),
    MEDIUM("Media"),
    HIGH("Alta"),
    URGENT("Urgente");

    private final String label;

    IncidentPriority(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
