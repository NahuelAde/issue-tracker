package com.nahuel.issuetracker.view.incident;

import com.nahuel.issuetracker.configuration.CurrentProject;
import com.nahuel.issuetracker.configuration.IncidentFilterState;
import com.nahuel.issuetracker.entity.Incident;
import com.nahuel.issuetracker.entity.Project;
import com.nahuel.issuetracker.entity.Sprint;
import com.nahuel.issuetracker.enums.IncidentPriority;
import com.nahuel.issuetracker.enums.IncidentStatus;
import com.nahuel.issuetracker.enums.IncidentType;
import com.nahuel.issuetracker.service.IncidentEntryService;
import com.nahuel.issuetracker.service.IncidentService;
import com.nahuel.issuetracker.service.SprintService;
import com.nahuel.issuetracker.utils.Formats;
import com.nahuel.issuetracker.utils.Notifications;
import com.nahuel.issuetracker.view.ProjectAware;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.HeaderRow;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.shared.Tooltip;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Main view: lists the incidents of the currently selected project, with search
 * and filters. Clicking a code (or double-clicking a row) opens the detail view.
 */
@Route("")
@PageTitle("Incidencias")
public class IncidentListView extends VerticalLayout implements ProjectAware {

    /** Opción del selector de estado del listado: un estado concreto, o el agregado
     * "Abiertas" (todo lo que no esté Cerrada). Sustituye al antiguo checkbox "Sólo abiertas":
     * conceptualmente es lo contrario de "Cerrada", así que encaja como una opción más aquí. */
    private record StatusFilterOption(IncidentStatus status, boolean openAggregate) {
        static final StatusFilterOption OPEN = new StatusFilterOption(null, true);

        static StatusFilterOption of(IncidentStatus status) {
            return new StatusFilterOption(status, false);
        }

        String label() {
            return openAggregate ? "Abiertas" : status.getLabel();
        }
    }

    /** Fases del bloque "Desarrollo" del detalle, para poder filtrar el listado por una de
     * ellas. Solo tiene sentido como filtro de esta vista; no es un enum de dominio. */
    private enum DevelopmentPhase {
        STARTED("Empezada", Incident::isStarted),
        FINISHED("Terminada", Incident::isFinished),
        TESTED_LOCAL("Probada local", Incident::isTestedLocal),
        TESTED_PRE("Probada PRE", Incident::isTestedPre);

        private final String label;
        private final java.util.function.Predicate<Incident> matches;

        DevelopmentPhase(String label, java.util.function.Predicate<Incident> matches) {
            this.label = label;
            this.matches = matches;
        }
    }

    private final IncidentService incidentService;
    private final IncidentEntryService entryService;
    private final SprintService sprintService;
    private final CurrentProject currentProject;
    private final IncidentFilterState filterState;

    private Map<Long, BigDecimal> hoursByIncident = Map.of();
    private Map<Long, String> tooltipByIncident = Map.of();
    private Long currentSprintId;
    private List<Sprint> sprints = List.of();
    private boolean restoring;

    private final TextField search = new TextField();
    private final ComboBox<StatusFilterOption> statusFilter = new ComboBox<>();
    private final ComboBox<IncidentPriority> priorityFilter = new ComboBox<>();
    private final ComboBox<String> categoryFilter = new ComboBox<>();
    private final ComboBox<Sprint> sprintFilter = new ComboBox<>();
    private final ComboBox<Sprint> excludeSprintFilter = new ComboBox<>();
    private final ComboBox<IncidentType> typeFilter = new ComboBox<>();
    private final ComboBox<String> assigneeFilter = new ComboBox<>();
    private final ComboBox<DevelopmentPhase> developmentPhaseFilter = new ComboBox<>();
    private final Button extractHours = new Button("Extraer horas");
    private final Button newIncident = new Button("Nueva incidencia");
    private final Grid<Incident> grid = new Grid<>(Incident.class, false);
    private final Span countLabel = new Span();

