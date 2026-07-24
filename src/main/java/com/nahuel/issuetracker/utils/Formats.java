package com.nahuel.issuetracker.utils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/** Shared display formatting: dates as dd/MM/yyyy and hours with a Spanish comma. */
public final class Formats {

    private static final Locale ES = Locale.of("es", "ES");
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DATE_SHORT = DateTimeFormatter.ofPattern("dd/MM/yy");
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private Formats() {
    }

    public static String date(LocalDate date) {
        return date == null ? "" : DATE.format(date);
    }

    /** Short date with two-digit year (dd/MM/yy), for compact columns. */
    public static String dateShort(LocalDateTime dateTime) {
        return dateTime == null ? "" : DATE_SHORT.format(dateTime);
    }

    public static String dateTime(LocalDateTime dateTime) {
        return dateTime == null ? "" : DATE_TIME.format(dateTime);
    }

    public static String hours(BigDecimal value) {
        return value == null ? "—" : String.format(ES, "%.2f", value);
    }
}
