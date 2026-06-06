package org.store.common.exceptions;

import java.util.Set;

public record ErrorResponse(int statusCode , String message , Set<Error> errors) {
}
