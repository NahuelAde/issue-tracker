package com.nahuel.issuetracker.view;

import com.nahuel.issuetracker.configuration.CurrentProject;
import com.nahuel.issuetracker.entity.Project;
import com.nahuel.issuetracker.service.ProjectService;
import com.nahuel.issuetracker.service.SettingService;
import com.nahuel.issuetracker.utils.Notifications;
import com.nahuel.issuetracker.view.incident.IncidentListView;
import com.nahuel.issuetracker.view.project.ProjectDialog;
import com.nahuel.issuetracker.view.project.ProjectManagementView;
import com.nahuel.issuetracker.view.project.SprintManagementView;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.router.Layout;
import com.vaadin.flow.router.RouterLink;

import java.util.List;

/**
 * Application root layout. Registered with {@link Layout} so every view is
 * rendered inside it automatically. The header hosts the active-project selector
 * and the project actions.
 */
@Layout
public class MainLayout extends AppLayout {

    private final ProjectService projectService;
    private final SettingService settingService;
    private final CurrentProject currentProject;
    private final ComboBox<Project> projectSelector = new ComboBox<>();

    public MainLayout(ProjectService projectService, SettingService settingService,
            CurrentProject currentProject) {
        this.projectService = projectService;
        this.settingService = settingService;
        this.currentProject = currentProject;
        addToNavbar(createHeader());
        refreshProjectSelector();
    }

    private HorizontalLayout createHeader() {
        H1 appTitle = new H1("Gestor de incidencias");
        appTitle.getStyle().set("font-size", "1.125rem").set("margin", "0");
        RouterLink homeLink = new RouterLink();
        homeLink.setRoute(IncidentListView.class);
        homeLink.add(appTitle);
        homeLink.getStyle().set("text-decoration", "none").set("color", "inherit");

        projectSelector.setPlaceholder("Selecciona un proyecto");
        projectSelector.setItemLabelGenerator(Project::getName);
        projectSelector.addValueChangeListener(e -> {
            currentProject.setProject(e.getValue());
            if (e.getValue() != null) {
                settingService.set(SettingService.LAST_PROJECT_ID, String.valueOf(e.getValue().getId()));
            }
            notifyContentProjectChanged();
        });

        Button addProject = new Button("Añadir proyecto", e -> openNewProjectDialog());
        Button manageProjects = new Button("Gestionar proyectos",
                e -> UI.getCurrent().navigate(ProjectManagementView.class));
        Button manageSprints = new Button("Sprints",
                e -> UI.getCurrent().navigate(SprintManagementView.class));

        HorizontalLayout header = new HorizontalLayout(
                homeLink, projectSelector, addProject, manageProjects, manageSprints);
        header.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);
        header.setWidthFull();
        header.getStyle().set("padding", "0 var(--vaadin-padding-m)");
        header.expand(homeLink);
        return header;
    }

    /**
     * Reloads the active projects into the selector and preselects the current
     * one (or the first available). Public so other views can refresh it after
     * creating or (de)activating projects.
     */
    public void refreshProjectSelector() {
        List<Project> active = projectService.findActiveProjects();
        projectSelector.setItems(active);

        Project selected = currentProject.getProject();
        if (selected == null) {
            // Sesión nueva: recuperar el último proyecto usado (persistido entre reinicios).
            selected = loadLastProject(active);
        }
        if (selected != null && active.contains(selected)) {
            projectSelector.setValue(selected);
        } else if (!active.isEmpty()) {
            projectSelector.setValue(active.get(0));
        } else {
            projectSelector.clear();
            currentProject.setProject(null);
        }
    }

    private Project loadLastProject(List<Project> active) {
        return settingService.get(SettingService.LAST_PROJECT_ID)
                .map(Long::valueOf)
                .flatMap(id -> active.stream().filter(p -> id.equals(p.getId())).findFirst())
                .orElse(null);
    }

    private void notifyContentProjectChanged() {
        if (getContent() instanceof ProjectAware projectAware) {
            projectAware.onProjectChanged();
        }
    }

    private void openNewProjectDialog() {
        Project project = new Project();
        project.setActive(true);

        ProjectDialog dialog = new ProjectDialog();
        dialog.setSaveHandler(edited -> {
            try {
                Project saved = projectService.save(edited);
                currentProject.setProject(saved);
                Notifications.success("Proyecto creado");
                refreshProjectSelector();
                return true;
            } catch (IllegalArgumentException ex) {
                Notifications.error(ex.getMessage());
                return false;
            }
        });
        dialog.open(project, "Nuevo proyecto");
    }
}