    public IncidentListView(IncidentService incidentService, IncidentEntryService entryService,
            SprintService sprintService, CurrentProject currentProject,
            IncidentFilterState filterState) {
        this.incidentService = incidentService;
        this.entryService = entryService;
        this.sprintService = sprintService;
        this.currentProject = currentProject;
        this.filterState = filterState;
        setSizeFull();

        add(buildToolbar());
        configureGrid();
        add(grid);
        add(buildFooter());

        updateCategoryFilter();
        updateSprintFilter();
        updateAssigneeFilter();
        restoreState();
        refresh();
    }

    private Component buildToolbar() {
        search.setPlaceholder("Buscar código, título, categoría o referencia");
        search.setClearButtonVisible(true);
        search.setPrefixComponent(VaadinIcon.SEARCH.create());
        search.setValueChangeMode(ValueChangeMode.LAZY);
        search.setWidth("22rem");
        search.addValueChangeListener(e -> onFilterChanged());

        statusFilter.setPlaceholder("Estado");
        statusFilter.setItems(statusFilterOptions());
        statusFilter.setItemLabelGenerator(StatusFilterOption::label);
        statusFilter.setClearButtonVisible(true);
        statusFilter.setValue(StatusFilterOption.OPEN);
        statusFilter.setTooltipText(
                "\"Abiertas\" agrupa todo lo que no esté Cerrada; el aspa lo deja sin filtrar "
                        + "(incluye cerradas)");
        statusFilter.addValueChangeListener(e -> onFilterChanged());

        priorityFilter.setPlaceholder("Prioridad");
        priorityFilter.setItems(IncidentPriority.values());
        priorityFilter.setItemLabelGenerator(IncidentPriority::getLabel);
        priorityFilter.setClearButtonVisible(true);
        priorityFilter.addValueChangeListener(e -> onFilterChanged());

        developmentPhaseFilter.setPlaceholder("Fase de desarrollo");
        developmentPhaseFilter.setItems(DevelopmentPhase.values());
        developmentPhaseFilter.setItemLabelGenerator(phase -> phase.label);
        developmentPhaseFilter.setClearButtonVisible(true);
        developmentPhaseFilter.addValueChangeListener(e -> onFilterChanged());

        categoryFilter.setPlaceholder("Categoría");
        categoryFilter.setClearButtonVisible(true);
        categoryFilter.addValueChangeListener(e -> onFilterChanged());

        sprintFilter.setPlaceholder("Sprint");
        sprintFilter.setItemLabelGenerator(Sprint::getName);
        sprintFilter.setClearButtonVisible(true);
        sprintFilter.addValueChangeListener(e -> onFilterChanged());

        typeFilter.setPlaceholder("Tipo");
        typeFilter.setItems(IncidentType.values());
        typeFilter.setItemLabelGenerator(IncidentType::getLabel);
        typeFilter.setClearButtonVisible(true);
        typeFilter.addValueChangeListener(e -> onFilterChanged());

        assigneeFilter.setPlaceholder("Asignada a");
        assigneeFilter.setClearButtonVisible(true);
        assigneeFilter.addValueChangeListener(e -> onFilterChanged());

        excludeSprintFilter.setPlaceholder("Excluir sprint");
        excludeSprintFilter.setItemLabelGenerator(Sprint::getName);
        excludeSprintFilter.setClearButtonVisible(true);
        excludeSprintFilter.setTooltipText(
                "Oculta las incidencias del sprint elegido; útil para planificar el siguiente "
                        + "sprint sin ver las ya cerradas de uno anterior");
        excludeSprintFilter.addValueChangeListener(e -> onFilterChanged());

        Button clear = new Button("Limpiar filtros", e -> clearFilters());

        extractHours.setTooltipText(
                "Copia el identificador completo y las horas de las incidencias marcadas en la tabla");
        extractHours.setEnabled(false);
        extractHours.addClickListener(e -> openExtractHoursDialog());

        newIncident.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        newIncident.addClickListener(e -> UI.getCurrent().navigate(IncidentDetailView.class));

        HorizontalLayout toolbar = new HorizontalLayout(
                search, statusFilter, priorityFilter, developmentPhaseFilter, categoryFilter,
                sprintFilter, excludeSprintFilter, typeFilter, assigneeFilter, clear,
                extractHours, newIncident);
        toolbar.setAlignItems(FlexComponent.Alignment.END);
        toolbar.getStyle().set("flex-wrap", "wrap");
        toolbar.setWidthFull();
        return toolbar;
    }

