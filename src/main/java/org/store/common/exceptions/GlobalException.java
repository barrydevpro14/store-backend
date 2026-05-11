package org.store.common.exceptions;


import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.store.common.i18n.IMessageSourceService;

import java.util.Set;
import java.util.TreeSet;

@RestControllerAdvice
public class GlobalException {
    private final Logger logger = LoggerFactory.getLogger(GlobalException.class.getSimpleName());
    private final IMessageSourceService messageSourceService;

    public GlobalException(IMessageSourceService messageSourceService) {
        this.messageSourceService = messageSourceService;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> methodArgumentNotValidException(MethodArgumentNotValidException ex) {
        Set<Error> errors = new TreeSet<>();
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String errorMessage = error.getDefaultMessage();
            String field = ((FieldError)error).getField();
            errors.add(new Error(field, errorMessage));
        });

        return new ResponseEntity<>(new ErrorResponse(HttpStatus.BAD_REQUEST.value(),
                messageSourceService.getMessage("validation.error"), errors), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolation(ConstraintViolationException ex) {

        Set<Error> errors = new TreeSet<>();

        for (ConstraintViolation<?> violation : ex.getConstraintViolations()) {
            String field = violation.getPropertyPath().toString();
            String message = violation.getMessage();
            errors.add(new Error(field, message));
        }

        return new ResponseEntity<>(new ErrorResponse(HttpStatus.BAD_REQUEST.value(),
                messageSourceService.getMessage("validation.error"), errors), HttpStatus.BAD_REQUEST);
    }


    @ExceptionHandler(UniqueResourceException.class)
    public ResponseEntity<ErrorResponse> uniqueResourceException(UniqueResourceException ex) {
        return buildError(resolve(ex), HttpStatus.BAD_REQUEST.value());
    }

    @ExceptionHandler(FormatDataException.class)
    public ResponseEntity<ErrorResponse> formatDataException(FormatDataException ex) {
        return buildError(resolve(ex), HttpStatus.BAD_REQUEST.value());
    }

    @ExceptionHandler(RestTemplateException.class)
    public ResponseEntity<ErrorResponse> restTemplate(RestTemplateException ex) {
        return buildError(resolve(ex), HttpStatus.SERVICE_UNAVAILABLE.value());
    }

    @ExceptionHandler(BadArgumentException.class)
    public ResponseEntity<ErrorResponse> badArgumentException(BadArgumentException ex) {
        return buildError(resolve(ex), HttpStatus.BAD_REQUEST.value());
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> resourceNotFoundException(ResourceNotFoundException ex) {
        return buildError(resolve(ex), HttpStatus.NOT_FOUND.value());
    }

    @ExceptionHandler(AuthentificationException.class)
    public ResponseEntity<ErrorResponse> authentificationException(AuthentificationException ex) {
        return buildError(ex.getMessage(), HttpStatus.UNAUTHORIZED.value());
    }

    @ExceptionHandler(EntityException.class)
    public ResponseEntity<ErrorResponse> entityException(EntityException ex) {
        return buildError(resolve(ex), HttpStatus.NOT_ACCEPTABLE.value());
    }

    @ExceptionHandler(PaymentException.class)
    public ResponseEntity<ErrorResponse> paymentException(PaymentException ex) {
        return buildError(resolve(ex), ex.getStatusCode());
    }

    @ExceptionHandler(TokenException.class)
    public ResponseEntity<ErrorResponse> tokenException(TokenException ex) {
        return buildError(resolve(ex), HttpStatus.NOT_ACCEPTABLE.value());
    }

    @ExceptionHandler(MailException.class)
    public ResponseEntity<ErrorResponse> mailException(MailException ex) {
        return buildError(resolve(ex), HttpStatus.SERVICE_UNAVAILABLE.value());
    }

    @ExceptionHandler(NullPointerException.class)
    public ResponseEntity<ErrorResponse> nullPointerException(NullPointerException ex) {
        return buildError(ex.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR.value());
    }

    @ExceptionHandler(SseException.class)
    public ResponseEntity<ErrorResponse> seeException(SseException ex) {
        return buildError(resolve(ex), HttpStatus.SERVICE_UNAVAILABLE.value());
    }

    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<ErrorResponse> forbiddenException(ForbiddenException ex) {
        return buildError(resolve(ex), HttpStatus.FORBIDDEN.value());
    }

    @ExceptionHandler(UnauthorisedException.class)
    public ResponseEntity<ErrorResponse> unauthorisedException(UnauthorisedException ex) {
        return buildError(resolve(ex), HttpStatus.UNAUTHORIZED.value());
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErrorResponse> badCredentialsException(BadCredentialsException ex) {
        return buildError(ex.getMessage(), HttpStatus.UNAUTHORIZED.value());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> errorServer(Exception ex) {
        logger.error("error server", ex);
        return buildError(messageSourceService.getMessage("error.unexpected"), HttpStatus.INTERNAL_SERVER_ERROR.value());
    }


    private String resolve(LocalizedRuntimeException ex) {
        return ex.getMessageKey() != null
                ? messageSourceService.getMessage(ex.getMessageKey(), ex.getArgs())
                : ex.getMessage();
    }

    private ResponseEntity<ErrorResponse> buildError(String message, int statusCode) {
        return new ResponseEntity<>(new ErrorResponse(statusCode, message, null), HttpStatus.valueOf(statusCode));
    }


}
