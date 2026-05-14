package org.store.common.tools;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public final class DateHelper {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter DATE_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private DateHelper() {
    }

    /** Convertit une date ISO (yyyy-MM-dd) en LocalDateTime au début de la journée (00:00:00). Retourne null si null/blank. */
    public static LocalDateTime parseStartOfDay(String date) {
        return date != null && !date.isBlank() ? LocalDate.parse(date, DATE_FORMAT).atStartOfDay() : null;
    }

    /** Convertit une date ISO (yyyy-MM-dd) en LocalDateTime à la fin de la journée (23:59:59.999...). Retourne null si null/blank. */
    public static LocalDateTime parseEndOfDay(String date) {
        return date != null && !date.isBlank() ? LocalDate.parse(date, DATE_FORMAT).atTime(LocalTime.MAX) : null;
    }

    /** Formate une LocalDateTime en String "yyyy-MM-dd HH:mm:ss". Retourne null si null. */
    public static String format(LocalDateTime dateTime) {
        return dateTime != null ? dateTime.format(DATE_TIME_FORMAT) : null;
    }

    /** Formate une LocalDate en String "yyyy-MM-dd". Retourne null si null. */
    public static String format(LocalDate date) {
        return date != null ? date.format(DATE_FORMAT) : null;
    }
}
