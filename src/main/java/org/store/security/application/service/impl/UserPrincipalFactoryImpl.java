package org.store.security.application.service.impl;

import org.store.security.application.service.IPermissionsService;
import org.store.security.application.service.IUserPrincipalFactory;

import org.springframework.stereotype.Service;
import org.store.security.application.dto.UserPrincipal;
import org.store.security.application.strategies.UserPrincipalContext;
import org.store.security.application.strategies.UserPrincipalContextStrategy;
import org.store.security.domain.model.Account;
import org.store.security.domain.model.Role;
import org.store.users.domain.model.Utilisateur;

import java.util.List;

@Service
public class UserPrincipalFactoryImpl implements IUserPrincipalFactory {

    private final IPermissionsService permissionsService;
    private final List<UserPrincipalContextStrategy> strategies;

    public UserPrincipalFactoryImpl(IPermissionsService permissionsService,
                                    List<UserPrincipalContextStrategy> strategies) {
        this.permissionsService = permissionsService;
        this.strategies = strategies;
    }

    @Override
    public UserPrincipal build(Account account) {
        Utilisateur user = account.getUser();

        UserPrincipalContext context = strategies.stream()
                .filter(s -> s.targetType().isInstance(user))
                .reduce((a, b) -> a.targetType().isAssignableFrom(b.targetType()) ? b : a)
                .map(s -> s.resolve(user))
                .orElseGet(UserPrincipalContext::empty);

        Role role = account.getRole();
        return new UserPrincipal(
                account.getId(),
                user != null ? user.getId() : null,
                context.entrepriseId(),
                context.magasinId(),
                account.getUsername(),
                context.currency(),
                role.getLibelle(),
                permissionsService.findAllByRoleId(role.getId())
        );
    }
}
