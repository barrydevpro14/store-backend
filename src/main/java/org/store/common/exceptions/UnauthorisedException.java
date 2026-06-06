package org.store.common.exceptions;

public class UnauthorisedException extends LocalizedRuntimeException {
    public UnauthorisedException(String messageKey, Object... args) {
        super(messageKey, args);
    }
}
