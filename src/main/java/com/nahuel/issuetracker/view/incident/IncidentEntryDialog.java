package com.nahuel.issuetracker.view.incident;

import com.nahuel.issuetracker.entity.IncidentEntry;
import com.nahuel.issuetracker.enums.Environment;
import com.nahuel.issuetracker.enums.IncidentEntryType;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.BigDecimalField;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.BeanValidationBinder;

/**
 * Dialog to create or edit a chronological entry. When the type is a PRE/PRO
 * deployment and no version is given, it warns but still allows saving.
 */
public class IncidentEntryDialog extends Dialog {

    @FunctionalInterface
    public interface SaveHandler {
        boolean onSave(IncidentEntry entry);
    }

    private final BeanValidationBinder<IncidentEntry> binder = new BeanValidationBinder<>(IncidentEntry.class);
    private final DatePicker entryDate = new DatePicker("Fecha");
    private final Select<IncidentEntryType> entryType = new Select<>();
    private final ComboBox<Environment> environment = new ComboBox<>("Entorno");
    private final TextField frontendVersion = new TextField("Versión front");
    private final TextField backendVersion = new TextField("Versión back");
    private final BigDecimalField hours = new BigDecimalField("Horas");
    private final TextArea description = new TextArea("Descripción");

    private SaveHandler saveHandler = entry -> true;
    private IncidentEntry current;

    public IncidentEntryDialog() {
        setDraggable(true);
        setWidth("42rem");

        entryDate.setI18n(new DatePicker.DatePickerI18n().setDateFormat("dd/MM/yyyy"));
        entryDate.setRequiredIndicatorVisible(true);

        entryType.setLabel("Tipo");
        entryType.setItems(IncidentEntryType.values());
        entryType.setItemLabelGenerator(IncidentEntryType::getLabel);

        environment.setItems(Environment.values());
        environment.setItemLabelGenerator(Environment::getLabel);
        environment.setClearButtonVisible(true);
        environment.setHelperText("Opcional");

        frontendVersion.setHelperText("Recomendada en despliegues PRE/PRO");
        backendVersion.setHelperText("Recomendada en despliegues PRE/PRO");

        hours.setHelperText("Opcional, admite fracciones (0,5 · 1,25)");

        description.setMaxLength(4000);
        description.setHeight("13rem");
        description.setRequiredIndicatorVisible(true);

        binder.forField(entryDate).asRequired("La fecha es obligatoria").bind("entryDate");
        binder.forField(entryType).asRequired("El tipo es obligatorio").bind("entryType");
        binder.forField(environment).bind("environment");
        binder.forField(frontendVersion).bind("frontendVersion");
        binder.forField(backendVersion).bind("backendVersion");
        binder.forField(hours)
                .withValidator(h -> h == null || h.signum() >= 0, "Las horas no pueden ser negativas")
                .bind("hours");
        binder.forField(description).asRequired("La descripción es obligatoria").bind("description");

        FormLayout form = new FormLayout(entryDate, entryType, environment, hours,
                frontendVersion, backendVersion, description);
        // Dos columnas fijas: Fecha/Tipo · Entorno/Horas · Versión front/Versión back.
        form.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 2));
        form.setColspan(description, 2);
        add(form);

        Button save = new Button("Añadir entrada", e -> save());
        save.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        Button cancel = new Button("Cancelar", e -> close());
        getFooter().add(cancel, save);
    }

    public void setSaveHandler(SaveHandler saveHandler) {
        this.saveHandler = saveHandler;
    }

    public void open(IncidentEntry entry, String title) {
        setHeaderTitle(title);
        this.current = entry;
        binder.readBean(entry);
        open();
    }

    private void save() {
        if (!binder.writeBeanIfValid(current)) {
            return;
        }
        IncidentEntryType type = current.getEntryType();
        boolean deployment = type == IncidentEntryType.PRE_DEPLOYMENT
                || type == IncidentEntryType.PRO_DEPLOYMENT;
        boolean noVersion = isBlank(current.getFrontendVersion()) && isBlank(current.getBackendVersion());
        if (deployment && noVersion) {
            Notification.show("Recomendado: informa la versión (front y/o back) en los despliegues.", 4000,
                    Notification.Position.MIDDLE);
        }
        if (saveHandler.onSave(current)) {
            close();
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
