package com.nahuel.issuetracker.enums;

/**
 * Lifecycle status of an incident. {@code CLOSED} means the incident is finished;
 * every other value is considered "open". "Blocked" is not a status: it is a
 * cross-cutting flag on the incident ({@code blocked} + {@code blockedReason}).
 */
public enum IncidentStatus {

    PENDING("Pendiente"),
    IN_PROGRESS("En curso"),
    IN_TESTING("En pruebas"),
    WAITING_CLIENT("Pendiente del cliente"),
    READY_FOR_PRE("Preparada para PRE"),
    READY_FOR_PRO("Preparada para PRO"),
    CLOSED("Cerrada");

    private final String label;

    IncidentStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
