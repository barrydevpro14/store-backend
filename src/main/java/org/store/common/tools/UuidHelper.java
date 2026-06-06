package org.store.common.tools;

import java.util.Optional;
import java.util.UUID;

public final class UuidHelper {

    private UuidHelper() {
    }

    /** Parse une String en UUID. Retourne null si null/blank. Throw IllegalArgumentException si format invalide. */
    public static UUID parse(String value) {
        return value != null && !value.isBlank() ? UUID.fromString(value) : null;
    }

    /** Parse une String en UUID de façon safe : Optional.empty() si null/blank ou format invalide (pas de throw). */
    public static Optional<UUID> parseOptional(String value) {
        if (value == null || value.isBlank()) return Optional.empty();
        try {
            return Optional.of(UUID.fromString(value));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}
