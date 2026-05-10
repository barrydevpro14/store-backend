package org.store.common.exceptions;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class PaymentException extends RuntimeException {
    private int statusCode;
    public PaymentException(String message) {
        super(message);
        this.statusCode = HttpStatus.NOT_ACCEPTABLE.value();
    }
    public PaymentException(String message, int code) {
        super(message, new Throwable(String.valueOf(code)));
    }

}
