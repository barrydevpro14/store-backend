package org.store.magasin.application.service.impl;

import org.store.magasin.application.dto.MagasinSummaryResponse;
import org.store.magasin.application.service.IMagasinService;

import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.store.common.dto.ImageDownloadResponse;
import org.store.common.exceptions.EntityException;
import org.store.common.exceptions.ForbiddenException;
import org.store.common.tools.OwnershipHelper;
import org.store.common.model.PieceJointe;
import org.store.common.service.IUploadFileService;
import org.store.common.service.ValidatorService;
import org.store.entreprise.application.service.IEntrepriseService;
import org.store.entreprise.domain.model.Entreprise;
import org.store.magasin.application.dto.MagasinCountResponse;
import org.store.magasin.application.dto.MagasinFilter;
import org.store.magasin.application.dto.MagasinRequest;
import org.store.magasin.application.dto.MagasinResponse;
import org.store.magasin.domain.model.Magasin;
import org.store.magasin.domain.service.MagasinDomainService;
import org.store.security.application.dto.UserPrincipal;
import org.store.security.application.enums.PermissionCode;
import org.store.abonnement.application.service.AbonnementQuotaService;
import org.store.security.application.service.ICurrentUserService;

import java.util.List;
import java.util.UUID;

/** Manages the full lifecycle of a Magasin (CRUD, logo upload, access-control checks) scoped to the current user's entreprise. */
@Service
public class MagasinServiceImpl implements IMagasinService {

    private final MagasinDomainService magasinDomainService;
    private final IEntrepriseService entrepriseService;
    private final IUploadFileService uploadFileService;
    private final ICurrentUserService currentUserService;
    private final ValidatorService validatorService;
    private final AbonnementQuotaService quotaService;

    public MagasinServiceImpl(MagasinDomainService magasinDomainService,
                              IEntrepriseService entrepriseService,
                              IUploadFileService uploadFileService,
                              ICurrentUserService currentUserService,
                              ValidatorService validatorService,
                              AbonnementQuotaService quotaService) {
        this.magasinDomainService = magasinDomainService;
        this.entrepriseService = entrepriseService;
        this.uploadFileService = uploadFileService;
        this.currentUserService = currentUserService;
        this.validatorService = validatorService;
        this.quotaService = quotaService;
    }

    @Override
    public Magasin create(MagasinRequest magasinRequest, Entreprise entreprise) {
        return magasinDomainService.create(magasinRequest, entreprise);
    }

    @Override
    @Transactional
    public MagasinResponse create(MagasinRequest magasinRequest) {
        UserPrincipal currentUser = currentUserService.getCurrent();
        quotaService.ensureMagasinQuota(currentUser.entrepriseId());
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
        Magasin magasin = ensureAccessibleByCurrentUser(magasinDomainService.findById(id));
        return new MagasinResponse(magasin);
    }

    @Override
    public MagasinSummaryResponse findEmployeById(UUID id) {
        Magasin magasin = ensureAccessibleByCurrentUser(magasinDomainService.findById(id));
        return new MagasinSummaryResponse(magasin);
    }

    @Override
    public Page<MagasinResponse> findAllByCurrentEntreprise(MagasinFilter filter) {
        validatorService.validate(filter);
        UserPrincipal currentUser = currentUserService.getCurrent();
        return magasinDomainService.findResponsesByFilter(filter, currentUser.entrepriseId());
    }

    @Override
    public List<MagasinSummaryResponse> findAllByCurrentEntreprise() {
        return magasinDomainService.findAllByEntreprise(currentUserService.getCurrent().entrepriseId() , true);
    }

    @Override
    public MagasinCountResponse countByCurrentEntreprise() {
        return magasinDomainService.countStatsByEntrepriseId(currentUserService.getCurrent().entrepriseId());
    }

    @Override
    @Transactional
    public MagasinResponse update(UUID id, MagasinRequest magasinRequest) {
        Magasin magasin = ensureBelongsToCurrentEntreprise(magasinDomainService.findById(id));
        magasin.setNom(magasinRequest.nom());
        magasin.setAdresse(magasinRequest.adresse());
        magasin.setTelephone(magasinRequest.telephone());
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
        return OwnershipHelper.ensureOwnership(
                magasin,
                magasin.getEntreprise().getId(),
                currentUserService.getCurrent().entrepriseId(),
                "magasin.notOwned"
        );
    }

    @Override
    @Transactional
    public MagasinResponse uploadLogo(UUID id, MultipartFile file) {
        Magasin magasin = ensureBelongsToCurrentEntreprise(magasinDomainService.findById(id));
        PieceJointe logo = uploadFileService.buildImage(file);
        Magasin updated = magasinDomainService.setLogo(magasin, logo);
        return new MagasinResponse(updated);
    }

    @Override
    @Transactional(readOnly = true)
    public ImageDownloadResponse getLogo(UUID id) {
        Magasin magasin = ensureAccessibleByCurrentUser(magasinDomainService.findById(id));
        PieceJointe logo = magasin.getLogo();
        if (logo == null) {
            throw new EntityException("magasin.logo.notFound");
        }
        return new ImageDownloadResponse(logo.getDocument(), logo.getContentType());
    }

    @Override
    @Transactional
    public void deleteLogo(UUID id) {
        Magasin magasin = ensureBelongsToCurrentEntreprise(magasinDomainService.findById(id));
        if (magasin.getLogo() != null) {
            magasinDomainService.clearLogo(magasin);
        }
    }

    @Override
    public Magasin ensureAccessibleByCurrentUser(Magasin magasin) {
        UserPrincipal currentUser = currentUserService.getCurrent();
        if (currentUser.hasPermission(PermissionCode.OWNER_ACCESS)) {
            return ensureBelongsToCurrentEntreprise(magasin);
        }
        if (!magasin.getId().equals(currentUser.magasinId())) {
            throw new ForbiddenException("magasin.notOwned");
        }
        return magasin;
    }
}