    private void configureGrid() {
        grid.addComponentColumn(this::codeLink).setHeader("Código").setAutoWidth(true)
                .setFlexGrow(0);
        grid.addColumn(Incident::getTitle).setHeader("Título").setFlexGrow(4);
        grid.addColumn(Incident::getCategory).setHeader("Categoría").setAutoWidth(true).setFlexGrow(0);
        grid.addColumn(i -> i.getType() == null ? "" : i.getType().getLabel())
                .setHeader("Tipo").setAutoWidth(true).setFlexGrow(0);
        grid.addColumn(Incident::getAssignee).setHeader("Asignada").setAutoWidth(true).setFlexGrow(0);
        grid.addColumn(i -> i.getStatus().getLabel()).setHeader("Estado").setWidth("8rem").setFlexGrow(0);
        grid.addComponentColumn(this::priorityBadge).setHeader("Prioridad").setWidth("7rem")
                .setFlexGrow(0).setTextAlign(com.vaadin.flow.component.grid.ColumnTextAlign.CENTER);
        grid.addColumn(this::sprintName).setHeader("Sprint").setAutoWidth(true).setFlexGrow(0);

        Grid.Column<Incident> cE = addBoolColumn("E", Incident::isStarted);
        Grid.Column<Incident> cT = addBoolColumn("T", Incident::isFinished);
        Grid.Column<Incident> cL = addBoolColumn("L", Incident::isTestedLocal);
        Grid.Column<Incident> cPre = addBoolColumn("PRE", Incident::isTestedPre);
        Grid.Column<Incident> cF = addBoolColumn("F", Incident::isFrontendAffected);
        Grid.Column<Incident> cB = addBoolColumn("B", Incident::isBackendAffected);
        Grid.Column<Incident> cC = addBoolColumn("C", Incident::isConfigurationAffected);
        Grid.Column<Incident> cS = addBoolColumn("S", Incident::isDatabaseAffected);
        Grid.Column<Incident> cCp = addBoolColumn("CP", Incident::isTestCasesDone);
        Grid.Column<Incident> cEv = addBoolColumn("EV", Incident::isTestEvidenceDone);

        Grid.Column<Incident> cHours = grid.addColumn(
                        i -> Formats.hours(hoursByIncident.getOrDefault(i.getId(), BigDecimal.ZERO)))
                .setHeader("Horas").setAutoWidth(true).setFlexGrow(0);
        grid.addColumn(i -> Formats.dateShort(i.getUpdatedAt()))
                .setHeader("Actualizada").setAutoWidth(true).setFlexGrow(0);
        grid.addComponentColumn(this::infoIcon).setHeader("").setAutoWidth(true).setFlexGrow(0)
                .setTextAlign(com.vaadin.flow.component.grid.ColumnTextAlign.CENTER);

        // Bloques de columnas (cabecera superior que agrupa).
        HeaderRow groups = grid.prependHeaderRow();
        groups.join(cE, cT, cL, cPre).setComponent(groupHeader("Desarrollo"));
        groups.join(cF, cB, cC, cS).setComponent(groupHeader("Ubicación"));
        groups.join(cCp, cEv).setComponent(groupHeader("Pruebas"));

        // Líneas verticales que separan los bloques (borde izq. en la 1ª columna de cada bloque
        // y en la siguiente al último bloque para cerrarlo).
        cE.setPartNameGenerator(i -> "block-sep");
        cF.setPartNameGenerator(i -> "block-sep");
        cCp.setPartNameGenerator(i -> "block-sep");
        cHours.setPartNameGenerator(i -> "block-sep");
        // Mismas líneas en las cabeceras de columna.
        cE.setHeaderPartName("block-sep");
        cF.setHeaderPartName("block-sep");
        cCp.setHeaderPartName("block-sep");
        cHours.setHeaderPartName("block-sep");

        grid.setPartNameGenerator(this::rowBucket);
        grid.addItemDoubleClickListener(e -> openDetail(e.getItem()));
        grid.setSizeFull();

        grid.setSelectionMode(Grid.SelectionMode.MULTI);
        grid.addSelectionListener(e -> extractHours.setEnabled(!e.getAllSelectedItems().isEmpty()));
    }

