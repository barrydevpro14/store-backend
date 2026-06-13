package org.store.security.domain.service;

import org.springframework.stereotype.Service;
import org.store.common.service.GlobalService;
import org.store.security.domain.model.RefreshToken;
import org.store.security.domain.repository.RefreshTokenRepository;

import java.util.Optional;
import java.util.UUID;

@Service
public class RefreshTokenDomainService extends GlobalService<RefreshToken, RefreshTokenRepository> {
    public RefreshTokenDomainService(RefreshTokenRepository repository) {
        super(repository);
    }

    public Optional<RefreshToken> findByToken(String token) {
        return repository.findByToken(token);
    }

    /** Supprime tous les refresh tokens d'un utilisateur (avant suppression definitive). */
    public void deleteByUserId(UUID userId) {
        repository.deleteByUser_Id(userId);
    }
}
