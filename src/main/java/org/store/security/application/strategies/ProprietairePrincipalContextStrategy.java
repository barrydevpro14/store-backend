package org.store.security.application.strategies;

import org.springframework.stereotype.Component;
import org.store.entreprise.domain.model.Entreprise;
import org.store.magasin.domain.model.Magasin;
import org.store.users.domain.model.Proprietaire;
import org.store.users.domain.model.Utilisateur;

import java.util.List;
import java.util.UUID;

@Component
public class ProprietairePrincipalContextStrategy implements UserPrincipalContextStrategy {

    @Override
    public Class<? extends Utilisateur> targetType() {
        return Proprietaire.class;
    }

    @Override
    public UserPrincipalContext resolve(Utilisateur user) {
        Proprietaire proprietaire = (Proprietaire) user;
        Entreprise entreprise = proprietaire.getEntreprise();
        if (entreprise == null) {
            return UserPrincipalContext.empty();
        }

        return new UserPrincipalContext(entreprise.getId(), null);
    }
}
