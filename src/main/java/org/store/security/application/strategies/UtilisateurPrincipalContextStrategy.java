package org.store.security.application.strategies;

import org.springframework.stereotype.Component;
import org.store.users.domain.model.Utilisateur;

@Component
public class UtilisateurPrincipalContextStrategy implements UserPrincipalContextStrategy {

    @Override
    public Class<? extends Utilisateur> targetType() {
        return Utilisateur.class;
    }

    @Override
    public UserPrincipalContext resolve(Utilisateur user) {
        return UserPrincipalContext.empty();
    }
}
