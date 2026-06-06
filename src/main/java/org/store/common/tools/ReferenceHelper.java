package org.store.common.tools;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public final class ReferenceHelper {

    private static final DateTimeFormatter REFERENCE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmssSSS");

    private ReferenceHelper() {
    }

    /** Génère une référence unique au format {base}-yyyyMMdd-HHmmssSSS. */
    public static String generate(String base) {
        return base + "-" + LocalDateTime.now().format(REFERENCE_FORMAT);
    }
}
