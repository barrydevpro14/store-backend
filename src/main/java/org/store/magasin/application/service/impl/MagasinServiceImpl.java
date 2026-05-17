package org.store.magasin.application.service.impl;

import org.store.magasin.application.service.*;

import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.store.common.exceptions.ForbiddenException;
import org.store.common.service.ValidatorService;
import org.store.entreprise.application.service.IEntrepriseService;
import org.store.entreprise.domain.model.Entreprise;
import org.store.magasin.application.dto.MagasinFilter;
import org.store.magasin.application.dto.MagasinRequest;
import org.store.magasin.application.dto.MagasinResponse;
import org.store.magasin.domain.model.Magasin;
import org.store.magasin.domain.service.MagasinDomainService;
import org.store.security.application.dto.UserPrincipal;
import org.store.security.application.enums.PermissionCode;
import org.store.security.application.service.ICurrentUserService;

import java.util.UUID;

@Service
public class MagasinServiceImpl implements IMagasinService {

    private final MagasinDomainService magasinDomainService;
    private final IEntrepriseService entrepriseService;
    private final ICurrentUserService currentUserService;
    private final ValidatorService validatorService;

    public MagasinServiceImpl(MagasinDomainService magasinDomainService,
                              IEntrepriseService entrepriseService,
                              ICurrentUserService currentUserService,
                              ValidatorService validatorService) {
        this.magasinDomainService = magasinDomainService;
        this.entrepriseService = entrepriseService;
        this.currentUserService = currentUserService;
        this.validatorService = validatorService;
    }

    @Override
    public Magasin create(MagasinRequest magasinRequest, Entreprise entreprise) {
        return magasinDomainService.create(magasinRequest, entreprise);
    }

    @Override
    @Transactional
    public MagasinResponse create(MagasinRequest magasinRequest) {
        UserPrincipal currentUser = currentUserService.getCurrent();
        Entreprise entreprise = entrepriseService.findById(currentUser.entrepriseId());
        Magasin saved = create(magasinRequest, entreprise);
        return new MagasinResponse(saved);
    }

    @Override
    public Magasin findById(UUID id) {
        return magasinDomainService.findById(id);
    }

    @Override
    public MagasinResponse findResponseById(UUID id) {
        Magasin magasin = ensureBelongsToCurrentEntreprise(magasinDomainService.findById(id));
        return new MagasinResponse(magasin);
    }

    @Override
    public Page<MagasinResponse> findAllByCurrentEntreprise(MagasinFilter filter) {
        validatorService.validate(filter);
        UserPrincipal currentUser = currentUserService.getCurrent();
        return magasinDomainService.findResponsesByFilter(filter, currentUser.entrepriseId());
    }

    @Override
    @Transactional
    public MagasinResponse update(UUID id, MagasinRequest magasinRequest) {
        Magasin magasin = ensureBelongsToCurrentEntreprise(magasinDomainService.findById(id));
        magasin.setNom(magasinRequest.nom());
        magasin.setAdresse(magasinRequest.adresse());
        return new MagasinResponse(magasinDomainService.save(magasin));
    }

    @Override
    @Transactional
    public MagasinResponse activate(UUID id) {
        Magasin magasin = ensureBelongsToCurrentEntreprise(magasinDomainService.findById(id));
        magasin.setActif(true);
        return new MagasinResponse(magasinDomainService.save(magasin));
    }

    @Override
    @Transactional
    public MagasinResponse deactivate(UUID id) {
        Magasin magasin = ensureBelongsToCurrentEntreprise(magasinDomainService.findById(id));
        magasin.setActif(false);
        return new MagasinResponse(magasinDomainService.save(magasin));
    }

    @Override
    public Magasin ensureBelongsToCurrentEntreprise(Magasin magasin) {
        UserPrincipal currentUser = currentUserService.getCurrent();
        if (!magasin.getEntreprise().getId().equals(currentUser.entrepriseId())) {
            throw new ForbiddenException("magasin.notOwned");
        }
        return magasin;
    }

    @Override
    public Magasin ensureAccessibleByCurrentUser(Magasin magasin) {
        UserPrincipal currentUser = currentUserService.getCurrent();
        if (currentUser.hasPermission(PermissionCode.PROPRIETAIRE_ACCESS)) {
            return ensureBelongsToCurrentEntreprise(magasin);
        }
        if (!magasin.getId().equals(currentUser.magasinId())) {
            throw new ForbiddenException("magasin.notOwned");
        }
        return magasin;
    }
}
