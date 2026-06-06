package org.store.common.exceptions;

public class ForbiddenException extends LocalizedRuntimeException {
    public ForbiddenException(String messageKey, Object... args) {
        super(messageKey, args);
    }
}
