package com.nahuel.issuetracker.view.incident;

import com.nahuel.issuetracker.configuration.CurrentProject;
import com.nahuel.issuetracker.entity.Incident;
import com.nahuel.issuetracker.entity.IncidentEntry;
import com.nahuel.issuetracker.entity.Project;
import com.nahuel.issuetracker.entity.Sprint;
import com.nahuel.issuetracker.enums.Environment;
import com.nahuel.issuetracker.enums.IncidentEntryType;
import com.nahuel.issuetracker.enums.IncidentPriority;
import com.nahuel.issuetracker.enums.IncidentStatus;
import com.nahuel.issuetracker.enums.IncidentType;
import com.nahuel.issuetracker.enums.SprintCommitment;
import com.nahuel.issuetracker.service.IncidentEntryService;
import com.nahuel.issuetracker.service.IncidentService;
import com.nahuel.issuetracker.service.SprintService;
import com.nahuel.issuetracker.utils.Formats;
import com.nahuel.issuetracker.utils.Notifications;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.data.binder.BeanValidationBinder;
import com.vaadin.flow.router.BeforeEvent;
import com.vaadin.flow.router.BeforeLeaveEvent;
import com.vaadin.flow.router.BeforeLeaveObserver;
import com.vaadin.flow.router.HasUrlParameter;
import com.vaadin.flow.router.OptionalParameter;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Create/edit an incident. Route {@code incident} (new) or {@code incident/{id}}
 * (edit). Blocks: A main data, B evolution, C summary (computed), D chronological
 * entries. Resolution/tests and close/reopen come in a later increment.
 */
