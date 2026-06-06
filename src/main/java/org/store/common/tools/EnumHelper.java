package org.store.common.tools;

public final class EnumHelper {

    private EnumHelper() {
    }

    /** Parse une String en valeur d'enum. Retourne null si null/blank. Throw IllegalArgumentException si valeur invalide. */
    public static <E extends Enum<E>> E parse(Class<E> enumClass, String value) {
        return value != null && !value.isBlank() ? Enum.valueOf(enumClass, value) : null;
    }
}
