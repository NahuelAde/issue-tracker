package com.nahuel.issuetracker.enums;

/**
 * Whether an incident was committed at the start of the sprint or came up during
 * it. Only meaningful when the incident is assigned to a sprint.
 */
public enum SprintCommitment {

    PLANNED("Planificada"),
    UNPLANNED("Surgida en sprint");

    private final String label;

    SprintCommitment(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}