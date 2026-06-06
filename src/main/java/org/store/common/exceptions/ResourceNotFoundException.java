package org.store.common.exceptions;

public class ResourceNotFoundException extends LocalizedRuntimeException {
    public ResourceNotFoundException(String messageKey, Object... args) {
        super(messageKey, args);
    }
}
