package org.store.users.application.service;

import org.store.security.domain.model.Account;
import org.store.users.application.dto.UtilisateurRequest;
import org.store.users.domain.model.Proprietaire;

public interface IProprietaireService {

    Proprietaire create(UtilisateurRequest info, Account account);
}
