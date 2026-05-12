package org.store.entreprise.application.service.impl;

import org.store.entreprise.application.service.*;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.store.common.exceptions.ForbiddenException;
import org.store.entreprise.application.dto.EntrepriseRequest;
import org.store.entreprise.application.dto.EntrepriseResponse;
import org.store.entreprise.domain.model.Entreprise;
import org.store.entreprise.domain.service.EntrepriseDomainService;
import org.store.security.application.dto.UserPrincipal;
import org.store.security.application.service.ICurrentUserService;
import org.store.users.domain.model.Proprietaire;

import java.util.UUID;

@Service
public class EntrepriseServiceImpl implements IEntrepriseService {

    private final EntrepriseDomainService entrepriseDomainService;
    private final ICurrentUserService currentUserService;

    public EntrepriseServiceImpl(EntrepriseDomainService entrepriseDomainService,
                                 ICurrentUserService currentUserService) {
        this.entrepriseDomainService = entrepriseDomainService;
        this.currentUserService = currentUserService;
    }

    @Override
    public Entreprise create(EntrepriseRequest entrepriseRequest, Proprietaire proprietaire) {
        return entrepriseDomainService.create(entrepriseRequest, proprietaire);
    }

    @Override
    public Entreprise findById(UUID id) {
        return entrepriseDomainService.findById(id);
    }

    @Override
    public EntrepriseResponse findCurrentUserEntreprise() {
        UserPrincipal currentUser = currentUserService.getCurrent();
        Entreprise entreprise = entrepriseDomainService.findById(currentUser.entrepriseId());
        return new EntrepriseResponse(entreprise);
    }

    @Override
    @Transactional
    public EntrepriseResponse updateCurrentUserEntreprise(EntrepriseRequest entrepriseRequest) {
        UserPrincipal currentUser = currentUserService.getCurrent();
        Entreprise entreprise = ensureBelongsToCurrentUser(entrepriseDomainService.findById(currentUser.entrepriseId()));
        entreprise.setSigle(entrepriseRequest.sigle());
        entreprise.setRaisonSociale(entrepriseRequest.raisonSociale());
        entreprise.setNinea(entrepriseRequest.ninea());
        entreprise.setRccm(entrepriseRequest.rccm());
        entreprise.setAdresse(entrepriseRequest.adresse());
        return new EntrepriseResponse(entrepriseDomainService.save(entreprise));
    }

    @Override
    public Page<EntrepriseResponse> findAll(Pageable pageable) {
        return entrepriseDomainService.findAllProjected(pageable);
    }

    @Override
    public EntrepriseResponse findResponseById(UUID id) {
        return new EntrepriseResponse(entrepriseDomainService.findById(id));
    }

    @Override
    @Transactional
    public EntrepriseResponse activate(UUID id) {
        Entreprise entreprise = entrepriseDomainService.findById(id);
        entreprise.setActif(true);
        return new EntrepriseResponse(entrepriseDomainService.save(entreprise));
    }

    @Override
    @Transactional
    public EntrepriseResponse deactivate(UUID id) {
        Entreprise entreprise = entrepriseDomainService.findById(id);
        entreprise.setActif(false);
        return new EntrepriseResponse(entrepriseDomainService.save(entreprise));
    }

    @Override
    public Entreprise ensureBelongsToCurrentUser(Entreprise entreprise) {
        UserPrincipal currentUser = currentUserService.getCurrent();
        if (!entreprise.getId().equals(currentUser.entrepriseId())) {
            throw new ForbiddenException("entreprise.notOwned");
        }
        return entreprise;
    }
}
