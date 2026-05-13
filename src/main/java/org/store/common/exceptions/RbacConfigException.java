package org.store.common.exceptions;

public class RbacConfigException extends LocalizedRuntimeException {
    public RbacConfigException(String messageKey, Object... args) {
        super(messageKey, args);
    }

    public RbacConfigException(String messageKey, Throwable cause, Object... args) {
        super(messageKey, cause, args);
    }
}
