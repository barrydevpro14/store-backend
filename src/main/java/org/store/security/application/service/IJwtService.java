package org.store.security.application.service;

import org.store.security.application.dto.UserPrincipal;

public interface IJwtService {

    String generateToken(UserPrincipal principal);

    boolean isTokenValid(String token);

    UserPrincipal extractUserPrincipal(String token);
}
