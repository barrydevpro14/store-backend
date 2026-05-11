package org.store.common.exceptions;

public class RestTemplateException extends LocalizedRuntimeException {
    public RestTemplateException(String messageKey, Object... args) {
        super(messageKey, args);
    }
}
