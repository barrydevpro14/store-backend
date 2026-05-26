package org.store.security.application.strategies;

import org.springframework.stereotype.Component;
import org.store.magasin.domain.model.Magasin;
import org.store.users.domain.model.Employe;
import org.store.users.domain.model.Utilisateur;

@Component
public class EmployePrincipalContextStrategy implements UserPrincipalContextStrategy {

    @Override
    public Class<? extends Utilisateur> targetType() {
        return Employe.class;
    }

    @Override
    public UserPrincipalContext resolve(Utilisateur user) {
        Employe employe = (Employe) user;
        Magasin magasin = employe.getMagasin();
        if (magasin == null) {
            return UserPrincipalContext.empty();
        }
        return new UserPrincipalContext(magasin.getEntreprise().getId(), magasin.getId());
    }
}
