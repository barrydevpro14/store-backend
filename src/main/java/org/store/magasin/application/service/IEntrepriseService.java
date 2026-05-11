package org.store.magasin.application.service;

import org.store.magasin.application.dto.EntrepriseRequest;
import org.store.magasin.domain.model.Entreprise;
import org.store.users.domain.model.Proprietaire;

public interface IEntrepriseService {

    Entreprise create(EntrepriseRequest info, Proprietaire proprietaire);
}
