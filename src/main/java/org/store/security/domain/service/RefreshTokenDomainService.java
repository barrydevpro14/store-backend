package org.store.security.domain.service;

import org.springframework.stereotype.Service;
import org.store.common.service.GlobalService;
import org.store.security.domain.model.RefreshToken;
import org.store.security.domain.repository.RefreshTokenRepository;

@Service
public class RefreshTokenDomainService extends GlobalService<RefreshToken, RefreshTokenRepository> {
    public RefreshTokenDomainService(RefreshTokenRepository repository) {
        super(repository);
    }
}
