package org.store.security.application.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.store.common.exceptions.UnauthorisedException;
import org.store.property.JwtProperties;
import org.store.security.application.dto.AuthResponse;
import org.store.security.application.dto.UserPrincipal;
import org.store.security.domain.model.Account;
import org.store.security.domain.model.RefreshToken;
import org.store.security.domain.repository.RefreshTokenRepository;
import org.store.users.domain.model.Utilisateur;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class RefreshTokenServiceImpl implements IRefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final IJwtService jwtService;
    private final IUserPrincipalFactory userPrincipalFactory;
    private final JwtProperties jwtProperties;

    public RefreshTokenServiceImpl(RefreshTokenRepository refreshTokenRepository,
                                   IJwtService jwtService,
                                   IUserPrincipalFactory userPrincipalFactory,
                                   JwtProperties jwtProperties) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.jwtService = jwtService;
        this.userPrincipalFactory = userPrincipalFactory;
        this.jwtProperties = jwtProperties;
    }

    @Override
    @Transactional
    public String create(Account account) {
        RefreshToken token = new RefreshToken();
        token.setToken(UUID.randomUUID().toString());
        token.setUser(account.getUser());
        token.setExpiryDate(LocalDateTime.now().plus(jwtProperties.expiration().refreshToken()));
        token.setRevoked(false);
        return refreshTokenRepository.save(token).getToken();
    }

    @Override
    @Transactional(readOnly = true)
    public AuthResponse refresh(String refreshTokenValue) {
        RefreshToken token = refreshTokenRepository.findByToken(refreshTokenValue)
                .orElseThrow(() -> new UnauthorisedException("refreshToken.invalid"));

        if (token.isRevoked()) {
            throw new UnauthorisedException("refreshToken.revoked");
        }
        if (token.getExpiryDate() == null || token.getExpiryDate().isBefore(LocalDateTime.now())) {
            throw new UnauthorisedException("refreshToken.expired");
        }

        Utilisateur user = token.getUser();
        if (user == null || user.getAccount() == null) {
            throw new UnauthorisedException("refreshToken.invalid");
        }

        UserPrincipal principal = userPrincipalFactory.build(user.getAccount());
        String accessToken = jwtService.generateToken(principal);
        return new AuthResponse(accessToken, refreshTokenValue);
    }

    @Override
    @Transactional
    public void revoke(String refreshTokenValue) {
        refreshTokenRepository.findByToken(refreshTokenValue).ifPresent(token -> {
            if (!token.isRevoked()) {
                token.setRevoked(true);
                refreshTokenRepository.save(token);
            }
        });
    }
}
