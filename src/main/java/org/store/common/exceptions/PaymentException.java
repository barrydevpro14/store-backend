package org.store.common.exceptions;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class PaymentException extends LocalizedRuntimeException {

    private final int statusCode;

    public PaymentException(String messageKey, Object... args) {
        super(messageKey, args);
        this.statusCode = HttpStatus.NOT_ACCEPTABLE.value();
    }

    public PaymentException(int statusCode, String messageKey, Object... args) {
        super(messageKey, args);
        this.statusCode = statusCode;
    }
}
