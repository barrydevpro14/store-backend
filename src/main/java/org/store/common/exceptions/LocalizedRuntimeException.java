package org.store.common.exceptions;

import lombok.Getter;

public abstract class LocalizedRuntimeException extends RuntimeException {

    @Getter
    private final String messageKey;
    private final Object[] args;

    protected LocalizedRuntimeException(String messageKey, Object... args) {
        super(messageKey);
        this.messageKey = messageKey;
        this.args = args;
    }

    protected LocalizedRuntimeException(String messageKey, Throwable cause, Object... args) {
        super(messageKey, cause);
        this.messageKey = messageKey;
        this.args = args;
    }

    public Object[] getArgs() {
        return args == null ? new Object[0] : args;
    }
}
