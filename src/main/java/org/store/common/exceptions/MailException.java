package org.store.common.exceptions;

public class MailException extends LocalizedRuntimeException {
    public MailException(String messageKey, Object... args) {
        super(messageKey, args);
    }
}
