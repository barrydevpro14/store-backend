package org.store.security.application.dto;

public record AuthResponse(String accessToken, String refreshToken) {
}