    private Span groupHeader(String text) {
        Span span = new Span(text);
        span.getStyle()
                .set("display", "block")
                .set("width", "100%")
                .set("text-align", "center")
                .set("font-weight", "600");
        return span;
    }

    private Button codeLink(Incident incident) {
        Button link = new Button(incident.getCode(), e -> openDetail(incident));
        link.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
        link.getStyle().set("font-size", "0.8rem").set("white-space", "nowrap");
        return link;
    }

    private Grid.Column<Incident> addBoolColumn(String header,
            java.util.function.Predicate<Incident> flag) {
        return grid.addComponentColumn(i -> boolIcon(flag.test(i)))
                .setHeader(header).setAutoWidth(true).setFlexGrow(0).setTextAlign(
                        com.vaadin.flow.component.grid.ColumnTextAlign.CENTER);
    }

    private Component infoIcon(Incident incident) {
        String text = tooltipByIncident.get(incident.getId());
        if (text == null || text.isBlank()) {
            return new Span();
        }
        Icon icon = VaadinIcon.INFO_CIRCLE_O.create();
        icon.setSize("1.1rem");
        icon.getStyle().set("color", "var(--vaadin-text-color-secondary, gray)").set("cursor", "help");

        Tooltip tooltip = Tooltip.forComponent(icon);
        tooltip.setText(text);
        tooltip.setPosition(Tooltip.TooltipPosition.START_TOP);
        tooltip.setHoverDelay(400);
        return icon;
    }

    private Component boolIcon(boolean value) {
        if (!value) {
            return new Span();
        }
        Icon icon = VaadinIcon.CHECK.create();
        icon.setSize("1rem");
        icon.getStyle().set("color", "var(--vaadin-color-success, green)");
        return icon;
    }

    /**
     * "Pastilla" de prioridad en la columna Prioridad, con una escala de gravedad
     * coherente (gris → azul → naranja → rojo). Va aparte del color de estado de la
     * fila, así que se distingue sin pelearse con él.
     */
    private Component priorityBadge(Incident incident) {
        IncidentPriority priority = incident.getPriority();
        String background;
        String foreground;
        switch (priority) {
            case URGENT -> { background = "#c40000"; foreground = "#ffffff"; }
            case HIGH -> { background = "#ffc000"; foreground = "#3a2e00"; }
            case MEDIUM -> { background = "#cbdff2"; foreground = "#1c4e80"; }
            default -> { background = "#e2e2e2"; foreground = "#555555"; } // LOW
        }
        Span badge = new Span(priority.getLabel());
        badge.getStyle()
                .set("background-color", background)
                .set("color", foreground)
                .set("font-weight", "700")
                .set("font-size", "0.75rem")
                .set("padding", "0.1rem 0.5rem")
                .set("border-radius", "0.6rem")
                .set("white-space", "nowrap");
        return badge;
    }

    private String rowBucket(Incident incident) {
        if (incident.isBlocked()) {
            return "incident-blocked";
        }
        boolean closed = incident.getStatus() == IncidentStatus.CLOSED;
        boolean finished = closed || incident.isFinished();
        Long sprintId = incident.getSprint() == null ? null : incident.getSprint().getId();
        boolean inActiveSprint = currentSprintId != null && currentSprintId.equals(sprintId);
        boolean inOtherSprint = sprintId != null && !inActiveSprint;

        // El color del sprint manda (una cerrada de sprint pasado va en celeste).
        if (inActiveSprint) {
            if (closed) {
                return "incident-active-closed";
            }
            return finished ? "incident-active-finished" : "incident-active-unfinished";
        }
        if (inOtherSprint) {
            return finished ? "incident-past-finished" : "incident-past-unfinished";
        }
        // Sin sprint
        if (closed) {
            return "incident-closed";
        }
        if (finished) {
            return "incident-nosprint-finished";
        }
        if (incident.isStarted()) {
            return "incident-nosprint-started";
        }
        return "incident-new";
    }

