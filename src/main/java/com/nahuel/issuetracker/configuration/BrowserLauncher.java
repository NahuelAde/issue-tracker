package com.nahuel.issuetracker.configuration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

import java.awt.Desktop;
import java.net.URI;

/**
 * Abre el navegador al terminar de arrancar, en desarrollo y en producción por igual.
 * {@code vaadin.launch-browser} (propiedad de Vaadin) solo funciona en modo desarrollo, así que
 * el jar/ejecutable empaquetado nunca abría el navegador solo; de ahí esta propia implementación,
 * con su propia propiedad ({@code app.launch-browser}) para no chocar con la de Vaadin y evitar
 * que se abran dos pestañas en desarrollo.
 */
@Component
public class BrowserLauncher implements ApplicationListener<ApplicationReadyEvent> {

    private static final Logger log = LoggerFactory.getLogger(BrowserLauncher.class);

    @Value("${app.launch-browser:true}")
    private boolean launchBrowser;

    @Value("${server.port}")
    private int port;

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        if (!launchBrowser) {
            log.info("BrowserLauncher: desactivado por configuración (app.launch-browser=false)");
            return;
        }
        log.info("BrowserLauncher: java.awt.headless={}", System.getProperty("java.awt.headless"));
        if (!Desktop.isDesktopSupported()) {
            log.warn("BrowserLauncher: Desktop no soportado en esta JVM, no se puede abrir el navegador");
            return;
        }
        if (!Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
            log.warn("BrowserLauncher: la acción BROWSE no está soportada en este sistema");
            return;
        }
        try {
            Desktop.getDesktop().browse(new URI("http://localhost:" + port));
            log.info("BrowserLauncher: navegador abierto en http://localhost:{}", port);
        } catch (Exception e) {
            log.warn("No se pudo abrir el navegador automáticamente", e);
        }
    }
}
