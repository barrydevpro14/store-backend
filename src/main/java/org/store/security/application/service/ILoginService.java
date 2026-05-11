package org.store.security.application.service;

import org.store.security.application.dto.AuthResponse;
import org.store.security.application.dto.LoginRequest;

public interface ILoginService {

    AuthResponse login(LoginRequest request);
}