@Route("incident")
@PageTitle("Incidencia")
public class IncidentDetailView extends VerticalLayout
        implements HasUrlParameter<Long>, BeforeLeaveObserver {

    private final IncidentService incidentService;
    private final IncidentEntryService entryService;
    private final SprintService sprintService;
    private final CurrentProject currentProject;

    private final BeanValidationBinder<Incident> binder = new BeanValidationBinder<>(Incident.class);

    private final H2 heading = new H2();

    // Block A
    private static final ComboBox.ItemFilter<String> CODE_FILTER =
            (item, filter) -> item.toLowerCase().startsWith(filter.toLowerCase());

    private final TextField projectField = new TextField("Proyecto");
    private final ComboBox<String> code = new ComboBox<>("Código");
    private final TextField title = new TextField("Título");
    private final ComboBox<String> category = new ComboBox<>("Categoría");
    private final ComboBox<IncidentType> type = new ComboBox<>("Tipo");
    private final ComboBox<String> assignee = new ComboBox<>("Asignada a");
    private final Select<IncidentStatus> status = new Select<>();
    private final Select<IncidentPriority> priority = new Select<>();
    private final TextField externalReference = new TextField("Referencia externa");
    private final ComboBox<Sprint> sprint = new ComboBox<>("Sprint");
    private final ComboBox<SprintCommitment> commitment = new ComboBox<>("Planificación");

    // Block B
    private final Checkbox started = new Checkbox("Empezada");
    private final Checkbox finished = new Checkbox("Terminada");
    private final Checkbox testedLocal = new Checkbox("Probada en local");
    private final Checkbox testedPre = new Checkbox("Probada en PRE");
    private final Checkbox frontendAffected = new Checkbox("Frontend");
    private final Checkbox backendAffected = new Checkbox("Backend");
    private final Checkbox configurationAffected = new Checkbox("Configuración");
    private final Checkbox databaseAffected = new Checkbox("BBDD");
    private final Checkbox blocked = new Checkbox("Bloqueada");
    private final TextArea blockedReason = new TextArea("Motivo del bloqueo");
    private final Checkbox testCases = new Checkbox("Casos de prueba");
    private final Checkbox testEvidence = new Checkbox("Evidencias");

    // Block C (summary)
    private final Span hoursTotal = new Span();
    private final Span createdAtSpan = new Span();
    private final Span updatedAtSpan = new Span();
    private final Span closedAtSpan = new Span();
    private final Span lastPre = new Span();
    private final Span lastPro = new Span();
    private final VerticalLayout summarySection = new VerticalLayout();

    // Block D (entries)
    private final Grid<IncidentEntry> entriesGrid = new Grid<>(IncidentEntry.class, false);
    private final VerticalLayout entriesSection = new VerticalLayout();
    private final Paragraph newHint = new Paragraph(
            "Al añadir la primera entrada se guardará automáticamente la incidencia.");

    // Block E (resolution & tests)
    private final TextArea resolution = new TextArea("Resolución");
    private final TextArea tests = new TextArea("Pruebas");

    // Actions
    private final Button save = new Button("Guardar");
    private final Button closeButton = new Button("Cerrar incidencia");
    private final Button reopenButton = new Button("Reabrir incidencia");
    private final Button deleteButton = new Button("Eliminar");

    private Incident incident;
    private String originalCode;
    private Set<String> existingCodesLower = Set.of();

    public IncidentDetailView(IncidentService incidentService, IncidentEntryService entryService,
            SprintService sprintService, CurrentProject currentProject) {
        this.incidentService = incidentService;
        this.entryService = entryService;
        this.sprintService = sprintService;
        this.currentProject = currentProject;
        setSizeFull();
        heading.addClassNames("aura-surface", "incident-heading");
        add(heading, buildMainData(), buildEvolution(), buildSummary(), newHint,
                buildEntries(), buildResolutionTests(), buildActions());
        bindFields();
    }

    private FormLayout buildMainData() {
        projectField.setReadOnly(true);
        code.setRequiredIndicatorVisible(true);
        code.setAllowCustomValue(true);
        code.setClearButtonVisible(true);
        code.setHelperText("Escribe el código; se muestran los ya usados que empiezan igual");
        code.addCustomValueSetListener(e -> code.setValue(e.getDetail()));
        title.setRequiredIndicatorVisible(true);

        status.setLabel("Estado");
        status.setItems(IncidentStatus.values());
        status.setItemLabelGenerator(IncidentStatus::getLabel);

        priority.setLabel("Prioridad");
        priority.setItems(IncidentPriority.values());
        priority.setItemLabelGenerator(IncidentPriority::getLabel);

        type.setItems(IncidentType.values());
        type.setItemLabelGenerator(IncidentType::getLabel);
        type.setClearButtonVisible(true);

        assignee.setAllowCustomValue(true);
        assignee.setClearButtonVisible(true);
        assignee.setHelperText("Escribe un nombre o elige uno usado antes");
        assignee.addCustomValueSetListener(e -> assignee.setValue(e.getDetail()));

        category.setAllowCustomValue(true);
        category.setClearButtonVisible(true);
        category.setHelperText("Escribe una categoría o elige una usada antes");
        category.addCustomValueSetListener(e -> category.setValue(e.getDetail()));

        sprint.setItemLabelGenerator(Sprint::getName);
        sprint.setClearButtonVisible(true);
        sprint.setHelperText("Opcional");
        sprint.addValueChangeListener(e -> updateCommitmentState());

        commitment.setItems(SprintCommitment.values());
        commitment.setItemLabelGenerator(SprintCommitment::getLabel);
        commitment.setClearButtonVisible(true);
        commitment.setHelperText("Planificada o surgida durante el sprint");

        FormLayout form = new FormLayout();
        H3 sectionTitle = new H3("Datos principales");
        form.add(sectionTitle);
        form.setColspan(sectionTitle, 2);
        form.add(projectField, code, title, category, type, assignee, status, priority,
                externalReference, sprint, commitment);
        return form;
    }

    private VerticalLayout buildEvolution() {
        blockedReason.setMaxLength(1000);
        blockedReason.setWidthFull();
        blockedReason.setVisible(false);
        blocked.addValueChangeListener(e -> blockedReason.setVisible(Boolean.TRUE.equals(e.getValue())));

        HorizontalLayout desarrollo = new HorizontalLayout(
                started, finished, testedLocal, testedPre, blocked);
        desarrollo.getStyle().set("flex-wrap", "wrap");
        HorizontalLayout ubicacion = new HorizontalLayout(
                frontendAffected, backendAffected, configurationAffected, databaseAffected);
        ubicacion.getStyle().set("flex-wrap", "wrap");
        HorizontalLayout pruebas = new HorizontalLayout(testCases, testEvidence);
        pruebas.getStyle().set("flex-wrap", "wrap");

        VerticalLayout evolution = new VerticalLayout(
                new H3("Evolución"),
                new H4("Desarrollo"), desarrollo, blockedReason,
                new H4("Ubicación de los ajustes"), ubicacion,
                new H4("Pruebas"), pruebas);
        evolution.setPadding(false);
        return evolution;
    }

    private VerticalLayout buildSummary() {
        summarySection.setPadding(false);
        summarySection.setSpacing(false);
        summarySection.addClassName("incident-summary");
        summarySection.add(new H3("Resumen"), hoursTotal, createdAtSpan, updatedAtSpan,
                closedAtSpan, lastPre, lastPro);
        return summarySection;
    }

    private VerticalLayout buildEntries() {
        entriesGrid.addColumn(e -> Formats.date(e.getEntryDate())).setHeader("Fecha").setAutoWidth(true);
        entriesGrid.addColumn(e -> e.getEntryType() == null ? "" : e.getEntryType().getLabel())
                .setHeader("Tipo").setAutoWidth(true);
        entriesGrid.addColumn(e -> e.getEnvironment() == null ? "" : e.getEnvironment().getLabel())
                .setHeader("Entorno").setAutoWidth(true);
        entriesGrid.addColumn(IncidentDetailView::versionsLabel).setHeader("Versión").setAutoWidth(true);
        entriesGrid.addColumn(e -> Formats.hours(e.getHours())).setHeader("Horas").setAutoWidth(true);
        entriesGrid.addColumn(e -> excerpt(e.getDescription(), 80)).setHeader("Descripción").setFlexGrow(1);
        entriesGrid.addComponentColumn(this::entryActions).setHeader("Acciones").setAutoWidth(true);
        // Acordeón: al hacer clic en la fila se despliega la descripción completa.
        entriesGrid.setItemDetailsRenderer(new ComponentRenderer<>(entry -> {
            Div detail = new Div();
            detail.setText(entry.getDescription());
            detail.getStyle().set("white-space", "pre-wrap")
                    .set("padding", "var(--vaadin-padding-s, 0.5rem)");
            return detail;
        }));
        entriesGrid.setDetailsVisibleOnClick(true);
        entriesGrid.setAllRowsVisible(true);

        Button addEntry = new Button("Añadir entrada", e -> addEntry());
        addEntry.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        entriesSection.setPadding(false);
        entriesSection.setWidthFull();
        entriesSection.add(new H3("Seguimiento cronológico"), addEntry, entriesGrid);
        return entriesSection;
    }

    private HorizontalLayout entryActions(IncidentEntry entry) {
        Button edit = new Button("Editar", e -> openEntryDialog(entry, "Editar entrada"));
        Button delete = new Button("Eliminar", e -> confirmDeleteEntry(entry));
        delete.addThemeVariants(ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_TERTIARY);
        return new HorizontalLayout(edit, delete);
    }

    private VerticalLayout buildResolutionTests() {
        resolution.setMaxLength(4000);
        resolution.setHeight("8rem");
        resolution.setWidthFull();
        resolution.setHelperText("Resumen final de cómo se resolvió (opcional, al cerrar).");

        tests.setMaxLength(4000);
        tests.setHeight("8rem");
        tests.setWidthFull();
        tests.setHelperText("Resumen de datos, usuarios, escenarios o versiones usados en las pruebas.");

        VerticalLayout section = new VerticalLayout(new H3("Resolución y pruebas"), resolution, tests);
        section.setPadding(false);
        section.setWidthFull();
        return section;
    }

    private HorizontalLayout buildActions() {
        save.setText("Guardar");
        save.addClickListener(e -> save());
        save.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        closeButton.addClickListener(e -> confirmClose());
        reopenButton.addClickListener(e -> confirmReopen());

        deleteButton.addThemeVariants(ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_TERTIARY);
        deleteButton.addClickListener(e -> confirmDeleteIncident());

        Button back = new Button("Volver", e -> goToList());
        return new HorizontalLayout(save, closeButton, reopenButton, deleteButton, back);
    }

    private void bindFields() {
        binder.forField(code)
                .asRequired("El código es obligatorio")
                .withValidator(this::codeNotDuplicated,
                        "Ya existe una incidencia con ese código en este proyecto")
                .bind("code");
        binder.forField(title).asRequired("El título es obligatorio").bind("title");
        binder.forField(category).bind("category");
        binder.forField(type).bind("type");
        binder.forField(assignee).bind("assignee");
        binder.forField(status).asRequired("El estado es obligatorio").bind("status");
        binder.forField(priority).asRequired("La prioridad es obligatoria").bind("priority");
        binder.forField(externalReference).bind("externalReference");
        binder.forField(started).bind("started");
        binder.forField(finished).bind("finished");
        binder.forField(testedLocal).bind("testedLocal");
        binder.forField(testedPre).bind("testedPre");
        binder.forField(frontendAffected).bind("frontendAffected");
        binder.forField(backendAffected).bind("backendAffected");
        binder.forField(configurationAffected).bind("configurationAffected");
        binder.forField(databaseAffected).bind("databaseAffected");
        binder.forField(testCases).bind("testCasesDone");
        binder.forField(testEvidence).bind("testEvidenceDone");
        binder.forField(blocked).bind("blocked");
        binder.forField(blockedReason).bind("blockedReason");
        binder.forField(resolution).bind("resolution");
        binder.forField(tests).bind("tests");
        binder.forField(sprint).bind("sprint");
        binder.forField(commitment).bind("sprintCommitment");

        // Deshabilita "Guardar" mientras haya errores de validación.
        binder.addStatusChangeListener(e -> save.setEnabled(!e.hasValidationErrors()));
    }

    private boolean codeNotDuplicated(String value) {
        if (value == null || value.isBlank()) {
            return true; // la obligatoriedad la controla asRequired
        }
        String v = value.trim().toLowerCase();
        if (originalCode != null && v.equals(originalCode.toLowerCase())) {
            return true; // su propio código actual
        }
        return !existingCodesLower.contains(v);
    }

    private void updateCommitmentState() {
        boolean hasSprint = sprint.getValue() != null;
        commitment.setEnabled(hasSprint);
        if (!hasSprint) {
            commitment.clear();
        }
    }

    @Override
    public void setParameter(BeforeEvent event, @OptionalParameter Long id) {
        if (id == null) {
            Project project = currentProject.getProject();
            if (project == null) {
                Notifications.info("Selecciona o crea un proyecto antes de crear una incidencia.");
                goToList();
                return;
            }
            incident = new Incident();
            incident.setProject(project);
            incident.setStatus(IncidentStatus.PENDING);
            incident.setPriority(IncidentPriority.MEDIUM);
            heading.setText("Nueva incidencia");
        } else {
            incident = incidentService.findById(id).orElse(null);
            if (incident == null) {
                Notifications.info("La incidencia no existe.");
                goToList();
                return;
            }
            heading.setText(Formats.identifier(incident));
        }
        projectField.setValue(incident.getProject().getName());
        sprint.setItems(sprintService.findByProject(incident.getProject()));
        assignee.setItems(incidentService.findDistinctAssignees(incident.getProject()));
        category.setItems(incidentService.findDistinctCategories(incident.getProject()));
        List<String> codes = incidentService.findCodes(incident.getProject());
        code.setItems(CODE_FILTER, codes);
        originalCode = incident.getCode();
        existingCodesLower = codes.stream().map(c -> c.toLowerCase()).collect(Collectors.toSet());
        binder.readBean(incident);
        blockedReason.setVisible(incident.isBlocked());
        updateCommitmentState();

        boolean persisted = incident.getId() != null;
        summarySection.setVisible(persisted);
        newHint.setVisible(!persisted);
        updateCloseReopenButtons();
        if (persisted) {
            refreshSummary();
            refreshEntries();
        } else {
            entriesGrid.setItems(List.of());
        }
    }

    private void updateCloseReopenButtons() {
        boolean persisted = incident.getId() != null;
        boolean closed = incident.getStatus() == IncidentStatus.CLOSED;
        closeButton.setVisible(persisted && !closed);
        reopenButton.setVisible(persisted && closed);
        deleteButton.setVisible(persisted);
    }

    private void refreshEntries() {
        entriesGrid.setItems(entryService.findByIncident(incident));
    }

    private void refreshSummary() {
        hoursTotal.setText("Horas totales: " + Formats.hours(entryService.getTotalHours(incident)));
        createdAtSpan.setText("Creada: " + Formats.dateTime(incident.getCreatedAt()));
        updatedAtSpan.setText("Última actualización: " + Formats.dateTime(incident.getUpdatedAt()));
        closedAtSpan.setVisible(incident.getClosedAt() != null);
        closedAtSpan.setText("Cerrada: " + Formats.dateTime(incident.getClosedAt()));
        lastPre.setText("Último despliegue PRE: " + deploymentText(IncidentEntryType.PRE_DEPLOYMENT));
        lastPro.setText("Último despliegue PRO: " + deploymentText(IncidentEntryType.PRO_DEPLOYMENT));
    }

    private String deploymentText(IncidentEntryType type) {
        Optional<IncidentEntry> last = entryService.getLastDeployment(incident, type);
        return last.map(e -> {
            String date = Formats.date(e.getEntryDate());
            String versions = versionsLabel(e);
            return versions.isEmpty() ? date : date + " (" + versions + ")";
        }).orElse("—");
    }

    /** "F: x · B: y" con las versiones informadas; cadena vacía si no hay ninguna. */
    private static String versionsLabel(IncidentEntry e) {
        String front = e.getFrontendVersion();
        String back = e.getBackendVersion();
        boolean hasFront = front != null && !front.isBlank();
        boolean hasBack = back != null && !back.isBlank();
        if (hasFront && hasBack) {
            return "F: " + front + " · B: " + back;
        }
        if (hasFront) {
            return "F: " + front;
        }
        if (hasBack) {
            return "B: " + back;
        }
        return "";
    }

    private static String excerpt(String text, int max) {
        if (text == null) {
            return "";
        }
        String oneLine = text.replaceAll("\\s+", " ").trim();
        return oneLine.length() <= max ? oneLine : oneLine.substring(0, max) + "…";
    }

    private void addEntry() {
        // Para poder asociar la entrada, la incidencia debe existir; si es nueva, se guarda antes.
        if (incident.getId() == null && !persistIncidentInline()) {
            return;
        }
        openEntryDialog(newEntry(), "Nueva entrada");
    }

    private boolean persistIncidentInline() {
        if (!binder.writeBeanIfValid(incident)) {
            return false;
        }
        try {
            incident = incidentService.save(incident);
            heading.setText(Formats.identifier(incident));
            projectField.setValue(incident.getProject().getName());
            sprint.setItems(sprintService.findByProject(incident.getProject()));
            binder.readBean(incident);
            updateCommitmentState();
            summarySection.setVisible(true);
            newHint.setVisible(false);
            updateCloseReopenButtons();
            refreshSummary();
            refreshEntries();
            Notifications.success("Incidencia guardada");
            return true;
        } catch (IllegalArgumentException ex) {
            Notifications.error(ex.getMessage());
            return false;
        }
    }

    private IncidentEntry newEntry() {
        IncidentEntry entry = new IncidentEntry();
        entry.setIncident(incident);
        entry.setEntryDate(LocalDate.now());
        entry.setEntryType(IncidentEntryType.DEVELOPMENT);
        return entry;
    }

    private void openEntryDialog(IncidentEntry entry, String title) {
        IncidentEntryDialog dialog = new IncidentEntryDialog();
        dialog.setSaveHandler(edited -> {
            try {
                entryService.save(edited);
                Notifications.success("Entrada guardada");
                refreshEntries();
                refreshSummary();
                return true;
            } catch (IllegalArgumentException ex) {
                Notifications.error(ex.getMessage());
                return false;
            }
        });
        dialog.open(entry, title);
    }

    private void confirmDeleteEntry(IncidentEntry entry) {
        com.vaadin.flow.component.dialog.Dialog confirm = new com.vaadin.flow.component.dialog.Dialog();
        confirm.setHeaderTitle("Eliminar entrada");
        confirm.add(new Paragraph("¿Seguro que quieres eliminar esta entrada? No se puede deshacer."));
        Button yes = new Button("Eliminar", e -> {
            entryService.delete(entry.getId());
            Notifications.info("Entrada eliminada");
            refreshEntries();
            refreshSummary();
            confirm.close();
        });
        yes.addThemeVariants(ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_PRIMARY);
        Button no = new Button("Cancelar", e -> confirm.close());
        confirm.getFooter().add(no, yes);
        confirm.open();
    }

    private void save() {
        persistAndReload("Incidencia guardada", incidentService::save);
    }

    private void confirmDeleteIncident() {
        Dialog confirm = new Dialog();
        confirm.setHeaderTitle("Eliminar incidencia");
        confirm.add(new Paragraph(
                "¿Seguro que quieres eliminar esta incidencia y todas sus entradas? No se puede deshacer."));
        Button yes = new Button("Eliminar", e -> {
            confirm.close();
            Long id = incident.getId();
            incident = null; // evita el aviso de "cambios sin guardar" al navegar
            incidentService.delete(id);
            Notifications.info("Incidencia eliminada");
            goToList();
        });
        yes.addThemeVariants(ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_PRIMARY);
        Button no = new Button("Cancelar", e -> confirm.close());
        confirm.getFooter().add(no, yes);
        confirm.open();
    }

    private void confirmClose() {
        Dialog confirm = new Dialog();
        confirm.setHeaderTitle("Cerrar incidencia");
        confirm.add(new Paragraph("¿Seguro que quieres cerrar esta incidencia?"));
        Button yes = new Button("Cerrar", e -> {
            confirm.close();
            persistAndReload("Incidencia cerrada", incidentService::close);
        });
        yes.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        Button no = new Button("Cancelar", e -> confirm.close());
        confirm.getFooter().add(no, yes);
        confirm.open();
    }

    private void confirmReopen() {
        Dialog confirm = new Dialog();
        confirm.setHeaderTitle("Reabrir incidencia");
        confirm.add(new Paragraph("¿Seguro que quieres reabrir esta incidencia? Volverá a 'En curso'."));
        Button yes = new Button("Reabrir", e -> {
            confirm.close();
            persistAndReload("Incidencia reabierta", incidentService::reopen);
        });
        yes.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        Button no = new Button("Cancelar", e -> confirm.close());
        confirm.getFooter().add(no, yes);
        confirm.open();
    }

    /** Writes the form into the incident, applies the given operation and reloads its detail. */
    private void persistAndReload(String successMessage,
            java.util.function.UnaryOperator<Incident> operation) {
        if (!binder.writeBeanIfValid(incident)) {
            return;
        }
        try {
            Incident saved = operation.apply(incident);
            Notifications.success(successMessage);
            getUI().ifPresent(ui -> ui.navigate(IncidentDetailView.class, saved.getId()));
        } catch (IllegalArgumentException ex) {
            Notifications.error(ex.getMessage());
        }
    }

    @Override
    public void beforeLeave(BeforeLeaveEvent event) {
        if (incident == null || !binder.hasChanges()) {
            return;
        }
        BeforeLeaveEvent.ContinueNavigationAction action = event.postpone();
        Dialog confirm = new Dialog();
        confirm.setHeaderTitle("Cambios sin guardar");
        confirm.add(new Paragraph("Tienes cambios sin guardar. ¿Salir y descartarlos?"));
        Button leave = new Button("Salir sin guardar", e -> {
            confirm.close();
            action.proceed();
        });
        leave.addThemeVariants(ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_PRIMARY);
        Button stay = new Button("Seguir editando", e -> confirm.close());
        confirm.getFooter().add(stay, leave);
        confirm.open();
    }

    private void goToList() {
        getUI().ifPresent(ui -> ui.navigate(IncidentListView.class));
    }
}
