package org.store.security.application.service.impl;

import org.store.abonnement.application.service.IAbonnementService;
import org.store.common.exceptions.ForbiddenException;
import org.store.security.application.service.IJwtService;
import org.store.security.application.service.IRefreshTokenService;
import org.store.security.application.service.IUserPrincipalFactory;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.store.audit.application.event.AuditEvent;
import org.store.audit.application.service.IAuditEventPublisher;
import org.store.audit.domain.enums.AuditAction;
import org.store.audit.domain.enums.AuditEntityType;
import org.store.audit.domain.service.AuditLogDomainService;
import org.store.common.exceptions.UnauthorisedException;
import org.store.common.tools.RequestHelper;
import org.store.property.JwtProperties;
import org.store.security.application.dto.AuthResponse;
import org.store.security.application.dto.UserPrincipal;
import org.store.security.domain.model.Account;
import org.store.security.domain.model.RefreshToken;
import org.store.security.domain.service.RefreshTokenDomainService;
import org.store.users.domain.model.Utilisateur;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class RefreshTokenServiceImpl implements IRefreshTokenService {

    private final RefreshTokenDomainService refreshTokenDomainService;
    private final IJwtService jwtService;
    private final IUserPrincipalFactory userPrincipalFactory;
    private final JwtProperties jwtProperties;
    private final IAuditEventPublisher auditEventPublisher;
    private final AuditLogDomainService auditLogDomainService;
    private final IAbonnementService abonnementService;

    public RefreshTokenServiceImpl(RefreshTokenDomainService refreshTokenDomainService,
                                   IJwtService jwtService,
                                   IUserPrincipalFactory userPrincipalFactory,
                                   JwtProperties jwtProperties,
                                   IAuditEventPublisher auditEventPublisher,
                                   AuditLogDomainService auditLogDomainService,
                                   IAbonnementService abonnementService) {
        this.refreshTokenDomainService = refreshTokenDomainService;
        this.jwtService = jwtService;
        this.userPrincipalFactory = userPrincipalFactory;
        this.jwtProperties = jwtProperties;
        this.auditEventPublisher = auditEventPublisher;
        this.auditLogDomainService = auditLogDomainService;
        this.abonnementService = abonnementService;
    }

    @Override
    @Transactional
    public String create(Account account) {
        RefreshToken token = new RefreshToken();
        token.setToken(UUID.randomUUID().toString());
        token.setUser(account.getUser());
        token.setExpiryDate(LocalDateTime.now().plus(jwtProperties.expiration().refreshToken()));
        token.setRevoked(false);
        return refreshTokenDomainService.save(token).getToken();
    }

    @Override
    @Transactional(readOnly = true)
    public AuthResponse refresh(String refreshTokenValue) {
        RefreshToken token = refreshTokenDomainService.findByToken(refreshTokenValue)
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
        String accessToken = generateTokenForSubscriptionState(principal);
        return new AuthResponse(accessToken, refreshTokenValue);
    }

    @Override
    @Transactional
    public void revoke(String refreshTokenValue) {
        refreshTokenDomainService.findByToken(refreshTokenValue).ifPresent(token -> {
            if (!token.isRevoked()) {
                token.setRevoked(true);
                refreshTokenDomainService.save(token);
                publishLogoutAudit(token);
            }
        });
    }

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

    private void publishLogoutAudit(RefreshToken token) {
        if (token.getUser() == null || token.getUser().getAccount() == null) return;

        Account account = token.getUser().getAccount();
        UserPrincipal principal = userPrincipalFactory.build(account);
        String accountId = principal.accountId().toString();

        String duration = auditLogDomainService.findLastLogin(accountId)
                .map(last -> AuditLogDomainService.formatDuration(last.getCreatedAt()))
                .orElse("unknown");

        String details = "IP: " + RequestHelper.getClientIp() + " | Duration: " + duration;
        auditEventPublisher.publish(new AuditEvent(
                AuditAction.LOGOUT, AuditEntityType.ACCOUNT,
                principal.accountId(), principal.username(),
                accountId, principal.username(),
                principal.entrepriseId(), principal.magasinId(), details));
    }
}
