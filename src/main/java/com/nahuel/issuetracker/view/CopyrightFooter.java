package com.nahuel.issuetracker.view;

import com.vaadin.flow.component.html.Span;

import java.time.Year;

/**
 * Pie de página discreto con el copyright, para usar al final del listado y del
 * detalle de incidencias. El año se calcula en cada arranque, sin tocarlo a mano.
 */
public class CopyrightFooter extends Span {

    public CopyrightFooter() {
        Span brand = new Span("DNA");
        brand.getStyle().set("font-weight", "700").set("letter-spacing", "0.08em");

        add(new Span("© " + Year.now().getValue() + " "), brand,
                new Span(" Developments-NahuelAde"));

        getStyle()
                .set("display", "block")
                .set("width", "100%")
                .set("text-align", "center")
                .set("font-size", "0.7rem")
                .set("color", "var(--vaadin-text-color-disabled, #aaaaaa)")
                .set("margin-top", "0.5rem");
    }
}
