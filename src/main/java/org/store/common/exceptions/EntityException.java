package org.store.common.exceptions;

public class EntityException extends LocalizedRuntimeException {
    public EntityException(String messageKey, Object... args) {
        super(messageKey, args);
    }
}
