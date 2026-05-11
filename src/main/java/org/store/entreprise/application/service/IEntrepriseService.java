package org.store.entreprise.application.service;

import org.store.entreprise.application.dto.EntrepriseRequest;
import org.store.entreprise.domain.model.Entreprise;
import org.store.users.domain.model.Proprietaire;

public interface IEntrepriseService {

    Entreprise create(EntrepriseRequest entrepriseRequest, Proprietaire proprietaire);
}
