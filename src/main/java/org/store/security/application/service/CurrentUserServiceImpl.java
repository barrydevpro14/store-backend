package org.store.security.application.service;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.store.common.exceptions.UnauthorisedException;
import org.store.security.application.dto.UserPrincipal;

@Service
public class CurrentUserServiceImpl implements ICurrentUserService {

    @Override
    public UserPrincipal getCurrent() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new UnauthorisedException("auth.current.missing");
        }
        Object principal = authentication.getPrincipal();
        if (!(principal instanceof UserPrincipal userPrincipal)) {
            throw new UnauthorisedException("auth.current.missing");
        }
        return userPrincipal;
    }
}
