package com.nahuel.issuetracker.configuration;

import com.vaadin.flow.component.shared.TooltipConfiguration;
import com.vaadin.flow.server.ServiceInitEvent;
import com.vaadin.flow.server.VaadinServiceInitListener;
import org.springframework.stereotype.Component;

/**
 * Retraso único para todos los tooltips de la aplicación: antes cada uno lo fijaba a su
 * manera (o no lo fijaba), lo que hacía que unos se abrieran casi al instante y otros no.
 */
@Component
public class TooltipHoverDelayConfig implements VaadinServiceInitListener {

    private static final int HOVER_DELAY_MS = 700;

    @Override
    public void serviceInit(ServiceInitEvent event) {
        event.getSource().addUIInitListener(
                uiEvent -> TooltipConfiguration.setDefaultHoverDelay(HOVER_DELAY_MS));
    }
}
