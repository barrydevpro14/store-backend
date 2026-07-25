package org.store.users.application.service;

import org.store.security.domain.model.Account;
import org.store.users.application.dto.UtilisateurRequest;
import org.store.users.domain.model.Proprietaire;

import java.util.Optional;
import java.util.UUID;

public interface IProprietaireService {

    Proprietaire create(UtilisateurRequest utilisateurRequest, Account account);

    /** Finds the OWNER Account for the given enterprise, or empty if none. */
    Optional<Account> findAccountByEntrepriseId(UUID entrepriseId);
}
