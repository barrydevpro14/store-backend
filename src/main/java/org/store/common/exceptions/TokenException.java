package org.store.common.exceptions;

public class TokenException extends LocalizedRuntimeException {
    public TokenException(String messageKey, Object... args) {
        super(messageKey, args);
    }
}
