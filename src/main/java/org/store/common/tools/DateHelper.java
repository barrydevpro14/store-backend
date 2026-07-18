package org.store.common.tools;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public final class DateHelper {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter DATE_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter DATE_DISPLAY_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    /** Sentinel lower bound used to avoid IS NULL type-inference issues with PostgreSQL (2000-01-01). */
    public static final LocalDateTime SENTINEL_START = LocalDateTime.of(2000, 1, 1, 0, 0, 0);

    /** Sentinel upper bound used to avoid IS NULL type-inference issues with PostgreSQL (2099-12-31). */
    public static final LocalDateTime SENTINEL_END = LocalDateTime.of(2099, 12, 31, 23, 59, 59);

    private DateHelper() {
    }

    /** Returns value if non-null, otherwise SENTINEL_START. */
    public static LocalDateTime coalesceStart(LocalDateTime value) {
        return value != null ? value : SENTINEL_START;
    }

    /** Returns value if non-null, otherwise SENTINEL_END. */
    public static LocalDateTime coalesceEnd(LocalDateTime value) {
        return value != null ? value : SENTINEL_END;
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

    /** Formate une LocalDate en String "dd/MM/yyyy" pour l'affichage (PDF, UI). Retourne null si null. */
    public static String formatDisplay(LocalDate date) {
        return date != null ? date.format(DATE_DISPLAY_FORMAT) : null;
    }
}
