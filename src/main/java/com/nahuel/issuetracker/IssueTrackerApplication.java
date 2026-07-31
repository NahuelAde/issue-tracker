package com.nahuel.issuetracker;

import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.page.AppShellConfigurator;
import com.vaadin.flow.theme.aura.Aura;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@StyleSheet(Aura.STYLESHEET)
@StyleSheet("styles.css") // Your custom styles
public class IssueTrackerApplication implements AppShellConfigurator {

    public static void main(String[] args) {
        // Spring Boot fija "java.awt.headless=true" muy pronto en el arranque (antes de leer
        // application.properties, comprobado en el bytecode real de SpringApplication), así que
        // "spring.main.headless=false" en las properties llega demasiado tarde. Hay que fijar la
        // propiedad de sistema aquí, antes de SpringApplication.run(): Spring Boot respeta el valor
        // si ya está puesto y no lo sobrescribe. Necesario para que BrowserLauncher (AWT Desktop)
        // pueda abrir el navegador.
        System.setProperty("java.awt.headless", "false");
        SpringApplication.run(IssueTrackerApplication.class, args);
    }

}