    private boolean isInActiveSprint(Incident incident) {
        return currentSprintId != null && incident.getSprint() != null
                && currentSprintId.equals(incident.getSprint().getId());
    }

    private Component buildFooter() {
        Span legend = new Span("Desarrollo — E: Empezada · T: Terminada · L: Probada local · "
                + "PRE: Probada PRE · | Ubicación — F: Frontend · B: Backend · C: Configuración · "
                + "S: Script/BBDD | Pruebas — CP: Casos de prueba · EV: Evidencias");
        legend.getStyle().set("font-size", "0.75rem").set("color", "var(--vaadin-text-color-secondary, gray)");
        VerticalLayout footer = new VerticalLayout(countLabel, legend, colorLegend());
        footer.setPadding(false);
        footer.setSpacing(false);
        return footer;
    }

    /** Resumen del significado de los colores de fila del listado, con el mismo aspecto
     * de "pastilla" que {@link #priorityBadge}. Los colores deben coincidir con las reglas
     * {@code vaadin-grid::part(...)} de styles.css (fuente real de la coloración); aquí solo
     * se reproducen para mostrarlos, siguiendo el orden de prioridad de {@link #rowBucket}. */
    private Component colorLegend() {
        HorizontalLayout legend = new HorizontalLayout(
                colorChip("Bloqueada", "#f6d0d0", "#777"),
                colorChip("Sprint actual · sin terminar", "#8dd873", "#000"),
                colorChip("Sprint actual · terminada", "#d9f2d0", "#000"),
                colorChip("Sprint actual · cerrada", "#eef7e8", "#888"),
                colorChip("Sprint no activo · sin terminar", "#b8dcec", "#555"),
                colorChip("Sprint no activo · terminada o cerrada", "#dceef7", "#777"),
                colorChip("Cerrada, sin sprint", "#e4e4e4", "#666"),
                colorChip("Sin sprint · terminada", "#ececec", "#c77b30"),
                colorChip("Sin sprint · empezada", "#ffffff", "#c77b30"),
                colorChip("Sin sprint · nueva", "#ffffff", "#9e9e9e"));
        legend.getStyle().set("flex-wrap", "wrap").set("margin-top", "0.3rem");
        legend.setSpacing(false);
        legend.getStyle().set("gap", "0.4rem");
        return legend;
    }

    private Span colorChip(String text, String background, String foreground) {
        Span chip = new Span(text);
        chip.getStyle()
                .set("background-color", background)
                .set("color", foreground)
                .set("font-size", "0.72rem")
                .set("font-weight", "600")
                .set("padding", "0.15rem 0.55rem")
                .set("border-radius", "0.6rem")
                .set("white-space", "nowrap")
                .set("border", "1px solid rgba(0, 0, 0, 0.1)");
        return chip;
    }

    private void openDetail(Incident incident) {
        UI.getCurrent().navigate(IncidentDetailView.class, incident.getId());
    }

