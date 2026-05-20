package org.store.security.application.service;

import org.store.entreprise.application.dto.EntrepriseResponse;
import org.store.security.application.dto.AccountResponse;
import org.store.security.application.dto.AuthResponse;
import org.store.security.application.dto.RegisterPropertyRequest;
import org.store.security.domain.model.Account;

public interface IRegisterPropertyService {

    /**
     * Self-registration : crée account + proprietaire + entreprise + magasin + abonnement trial,
     * et renvoie des tokens pour que l'utilisateur soit connecté immédiatement.
     */
    AuthResponse register(RegisterPropertyRequest request);

    /**
     * Création pilotée par un ADMIN : même flux que {@link #register} mais renvoie l'account créé
     * (avec les infos du proprietaire) — pas de tokens.
     */
    AccountResponse registerOwnerByAdmin(RegisterPropertyRequest request);

    /**
     * Création pilotée par un ADMIN d'une entreprise (et de son propriétaire) : même flux que
     * {@link #registerOwnerByAdmin} mais renvoie la {@link EntrepriseResponse} de l'entreprise créée.
     */
    EntrepriseResponse registerEntrepriseByAdmin(RegisterPropertyRequest request);

    /**
     * Orchestration commune : crée le compte + le proprietaire + l'entreprise + le premier magasin
     * + l'abonnement trial. Le {@code roleName} est le libellé du rôle à attribuer au compte
     * (ex. "OWNER"). Retourne le compte créé (avec sa chaîne d'entités liées).
     */
    Account createAccount(RegisterPropertyRequest request, String roleName);
}
