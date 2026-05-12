package org.store.security.application.service;

import org.store.entreprise.application.dto.EntrepriseResponse;
import org.store.security.application.dto.AuthResponse;
import org.store.security.application.dto.RegisterPropertyRequest;

public interface IRegisterPropertyService {

    /**
     * Self-registration : crée account + proprietaire + entreprise + magasin + abonnement trial,
     * et renvoie des tokens pour que l'utilisateur soit connecté immédiatement.
     */
    AuthResponse register(RegisterPropertyRequest request);

    /**
     * Création pilotée par un ADMIN : même flux que {@link #register} mais retourne uniquement
     * l'entreprise créée (pas de tokens — l'ADMIN ne se connecte pas à la place du proprietaire).
     */
    EntrepriseResponse adminCreate(RegisterPropertyRequest request);
}
