package com.nahuel.issuetracker.view.project;

import com.nahuel.issuetracker.configuration.CurrentProject;
import com.nahuel.issuetracker.entity.Project;
import com.nahuel.issuetracker.entity.Sprint;
import com.nahuel.issuetracker.service.SprintService;
import com.nahuel.issuetracker.utils.Formats;
import com.nahuel.issuetracker.utils.Notifications;
import com.nahuel.issuetracker.view.ProjectAware;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import java.time.LocalDate;

/** Sprint management for the currently selected project. */
@Route("sprints")
@PageTitle("Sprints")
public class SprintManagementView extends VerticalLayout implements ProjectAware {

    private final SprintService sprintService;
    private final CurrentProject currentProject;
    private final Grid<Sprint> grid = new Grid<>(Sprint.class, false);
    private final Span info = new Span();
    private final Button newSprint = new Button("Nuevo sprint");

    public SprintManagementView(SprintService sprintService, CurrentProject currentProject) {
        this.sprintService = sprintService;
        this.currentProject = currentProject;
        setSizeFull();

        newSprint.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        newSprint.addClickListener(e -> openDialog(newSprintInstance(), "Nuevo sprint"));
        add(new HorizontalLayout(newSprint), info);

        configureGrid();
        add(grid);
        refresh();
    }

    private void configureGrid() {
        grid.addColumn(Sprint::getName).setHeader("Nombre").setAutoWidth(true);
        grid.addColumn(s -> Formats.date(s.getStartDate())).setHeader("Inicio").setAutoWidth(true);
        grid.addColumn(s -> Formats.date(s.getEndDate())).setHeader("Fin").setAutoWidth(true);
        grid.addColumn(s -> s.isActive() ? "Activo" : "Inactivo").setHeader("Estado").setAutoWidth(true);
        grid.addComponentColumn(this::buildActions).setHeader("Acciones").setAutoWidth(true);
        grid.setSizeFull();
    }

    private HorizontalLayout buildActions(Sprint sprint) {
        Button edit = new Button("Editar", e -> openDialog(sprint, "Editar sprint"));
        Button toggle = new Button(sprint.isActive() ? "Desactivar" : "Activar",
                e -> toggleActive(sprint));
        Button delete = new Button("Eliminar", e -> confirmDelete(sprint));
        delete.addThemeVariants(ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_TERTIARY);
        return new HorizontalLayout(edit, toggle, delete);
    }

    private Sprint newSprintInstance() {
        Sprint sprint = new Sprint();
        sprint.setActive(true);
        sprint.setProject(currentProject.getProject());
        sprint.setStartDate(LocalDate.now());
        sprint.setEndDate(LocalDate.now().plusDays(14));
        return sprint;
    }

    private void openDialog(Sprint sprint, String title) {
        if (currentProject.getProject() == null) {
            Notifications.info("Selecciona un proyecto primero.");
            return;
        }
        SprintDialog dialog = new SprintDialog();
        dialog.setSaveHandler(edited -> {
            try {
                edited.setProject(currentProject.getProject());
                sprintService.save(edited);
                Notifications.success("Sprint guardado");
                refresh();
                return true;
            } catch (IllegalArgumentException ex) {
                Notifications.error(ex.getMessage());
                return false;
            }
        });
        dialog.open(sprint, title);
    }

    private void confirmDelete(Sprint sprint) {
        Dialog confirm = new Dialog();
        confirm.setHeaderTitle("Eliminar sprint");
        confirm.add(new Paragraph("¿Seguro que quieres eliminar el sprint \"" + sprint.getName()
                + "\"? Las incidencias asignadas quedarán sin sprint. No se puede deshacer."));
        Button yes = new Button("Eliminar", e -> {
            confirm.close();
            sprintService.delete(sprint.getId());
            Notifications.info("Sprint eliminado");
            refresh();
        });
        yes.addThemeVariants(ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_PRIMARY);
        Button no = new Button("Cancelar", e -> confirm.close());
        confirm.getFooter().add(no, yes);
        confirm.open();
    }

    private void toggleActive(Sprint sprint) {
        try {
            sprintService.setActive(sprint.getId(), !sprint.isActive());
            refresh();
        } catch (IllegalArgumentException ex) {
            Notifications.error(ex.getMessage());
        }
    }

    private void refresh() {
        Project project = currentProject.getProject();
        newSprint.setEnabled(project != null);
        if (project == null) {
            grid.setItems();
            info.setText("Selecciona un proyecto para gestionar sus sprints.");
            return;
        }
        info.setText("Sprints de " + project.getName());
        grid.setItems(sprintService.findByProject(project));
    }

    @Override
    public void onProjectChanged() {
        refresh();
    }
}