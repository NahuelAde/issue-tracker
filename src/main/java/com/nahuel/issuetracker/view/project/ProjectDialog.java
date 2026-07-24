package com.nahuel.issuetracker.view.project;

import com.nahuel.issuetracker.entity.Project;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.BeanValidationBinder;

/**
 * Dialog to create or edit a project. The caller supplies a {@link SaveHandler}
 * that persists the project and returns whether it succeeded; the dialog closes
 * only on success, so validation errors keep it open.
 */
public class ProjectDialog extends Dialog {

    /** Persists the project; returns true on success, false to keep the dialog open. */
    @FunctionalInterface
    public interface SaveHandler {
        boolean onSave(Project project);
    }

    private final BeanValidationBinder<Project> binder = new BeanValidationBinder<>(Project.class);
    private final TextField name = new TextField("Nombre");
    private final TextField code = new TextField("Código");
    private final TextArea description = new TextArea("Descripción");
    private final Checkbox active = new Checkbox("Activo");

    private SaveHandler saveHandler = project -> true;
    private Project current;

    public ProjectDialog() {
        setDraggable(true);
        setWidth("32rem");

        name.setRequiredIndicatorVisible(true);
        code.setRequiredIndicatorVisible(true);
        description.setMaxLength(1000);

        binder.forField(name).asRequired("El nombre es obligatorio").bind("name");
        binder.forField(code).asRequired("El código es obligatorio").bind("code");
        binder.forField(description).bind("description");
        binder.forField(active).bind("active");

        FormLayout form = new FormLayout(name, code, description, active);
        form.setColspan(description, 2);
        add(form);

        Button save = new Button("Guardar", e -> save());
        save.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        Button cancel = new Button("Cancelar", e -> close());
        getFooter().add(cancel, save);
    }

    public void setSaveHandler(SaveHandler saveHandler) {
        this.saveHandler = saveHandler;
    }

    /** Opens the dialog to edit the given project (use a new instance for creation). */
    public void open(Project project, String title) {
        setHeaderTitle(title);
        this.current = project;
        binder.readBean(project);
        open();
    }

    private void save() {
        if (binder.writeBeanIfValid(current) && saveHandler.onSave(current)) {
            close();
        }
    }
}