    /** Diálogo con el identificador completo y las horas de las incidencias marcadas en la
     * tabla, en texto plano para copiar y pegar (p. ej. al cerrar un sprint). */
    private void openExtractHoursDialog() {
        Set<Incident> selected = grid.getSelectedItems();
        if (selected.isEmpty()) {
            Notifications.info("Selecciona al menos una incidencia en la tabla.");
            return;
        }
        List<Incident> sorted = new ArrayList<>(selected);
        sorted.sort(Comparator.comparing(i -> i.getCode() == null ? "" : i.getCode(),
                String.CASE_INSENSITIVE_ORDER));

        StringBuilder text = new StringBuilder();
        BigDecimal total = BigDecimal.ZERO;
        for (Incident incident : sorted) {
            BigDecimal hours = hoursByIncident.getOrDefault(incident.getId(), BigDecimal.ZERO);
            total = total.add(hours);
            text.append(Formats.identifier(incident)).append(" — ").append(Formats.hours(hours))
                    .append(" h").append(System.lineSeparator());
        }
        text.append(System.lineSeparator()).append("Total: ").append(Formats.hours(total)).append(" h");

        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Extraer horas (" + sorted.size()
                + (sorted.size() == 1 ? " incidencia)" : " incidencias)"));
        dialog.setWidth("min(90rem, 80vw)");
        dialog.setDraggable(true);

        TextArea content = new TextArea();
        content.setValue(text.toString());
        content.setReadOnly(true);
        content.setWidthFull();
        content.setHeight("22rem");
        content.setHelperText("Selecciona todo el texto (Ctrl+A) y cópialo (Ctrl+C).");
        dialog.add(content);

        Button close = new Button("Cerrar", e -> dialog.close());
        dialog.getFooter().add(close);
        dialog.open();
    }

    private void onFilterChanged() {
        if (restoring) {
            return;
        }
        saveState();
        refresh();
    }

    private void clearFilters() {
        restoring = true;
        search.clear();
        statusFilter.setValue(StatusFilterOption.OPEN);
        priorityFilter.clear();
        categoryFilter.clear();
        sprintFilter.clear();
        typeFilter.clear();
        assigneeFilter.clear();
        excludeSprintFilter.clear();
        developmentPhaseFilter.clear();
        restoring = false;
        saveState();
        refresh();
    }

    private void saveState() {
        filterState.setSearchText(search.getValue());
        filterState.setStatusFilter(encodeStatusOption(statusFilter.getValue()));
        filterState.setPriority(priorityFilter.getValue());
        filterState.setCategory(categoryFilter.getValue());
        filterState.setSprintId(sprintFilter.getValue() == null ? null : sprintFilter.getValue().getId());
        filterState.setType(typeFilter.getValue());
        filterState.setAssignee(assigneeFilter.getValue());
        filterState.setExcludeSprintId(
                excludeSprintFilter.getValue() == null ? null : excludeSprintFilter.getValue().getId());
        filterState.setDevelopmentPhase(developmentPhaseFilter.getValue() == null
                ? null : developmentPhaseFilter.getValue().name());
    }

    private void restoreState() {
        restoring = true;
        search.setValue(filterState.getSearchText() == null ? "" : filterState.getSearchText());
        statusFilter.setValue(decodeStatusOption(filterState.getStatusFilter()));
        priorityFilter.setValue(filterState.getPriority());
        categoryFilter.setValue(filterState.getCategory());
        sprintFilter.setValue(findSprintById(filterState.getSprintId()));
        typeFilter.setValue(filterState.getType());
        assigneeFilter.setValue(filterState.getAssignee());
        excludeSprintFilter.setValue(findSprintById(filterState.getExcludeSprintId()));
        developmentPhaseFilter.setValue(parseDevelopmentPhase(filterState.getDevelopmentPhase()));
        restoring = false;
    }

