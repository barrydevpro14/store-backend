package org.store.security.application.service.impl;

import org.store.security.application.service.*;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.store.abonnement.application.service.IAbonnementService;
import org.store.common.exceptions.ForbiddenException;
import org.store.security.application.dto.AuthResponse;
import org.store.security.application.dto.LoginRequest;
import org.store.security.application.dto.UserPrincipal;
import org.store.security.domain.model.Account;

/**
 * Handles the login flow: validates credentials, enforces the subscription gate, and issues an
 * access + refresh token pair. The gate rejects tenants whose entreprise has no active paid
 * Abonnement and no live trial window — ADMIN accounts (no entreprise attached) bypass the gate.
 */
@Service
public class LoginServiceImpl implements ILoginService {

    private final AuthenticationManager authenticationManager;
    private final IAccountService accountService;
    private final IAbonnementService abonnementService;
    private final IJwtService jwtService;
    private final IUserPrincipalFactory userPrincipalFactory;
    private final IRefreshTokenService refreshTokenService;

    public LoginServiceImpl(AuthenticationManager authenticationManager,
                            IAccountService accountService,
                            IAbonnementService abonnementService,
                            IJwtService jwtService,
                            IUserPrincipalFactory userPrincipalFactory,
                            IRefreshTokenService refreshTokenService) {
        this.authenticationManager = authenticationManager;
        this.accountService = accountService;
        this.abonnementService = abonnementService;
        this.jwtService = jwtService;
        this.userPrincipalFactory = userPrincipalFactory;
        this.refreshTokenService = refreshTokenService;
    }

    /** Authenticates the caller, runs the subscription gate, then issues the JWT pair. */
    @Override
    @Transactional
    public AuthResponse login(LoginRequest loginRequest) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(loginRequest.username(), loginRequest.password())
        );

        Account account = accountService.findByUsername(loginRequest.username());
        UserPrincipal principal = userPrincipalFactory.build(account);
        ensureEntrepriseHasActiveSubscription(principal);

        String accessToken = jwtService.generateToken(principal);
        String refreshToken = refreshTokenService.create(account);
        return new AuthResponse(accessToken, refreshToken);
    }

    /**
     * Rejects callers whose entreprise has no active paid Abonnement and no live trial window.
     * Skipped for ADMIN principals (no entreprise attached).
     */
    public void ensureEntrepriseHasActiveSubscription(UserPrincipal principal) {
        if (principal.entrepriseId() == null) {
            return;
        }
        if (!abonnementService.hasActiveSubscription(principal.entrepriseId())) {
            throw new ForbiddenException("auth.subscription.required");
        }
    }
}
