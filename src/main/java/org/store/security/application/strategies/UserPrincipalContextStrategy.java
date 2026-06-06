package org.store.security.application.strategies;

import org.store.users.domain.model.Utilisateur;

public interface UserPrincipalContextStrategy {

    Class<? extends Utilisateur> targetType();

    UserPrincipalContext resolve(Utilisateur user);
}
