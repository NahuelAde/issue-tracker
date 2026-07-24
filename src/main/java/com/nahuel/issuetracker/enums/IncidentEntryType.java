package com.nahuel.issuetracker.enums;

/**
 * Type of a chronological entry within an incident. {@code PRE_DEPLOYMENT} and
 * {@code PRO_DEPLOYMENT} are used to derive the last PRE/PRO deployments.
 */
public enum IncidentEntryType {

    ANALYSIS("Análisis"),
    DEVELOPMENT("Desarrollo"),
    CLIENT_COMMENT("Comentario del cliente"),
    INTERNAL_COMMENT("Comentario interno"),
    LOCAL_TEST("Prueba en local"),
    PRE_TEST("Prueba en PRE"),
    PRE_DEPLOYMENT("Despliegue PRE"),
    PRO_DEPLOYMENT("Despliegue PRO"),
    MEETING("Reunión"),
    OTHER("Otro");

    private final String label;

    IncidentEntryType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
