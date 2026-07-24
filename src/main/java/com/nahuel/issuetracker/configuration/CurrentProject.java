package com.nahuel.issuetracker.configuration;

import com.nahuel.issuetracker.entity.Project;
import com.vaadin.flow.spring.annotation.VaadinSessionScope;
import org.springframework.stereotype.Component;

import java.io.Serializable;

/**
 * Holds the project currently selected in the header, for the duration of the
 * Vaadin session. Reset when the session ends.
 */
@Component
@VaadinSessionScope
public class CurrentProject implements Serializable {

    private Project project;

    public Project getProject() {
        return project;
    }

    public void setProject(Project project) {
        this.project = project;
    }
}
