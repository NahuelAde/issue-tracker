package com.nahuel.issuetracker.utils;

import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;

/** Centered notifications, so they don't cover action buttons. */
public final class Notifications {

    private static final int DURATION = 3000;

    private Notifications() {
    }

    public static void success(String message) {
        Notification n = Notification.show(message, DURATION, Notification.Position.MIDDLE);
        n.addThemeVariants(NotificationVariant.LUMO_SUCCESS);
    }

    public static void error(String message) {
        Notification n = Notification.show(message, 4000, Notification.Position.MIDDLE);
        n.addThemeVariants(NotificationVariant.LUMO_ERROR);
    }

    public static void info(String message) {
        Notification.show(message, DURATION, Notification.Position.MIDDLE);
    }
}
