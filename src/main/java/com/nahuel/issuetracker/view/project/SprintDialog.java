package com.nahuel.issuetracker.view.project;

import com.nahuel.issuetracker.entity.Sprint;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.BeanValidationBinder;

/** Dialog to create or edit a sprint (name and date range). */
public class SprintDialog extends Dialog {

    @FunctionalInterface
    public interface SaveHandler {
        boolean onSave(Sprint sprint);
    }

    private final BeanValidationBinder<Sprint> binder = new BeanValidationBinder<>(Sprint.class);
    private final TextField name = new TextField("Nombre");
    private final DatePicker startDate = new DatePicker("Inicio");
    private final DatePicker endDate = new DatePicker("Fin");
    private final Checkbox active = new Checkbox("Activo");

    private SaveHandler saveHandler = sprint -> true;
    private Sprint current;

    public SprintDialog() {
        setDraggable(true);
        setWidth("30rem");

        name.setRequiredIndicatorVisible(true);
        startDate.setI18n(new DatePicker.DatePickerI18n().setDateFormat("dd/MM/yyyy"));
        startDate.setRequiredIndicatorVisible(true);
        endDate.setI18n(new DatePicker.DatePickerI18n().setDateFormat("dd/MM/yyyy"));
        endDate.setRequiredIndicatorVisible(true);

        binder.forField(name).asRequired("El nombre es obligatorio").bind("name");
        binder.forField(startDate).asRequired("La fecha de inicio es obligatoria").bind("startDate");
        binder.forField(endDate).asRequired("La fecha de fin es obligatoria").bind("endDate");
        binder.forField(active).bind("active");

        add(new FormLayout(name, startDate, endDate, active));

        Button save = new Button("Guardar", e -> save());
        save.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        Button cancel = new Button("Cancelar", e -> close());
        getFooter().add(cancel, save);
    }

    public void setSaveHandler(SaveHandler saveHandler) {
        this.saveHandler = saveHandler;
    }

    public void open(Sprint sprint, String title) {
        setHeaderTitle(title);
        this.current = sprint;
        binder.readBean(sprint);
        open();
    }

    private void save() {
        if (binder.writeBeanIfValid(current) && saveHandler.onSave(current)) {
            close();
        }
    }
}