    /** Convierte el nombre guardado en el estado de filtros a su fase; ignora valores
     * desconocidos (p. ej. si el enum cambiase en el futuro). */
    private DevelopmentPhase parseDevelopmentPhase(String name) {
        if (name == null) {
            return null;
        }
        try {
            return DevelopmentPhase.valueOf(name);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    /** "Abiertas" (agregado) seguido de cada estado concreto, en el orden del enum. */
    private static List<StatusFilterOption> statusFilterOptions() {
        List<StatusFilterOption> options = new ArrayList<>();
        options.add(StatusFilterOption.OPEN);
        for (IncidentStatus status : IncidentStatus.values()) {
            options.add(StatusFilterOption.of(status));
        }
        return options;
    }

    /** Codifica la opción elegida para guardarla en el estado de filtros: "OPEN" para el
     * agregado, o el nombre del estado concreto; null si no hay selección (sin filtrar). */
    private static String encodeStatusOption(StatusFilterOption option) {
        if (option == null) {
            return null;
        }
        return option.openAggregate() ? "OPEN" : option.status().name();
    }

    /** Inverso de {@link #encodeStatusOption}; ignora valores desconocidos. */
    private static StatusFilterOption decodeStatusOption(String value) {
        if (value == null) {
            return null;
        }
        if ("OPEN".equals(value)) {
            return StatusFilterOption.OPEN;
        }
        try {
            return StatusFilterOption.of(IncidentStatus.valueOf(value));
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private Sprint findSprintById(Long id) {
        if (id == null) {
            return null;
        }
        return sprints.stream().filter(s -> id.equals(s.getId())).findFirst().orElse(null);
    }

    /** Nombre del sprint de la incidencia, o cadena vacía si no tiene. Evita tocar la
     * asociación LAZY más allá de su id (fuera de transacción), resolviendo el nombre
     * contra la lista de sprints del proyecto ya cargada. */
    private String sprintName(Incident incident) {
        Sprint incidentSprint = incident.getSprint();
        if (incidentSprint == null) {
            return "";
        }
        Sprint found = findSprintById(incidentSprint.getId());
        return found == null ? "" : found.getName();
    }

    private void updateCategoryFilter() {
        Project project = currentProject.getProject();
        categoryFilter.setItems(project == null ? List.of()
                : incidentService.findDistinctCategories(project));
    }

    private void updateSprintFilter() {
        Project project = currentProject.getProject();
        sprints = project == null ? List.of() : sprintService.findByProject(project);
        sprintFilter.setItems(sprints);
        excludeSprintFilter.setItems(sprints);
    }

    private void updateAssigneeFilter() {
        Project project = currentProject.getProject();
        assigneeFilter.setItems(project == null ? List.of()
                : incidentService.findDistinctAssignees(project));
    }

    private void refresh() {
        Project project = currentProject.getProject();
        newIncident.setEnabled(project != null);
        if (project == null) {
            grid.setItems(List.of());
            countLabel.setText("Selecciona o crea un proyecto para ver sus incidencias.");
            return;
        }
        currentSprintId = sprintService.findCurrent(project).map(Sprint::getId).orElse(null);
        hoursByIncident = entryService.hoursByProject(project);
        tooltipByIncident = entryService.entriesTooltipByProject(project);
        StatusFilterOption selectedStatus = statusFilter.getValue();
        IncidentStatus statusParam = selectedStatus == null ? null : selectedStatus.status();
        boolean onlyOpenParam = selectedStatus != null && selectedStatus.openAggregate();
        List<Incident> items = new ArrayList<>(incidentService.search(project, search.getValue(),
                statusParam, priorityFilter.getValue(),
                categoryFilter.getValue(), sprintFilter.getValue(), typeFilter.getValue(),
                assigneeFilter.getValue(), onlyOpenParam));
        Sprint excludeSprint = excludeSprintFilter.getValue();
        if (excludeSprint != null) {
            Long excludeId = excludeSprint.getId();
            items.removeIf(i -> i.getSprint() != null && excludeId.equals(i.getSprint().getId()));
        }
        DevelopmentPhase phase = developmentPhaseFilter.getValue();
        if (phase != null) {
            items.removeIf(i -> !phase.matches.test(i));
        }
        // Primero las del sprint activo; dentro, orden alfabético por código.
        items.sort(Comparator
                .comparingInt((Incident i) -> isInActiveSprint(i) ? 0 : 1)
                .thenComparing(i -> i.getCode() == null ? "" : i.getCode(),
                        String.CASE_INSENSITIVE_ORDER));
        grid.setItems(items);
        countLabel.setText(items.size() + (items.size() == 1 ? " incidencia" : " incidencias"));
        extractHours.setEnabled(!grid.getSelectedItems().isEmpty());
    }

    @Override
    public void onProjectChanged() {
        restoring = true;
        updateCategoryFilter();
        updateSprintFilter();
        updateAssigneeFilter();
        categoryFilter.clear();
        sprintFilter.clear();
        assigneeFilter.clear();
        excludeSprintFilter.clear();
        restoring = false;
        saveState();
        refresh();
    }
}
