package org.store.security.application.service;

import org.store.security.application.dto.AuthResponse;
import org.store.security.application.dto.RegisterPropertyRequest;

public interface IRegisterPropertyService {

    AuthResponse register(RegisterPropertyRequest request);
}
