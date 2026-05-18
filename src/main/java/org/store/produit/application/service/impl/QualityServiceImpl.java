package org.store.produit.application.service.impl;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.store.common.exceptions.ForbiddenException;
import org.store.common.exceptions.UniqueResourceException;
import org.store.entreprise.application.service.IEntrepriseService;
import org.store.entreprise.domain.model.Entreprise;
import org.store.produit.application.dto.QualityRequest;
import org.store.produit.application.dto.QualityResponse;
import org.store.produit.application.service.IQualityService;
import org.store.produit.domain.model.Quality;
import org.store.produit.domain.service.QualityDomainService;
import org.store.security.application.dto.UserPrincipal;
import org.store.security.application.service.ICurrentUserService;

import java.util.UUID;

/**
 * Gère le CRUD des qualités de produit, scopé sur l'entreprise de l'utilisateur courant.
 */
@Service
public class QualityServiceImpl implements IQualityService {

    private final QualityDomainService qualityDomainService;
    private final IEntrepriseService entrepriseService;
    private final ICurrentUserService currentUserService;

    public QualityServiceImpl(QualityDomainService qualityDomainService,
                              IEntrepriseService entrepriseService,
                              ICurrentUserService currentUserService) {
        this.qualityDomainService = qualityDomainService;
        this.entrepriseService = entrepriseService;
        this.currentUserService = currentUserService;
    }

    /** Crée une qualité pour l'entreprise du caller après contrôle d'unicité du libellé. */
    @Override
    @Transactional
    public QualityResponse create(QualityRequest qualityRequest) {
        UUID entrepriseId = currentUserService.getCurrent().entrepriseId();
        ensureLibelleAvailable(qualityRequest.libelle(), entrepriseId);
        Entreprise entreprise = entrepriseService.findById(entrepriseId);
        return new QualityResponse(qualityDomainService.create(qualityRequest, entreprise));
    }

    /** Retourne la qualité ou lève `EntityException`. */
    @Override
    public Quality findById(UUID id) {
        return qualityDomainService.findById(id);
    }

    /** Retourne la qualité en `Response` après vérification de l'appartenance à l'entreprise du caller. */
    @Override
    public QualityResponse findResponseById(UUID id) {
        Quality quality = ensureBelongsToCurrentEntreprise(qualityDomainService.findById(id));
        return new QualityResponse(quality);
    }

    /** Liste paginée des qualités de l'entreprise du caller. */
    @Override
    public Page<QualityResponse> findAllByCurrentEntreprise(Pageable pageable) {
        UUID entrepriseId = currentUserService.getCurrent().entrepriseId();
        return qualityDomainService.findResponsesByEntrepriseId(entrepriseId, pageable);
    }

    /** Met à jour libellé et description après contrôle d'appartenance et d'unicité. */
    @Override
    @Transactional
    public QualityResponse update(UUID id, QualityRequest qualityRequest) {
        Quality quality = ensureBelongsToCurrentEntreprise(qualityDomainService.findById(id));
        if (!quality.getLibelle().equals(qualityRequest.libelle())) {
            ensureLibelleAvailable(qualityRequest.libelle(), quality.getEntreprise().getId());
        }
        quality.setLibelle(qualityRequest.libelle());
        quality.setDescription(qualityRequest.description());
        return new QualityResponse(qualityDomainService.save(quality));
    }

    /** Supprime la qualité après contrôle d'appartenance à l'entreprise du caller. */
    @Override
    @Transactional
    public void delete(UUID id) {
        Quality quality = ensureBelongsToCurrentEntreprise(qualityDomainService.findById(id));
        qualityDomainService.delete(quality);
    }

    /** Lève `ForbiddenException` si la qualité n'appartient pas à l'entreprise du caller. */
    @Override
    public Quality ensureBelongsToCurrentEntreprise(Quality quality) {
        UserPrincipal currentUser = currentUserService.getCurrent();
        if (!quality.getEntreprise().getId().equals(currentUser.entrepriseId())) {
            throw new ForbiddenException("quality.notOwned");
        }
        return quality;
    }

    /** Lève `UniqueResourceException` si le libellé est déjà utilisé dans l'entreprise. */
    @Override
    public void ensureLibelleAvailable(String libelle, UUID entrepriseId) {
        if (qualityDomainService.existsByLibelleAndEntrepriseId(libelle, entrepriseId)) {
            throw new UniqueResourceException("quality.libelle.alreadyExists", libelle);
        }
    }
}
