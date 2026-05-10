package org.store.common.exceptions;

import org.springframework.security.core.AuthenticationException;

public class AuthentificationException extends AuthenticationException {
    public AuthentificationException(String msg) {
        super(msg);
    }
}
