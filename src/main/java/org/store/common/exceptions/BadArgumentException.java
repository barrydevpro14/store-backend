package org.store.common.exceptions;

public class BadArgumentException extends LocalizedRuntimeException {
    public BadArgumentException(String messageKey, Object... args) {
        super(messageKey, args);
    }
}
