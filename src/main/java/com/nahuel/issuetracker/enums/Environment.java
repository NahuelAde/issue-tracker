package com.nahuel.issuetracker.enums;

public enum Environment {

    LOCAL("Local"),
    DES("DES"),
    PRE("PRE"),
    PRO("PRO"),
    NOT_APPLICABLE("No aplica");

    private final String label;

    Environment(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
