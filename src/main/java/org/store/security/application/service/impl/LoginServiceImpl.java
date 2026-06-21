package org.store.security.application.service.impl;

import org.store.security.application.service.*;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.store.abonnement.application.service.IAbonnementService;
import org.store.audit.application.event.AuditEvent;
import org.store.audit.application.service.IAuditEventPublisher;
import org.store.audit.domain.enums.AuditAction;
import org.store.audit.domain.enums.AuditEntityType;
import org.store.common.exceptions.ForbiddenException;
import org.store.common.tools.RequestHelper;
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
    private final IAuditEventPublisher auditEventPublisher;

    public LoginServiceImpl(AuthenticationManager authenticationManager,
                            IAccountService accountService,
                            IAbonnementService abonnementService,
                            IJwtService jwtService,
                            IUserPrincipalFactory userPrincipalFactory,
                            IRefreshTokenService refreshTokenService,
                            IAuditEventPublisher auditEventPublisher) {
        this.authenticationManager = authenticationManager;
        this.accountService = accountService;
        this.abonnementService = abonnementService;
        this.jwtService = jwtService;
        this.userPrincipalFactory = userPrincipalFactory;
        this.refreshTokenService = refreshTokenService;
        this.auditEventPublisher = auditEventPublisher;
    }

    /** Authenticates the caller, then issues a full or restricted JWT pair depending on subscription state. */
    @Override
    @Transactional
    public AuthResponse login(LoginRequest loginRequest) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(loginRequest.username(), loginRequest.password())
        );

        Account account = accountService.findByUsername(loginRequest.username());
        UserPrincipal principal = userPrincipalFactory.build(account);

        String accessToken = generateTokenForSubscriptionState(principal);
        String refreshToken = refreshTokenService.create(account);

        String details = "IP: " + RequestHelper.getClientIp() + " | UA: " + RequestHelper.getUserAgent();
        auditEventPublisher.publish(new AuditEvent(
                AuditAction.LOGIN, AuditEntityType.ACCOUNT,
                principal.accountId(), principal.username(),
                principal.accountId().toString(), principal.username(),
                principal.entrepriseId(), principal.magasinId(), details));

        return new AuthResponse(accessToken, refreshToken);
    }

    /**
     * Generates a full token for ADMIN and active-subscription callers.
     * For an OWNER with expired subscription: emits a restricted token (subscription endpoints only).
     * For an EMPLOYEE with expired subscription: blocks login entirely.
     */
    private String generateTokenForSubscriptionState(UserPrincipal principal) {
        if (principal.entrepriseId() == null) {
            return jwtService.generateToken(principal);
        }

        boolean isEmployee      = principal.magasinId() != null;
        boolean hasSubscription = abonnementService.hasActiveSubscription(principal.entrepriseId());

        if (hasSubscription) {
            return jwtService.generateToken(principal);
        }
        if (isEmployee) {
            throw new ForbiddenException("auth.subscription.employee.expired");
        }
        return jwtService.generateRestrictedToken(principal);
    }
}
