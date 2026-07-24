package com.nahuel.issuetracker.enums;

/** Kind of work an incident represents. */
public enum IncidentType {

    BUG("Error"),
    FEATURE("Funcionalidad"),
    IMPROVEMENT("Mejora"),
    TASK("Tarea"),
    QUESTION("Consulta");

    private final String label;

    IncidentType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
