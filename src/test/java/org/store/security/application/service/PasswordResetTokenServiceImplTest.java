package org.store.security.application.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.store.security.application.service.impl.PasswordResetTokenServiceImpl;
import org.store.security.domain.model.PasswordResetToken;
import org.store.security.domain.service.PasswordResetTokenDomainService;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PasswordResetTokenServiceImplTest {

    @Mock
    private PasswordResetTokenDomainService passwordResetTokenDomainService;

    @InjectMocks
    private PasswordResetTokenServiceImpl service;

    @Test
    void findByToken_should_delegate_to_domain_service() {
        PasswordResetToken token = new PasswordResetToken();
        when(passwordResetTokenDomainService.findByToken("tok-abc")).thenReturn(Optional.of(token));

        Optional<PasswordResetToken> result = service.findByToken("tok-abc");

        assertThat(result).contains(token);
    }

    @Test
    void findByToken_should_return_empty_when_not_found() {
        when(passwordResetTokenDomainService.findByToken("unknown")).thenReturn(Optional.empty());

        Optional<PasswordResetToken> result = service.findByToken("unknown");

        assertThat(result).isEmpty();
    }

    @Test
    void deleteByAccountId_should_delegate_to_domain_service() {
        UUID accountId = UUID.randomUUID();

        service.deleteByAccountId(accountId);

        verify(passwordResetTokenDomainService).deleteByAccountId(accountId);
    }

    @Test
    void save_should_delegate_to_domain_service_and_return_saved_token() {
        PasswordResetToken token = new PasswordResetToken();
        PasswordResetToken savedToken = new PasswordResetToken();
        when(passwordResetTokenDomainService.save(token)).thenReturn(savedToken);

        PasswordResetToken result = service.save(token);

        assertThat(result).isSameAs(savedToken);
    }
}
