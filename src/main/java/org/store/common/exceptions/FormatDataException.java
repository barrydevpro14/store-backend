package org.store.common.exceptions;

public class FormatDataException extends LocalizedRuntimeException {
    public FormatDataException(String messageKey, Object... args) {
        super(messageKey, args);
    }
}
