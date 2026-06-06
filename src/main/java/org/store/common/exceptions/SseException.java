package org.store.common.exceptions;

public class SseException extends LocalizedRuntimeException {
    public SseException(String messageKey, Object... args) {
        super(messageKey, args);
    }
}
