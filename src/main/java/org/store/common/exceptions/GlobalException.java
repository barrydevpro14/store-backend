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
        String message = messageSourceService.getMessage("validation.error");
        logger.warn("HTTP 400 - {} ({})", message, errors);
        return new ResponseEntity<>(new ErrorResponse(HttpStatus.BAD_REQUEST.value(), message, errors), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolation(ConstraintViolationException ex) {

        Set<Error> errors = new TreeSet<>();

        for (ConstraintViolation<?> violation : ex.getConstraintViolations()) {
            String field = violation.getPropertyPath().toString();
            String message = violation.getMessage();
            errors.add(new Error(field, message));
        }
        String message = messageSourceService.getMessage("validation.error");
        logger.warn("HTTP 400 - {} ({})", message, errors);
        return new ResponseEntity<>(new ErrorResponse(HttpStatus.BAD_REQUEST.value(), message, errors), HttpStatus.BAD_REQUEST);
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
        return buildError(resolve(ex), HttpStatus.SERVICE_UNAVAILABLE.value(), ex);
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
        return buildError(resolve(ex), HttpStatus.SERVICE_UNAVAILABLE.value(), ex);
    }

    @ExceptionHandler(NullPointerException.class)
    public ResponseEntity<ErrorResponse> nullPointerException(NullPointerException ex) {
        return buildError(ex.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR.value(), ex);
    }

    @ExceptionHandler(SseException.class)
    public ResponseEntity<ErrorResponse> seeException(SseException ex) {
        return buildError(resolve(ex), HttpStatus.SERVICE_UNAVAILABLE.value(), ex);
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
        return buildError(messageSourceService.getMessage("error.unexpected"), HttpStatus.INTERNAL_SERVER_ERROR.value(), ex);
    }


    private String resolve(LocalizedRuntimeException ex) {
        return ex.getMessageKey() != null
                ? messageSourceService.getMessage(ex.getMessageKey(), ex.getArgs())
                : ex.getMessage();
    }

    /**
     * Build une réponse d'erreur sans cause (utilisé pour les 4xx métier).
     * Log automatiquement en WARN.
     */
    private ResponseEntity<ErrorResponse> buildError(String message, int statusCode) {
        return buildError(message, statusCode, null);
    }

    /**
     * Build une réponse d'erreur avec cause optionnelle.
     * Log en WARN sans stack si statusCode &lt; 500, en ERROR avec stack sinon.
     */
    private ResponseEntity<ErrorResponse> buildError(String message, int statusCode, Throwable cause) {
        if (statusCode >= 500) {
            if (cause != null) {
                logger.error("HTTP {} - {}", statusCode, message, cause);
            } else {
                logger.error("HTTP {} - {}", statusCode, message);
            }
        } else {
            logger.warn("HTTP {} - {}", statusCode, message);
        }
        return new ResponseEntity<>(new ErrorResponse(statusCode, message, null), HttpStatus.valueOf(statusCode));
    }


}
