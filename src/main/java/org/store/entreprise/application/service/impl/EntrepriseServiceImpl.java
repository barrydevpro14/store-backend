package org.store.entreprise.application.service.impl;

import org.store.entreprise.application.service.*;

import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.store.common.dto.ImageDownloadResponse;
import org.store.common.exceptions.EntityException;
import org.store.common.exceptions.ForbiddenException;
import org.store.common.model.PieceJointe;
import org.store.common.service.IUploadFileService;
import org.store.common.service.ValidatorService;
import org.store.entreprise.application.dto.EntrepriseFilter;
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
    private final IUploadFileService uploadFileService;
    private final ICurrentUserService currentUserService;
    private final ValidatorService validatorService;

    public EntrepriseServiceImpl(EntrepriseDomainService entrepriseDomainService,
                                 IUploadFileService uploadFileService,
                                 ICurrentUserService currentUserService,
                                 ValidatorService validatorService) {
        this.entrepriseDomainService = entrepriseDomainService;
        this.uploadFileService = uploadFileService;
        this.currentUserService = currentUserService;
        this.validatorService = validatorService;
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
    public Page<EntrepriseResponse> findAll(EntrepriseFilter filter) {
        validatorService.validate(filter);
        return entrepriseDomainService.findResponsesByFilter(filter);
    }

    @Override
    @Transactional
    public EntrepriseResponse update(java.util.UUID id, EntrepriseRequest request) {
        validatorService.validate(request);
        Entreprise entreprise = entrepriseDomainService.findById(id);
        entreprise.setSigle(request.sigle());
        entreprise.setRaisonSociale(request.raisonSociale());
        entreprise.setNinea(request.ninea());
        entreprise.setRccm(request.rccm());
        entreprise.setAdresse(request.adresse());
        return new EntrepriseResponse(entrepriseDomainService.save(entreprise));
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
    @Transactional
    public EntrepriseResponse uploadCurrentUserLogo(MultipartFile file) {
        Entreprise entreprise = findCurrentEntreprise();
        PieceJointe logo = uploadFileService.buildImage(file);
        Entreprise updated = entrepriseDomainService.setLogo(entreprise, logo);
        return new EntrepriseResponse(updated);
    }

    @Override
    public ImageDownloadResponse getCurrentUserLogo() {
        PieceJointe logo = findCurrentEntreprise().getLogo();
        if (logo == null) {
            throw new EntityException("entreprise.logo.notFound");
        }
        return new ImageDownloadResponse(logo.getDocument(), logo.getContentType());
    }

    @Override
    @Transactional
    public void deleteCurrentUserLogo() {
        Entreprise entreprise = findCurrentEntreprise();
        if (entreprise.getLogo() != null) {
            entrepriseDomainService.clearLogo(entreprise);
        }
    }

    /** Resout l'entreprise du proprietaire connecte via UserPrincipal.entrepriseId. */
    public Entreprise findCurrentEntreprise() {
        UserPrincipal currentUser = currentUserService.getCurrent();
        return ensureBelongsToCurrentUser(entrepriseDomainService.findById(currentUser.entrepriseId()));
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
