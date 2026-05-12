package org.store.security.application.service;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.store.security.application.dto.AuthResponse;
import org.store.security.application.dto.LoginRequest;
import org.store.security.application.dto.UserPrincipal;
import org.store.security.domain.model.Account;

@Service
public class LoginServiceImpl implements ILoginService {

    private final AuthenticationManager authenticationManager;
    private final IAccountService accountService;
    private final IJwtService jwtService;
    private final IUserPrincipalFactory userPrincipalFactory;
    private final IRefreshTokenService refreshTokenService;

    public LoginServiceImpl(AuthenticationManager authenticationManager,
                            IAccountService accountService,
                            IJwtService jwtService,
                            IUserPrincipalFactory userPrincipalFactory,
                            IRefreshTokenService refreshTokenService) {
        this.authenticationManager = authenticationManager;
        this.accountService = accountService;
        this.jwtService = jwtService;
        this.userPrincipalFactory = userPrincipalFactory;
        this.refreshTokenService = refreshTokenService;
    }

    @Override
    @Transactional
    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.username(), request.password())
        );

        Account account = accountService.findByUsername(request.username());

        UserPrincipal principal = userPrincipalFactory.build(account);
        String accessToken = jwtService.generateToken(principal);
        String refreshToken = refreshTokenService.create(account);
        return new AuthResponse(accessToken, refreshToken);
    }
}
