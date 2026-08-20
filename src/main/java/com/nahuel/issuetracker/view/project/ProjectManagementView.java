package com.nahuel.issuetracker.view.project;

import com.nahuel.issuetracker.configuration.CurrentProject;
import com.nahuel.issuetracker.entity.Project;
import com.nahuel.issuetracker.service.IncidentService;
import com.nahuel.issuetracker.service.ProjectService;
import com.nahuel.issuetracker.utils.Notifications;
import com.nahuel.issuetracker.view.MainLayout;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import java.util.List;

/**
 * Project management: list all projects and create, edit, activate or deactivate
 * them. Projects are never physically deleted.
 */
@Route("projects")
@PageTitle("Proyectos")
public class ProjectManagementView extends VerticalLayout {

    private final ProjectService projectService;
    private final IncidentService incidentService;
    private final CurrentProject currentProject;
    private final Grid<Project> grid = new Grid<>(Project.class, false);

    public ProjectManagementView(ProjectService projectService, IncidentService incidentService,
            CurrentProject currentProject) {
        this.projectService = projectService;
        this.incidentService = incidentService;
        this.currentProject = currentProject;
        setSizeFull();

        Button newProject = new Button("Nuevo proyecto", e -> openDialog(newProjectInstance(), "Nuevo proyecto"));
        newProject.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        add(new HorizontalLayout(newProject));

        configureGrid();
        add(grid);

        refresh();
    }

    private void configureGrid() {
        grid.addColumn(Project::getCode).setHeader("Código").setAutoWidth(true).setFlexGrow(0);
        grid.addColumn(Project::getName).setHeader("Nombre").setAutoWidth(true).setFlexGrow(0);
        grid.addColumn(Project::getDescription).setHeader("Descripción").setFlexGrow(1);
        grid.addColumn(project -> project.isActive() ? "Activo" : "Inactivo")
                .setHeader("Estado").setAutoWidth(true).setFlexGrow(0);
        grid.addColumn(project -> incidentService.countByProject(project))
                .setHeader("Incidencias").setAutoWidth(true).setFlexGrow(0);
        grid.addComponentColumn(this::buildActions).setHeader("Acciones").setAutoWidth(true).setFlexGrow(0);
        grid.setSizeFull();
    }

    private HorizontalLayout buildActions(Project project) {
        Button edit = new Button("Editar", e -> openDialog(project, "Editar proyecto"));
        Button toggle = new Button(project.isActive() ? "Desactivar" : "Activar",
                e -> toggleActive(project));
        Button delete = new Button("Eliminar", e -> confirmDelete(project));
        delete.addThemeVariants(ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_TERTIARY);
        return new HorizontalLayout(edit, toggle, delete);
    }

    private Project newProjectInstance() {
        Project project = new Project();
        project.setActive(true);
        return project;
    }

    private void openDialog(Project project, String title) {
        ProjectDialog dialog = new ProjectDialog();
        dialog.setSaveHandler(edited -> {
            try {
                projectService.save(edited);
                showSuccess("Proyecto guardado");
                refresh();
                refreshHeaderSelector();
                return true;
            } catch (IllegalArgumentException ex) {
                showError(ex.getMessage());
                return false;
            }
        });
        dialog.open(project, title);
    }

    private void confirmDelete(Project project) {
        Dialog confirm = new Dialog();
        confirm.setHeaderTitle("Eliminar proyecto");
        confirm.add(new Paragraph("¿Seguro que quieres eliminar el proyecto \""
                + project.getName() + "\"? No se puede deshacer."));
        Button yes = new Button("Eliminar", e -> {
            confirm.close();
            try {
                projectService.delete(project.getId());
                showInfo("Proyecto eliminado");
                refresh();
                refreshHeaderSelector();
            } catch (IllegalArgumentException ex) {
                showError(ex.getMessage());
            }
        });
        yes.addThemeVariants(ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_PRIMARY);
        Button no = new Button("Cancelar", e -> confirm.close());
        confirm.getFooter().add(no, yes);
        confirm.open();
    }

    private void toggleActive(Project project) {
        boolean newActive = !project.isActive();
        projectService.setActive(project.getId(), newActive);

        // Si se desactiva el proyecto actualmente seleccionado, pasar a otro activo.
        if (!newActive && project.equals(currentProject.getProject())) {
            List<Project> active = projectService.findActiveProjects();
            currentProject.setProject(active.isEmpty() ? null : active.get(0));
            if (active.isEmpty()) {
                showInfo("No hay proyectos activos. Crea uno nuevo.");
            }
        }
        refresh();
        refreshHeaderSelector();
    }

    private void refresh() {
        grid.setItems(projectService.findAllProjects());
    }

    private void refreshHeaderSelector() {
        MainLayout layout = findAncestor(MainLayout.class);
        if (layout != null) {
            layout.refreshProjectSelector();
        }
    }

    private void showSuccess(String message) {
        Notifications.success(message);
    }

    private void showError(String message) {
        Notifications.error(message);
    }

    private void showInfo(String message) {
        Notifications.info(message);
    }
}
