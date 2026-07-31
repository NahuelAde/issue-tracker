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
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.page.ColorScheme;
import com.vaadin.flow.component.shared.Tooltip;
import com.vaadin.flow.router.Layout;

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
    private final NativeButton themeToggle = new NativeButton();
    private final Tooltip themeToggleTooltip = Tooltip.forComponent(themeToggle);
    private boolean darkMode;

    public MainLayout(ProjectService projectService, SettingService settingService,
            CurrentProject currentProject) {
        this.projectService = projectService;
        this.settingService = settingService;
        this.currentProject = currentProject;
        this.darkMode = Boolean.parseBoolean(settingService.get(SettingService.DARK_MODE).orElse("false"));
        addToNavbar(createHeader());
        applyColorScheme();
        refreshProjectSelector();
    }

    private HorizontalLayout createHeader() {
        Icon wrenchIcon = VaadinIcon.WRENCH.create();
        wrenchIcon.getStyle().set("color", "#ffffff");

        Button homeLink = new Button("Gestor de incidencias",
                e -> UI.getCurrent().navigate(IncidentListView.class));
        homeLink.setIcon(wrenchIcon);
        homeLink.addClassName("app-title-button");

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
                homeLink, projectSelector, addProject, manageProjects, manageSprints, createThemeToggle());
        header.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);
        header.setWidthFull();
        header.getStyle().set("padding", "0 var(--vaadin-padding-m)");
        header.expand(homeLink);
        return header;
    }

    /** Interruptor claro/oscuro de la cabecera: una "pastilla" con un círculo que se
     * desplaza a un lado u otro, en vez de un botón con icono. */
    private NativeButton createThemeToggle() {
        Span thumb = new Span();
        thumb.addClassName("theme-toggle-thumb");
        themeToggle.addClassName("theme-toggle");
        themeToggle.setAriaLabel("Cambiar entre modo claro y oscuro");
        themeToggle.add(thumb);
        themeToggle.addClickListener(e -> toggleDarkMode());
        updateThemeToggleState();
        return themeToggle;
    }

    private void toggleDarkMode() {
        darkMode = !darkMode;
        settingService.set(SettingService.DARK_MODE, String.valueOf(darkMode));
        applyColorScheme();
        updateThemeToggleState();
    }

    private void applyColorScheme() {
        UI.getCurrent().getPage().setColorScheme(darkMode ? ColorScheme.Value.DARK : ColorScheme.Value.LIGHT);
    }

    private void updateThemeToggleState() {
        themeToggle.getClassNames().set("theme-toggle-dark", darkMode);
        themeToggleTooltip.setText(darkMode ? "Cambiar a modo claro" : "Cambiar a modo oscuro");
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