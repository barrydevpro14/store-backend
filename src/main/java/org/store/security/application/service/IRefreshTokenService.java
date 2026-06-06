package org.store.security.application.service;

import org.store.security.application.dto.AuthResponse;
import org.store.security.domain.model.Account;

public interface IRefreshTokenService {

    String create(Account account);

    AuthResponse refresh(String refreshTokenValue);

    void revoke(String refreshTokenValue);
}
