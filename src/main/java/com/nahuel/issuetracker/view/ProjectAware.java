package com.nahuel.issuetracker.view;

/**
 * Implemented by views that depend on the currently selected project, so the
 * {@link MainLayout} can tell them to reload when the header selector changes.
 */
public interface ProjectAware {

    void onProjectChanged();
}
