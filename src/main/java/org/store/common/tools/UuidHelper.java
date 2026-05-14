package org.store.common.tools;

import java.util.UUID;

public final class UuidHelper {

    private UuidHelper() {
    }

    /** Parse une String en UUID. Retourne null si null/blank. Throw IllegalArgumentException si format invalide. */
    public static UUID parse(String value) {
        return value != null && !value.isBlank() ? UUID.fromString(value) : null;
    }
}
