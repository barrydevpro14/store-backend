package org.store.common.exceptions;

public class UniqueResourceException extends LocalizedRuntimeException {
    public UniqueResourceException(String messageKey, Object... args) {
        super(messageKey, args);
    }
}
