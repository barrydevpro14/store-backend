package org.store.security.application.service;

import org.store.security.application.dto.UserPrincipal;

public interface ICurrentUserService {

    UserPrincipal getCurrent();
}
