package org.store.security.domain.service;

import org.springframework.stereotype.Service;
import org.store.common.service.GlobalService;
import org.store.security.domain.model.PasswordResetToken;
import org.store.security.domain.repository.PasswordResetTokenRepository;

import java.util.Optional;
import java.util.UUID;

@Service
public class PasswordResetTokenDomainService extends GlobalService<PasswordResetToken, PasswordResetTokenRepository> {

    public PasswordResetTokenDomainService(PasswordResetTokenRepository repository) {
        super(repository);
    }

    public Optional<PasswordResetToken> findByToken(String token) {
        return repository.findByToken(token);
    }

    public void deleteByAccountId(UUID accountId) {
        repository.deleteByAccount_Id(accountId);
    }
}
