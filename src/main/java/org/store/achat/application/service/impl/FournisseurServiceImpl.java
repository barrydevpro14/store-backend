package org.store.achat.application.service.impl;

import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.store.achat.application.dto.FournisseurFilter;
import org.store.achat.application.dto.FournisseurRequest;
import org.store.achat.application.dto.FournisseurResponse;
import org.store.achat.application.service.IFournisseurService;
import org.store.achat.domain.model.Fournisseur;
import org.store.achat.domain.service.FournisseurDomainService;
import org.store.common.exceptions.ForbiddenException;
import org.store.common.exceptions.UniqueResourceException;
import org.store.entreprise.application.service.IEntrepriseService;
import org.store.entreprise.domain.model.Entreprise;
import org.store.security.application.dto.UserPrincipal;
import org.store.security.application.service.ICurrentUserService;

import java.util.UUID;

/**
 * Gère le CRUD des fournisseurs, scopé sur l'entreprise de l'utilisateur courant.
 */
@Service
public class FournisseurServiceImpl implements IFournisseurService {

    private final FournisseurDomainService fournisseurDomainService;
    private final IEntrepriseService entrepriseService;
    private final ICurrentUserService currentUserService;

    public FournisseurServiceImpl(FournisseurDomainService fournisseurDomainService,
                                  IEntrepriseService entrepriseService,
                                  ICurrentUserService currentUserService) {
        this.fournisseurDomainService = fournisseurDomainService;
        this.entrepriseService = entrepriseService;
        this.currentUserService = currentUserService;
    }

    /** Crée un fournisseur pour l'entreprise du caller après contrôle d'unicité de la référence. */
    @Override
    @Transactional
    public FournisseurResponse create(FournisseurRequest fournisseurRequest) {
        UUID entrepriseId = currentUserService.getCurrent().entrepriseId();
        ensureReferenceAvailable(fournisseurRequest.reference(), entrepriseId);
        Entreprise entreprise = entrepriseService.findById(entrepriseId);
        return new FournisseurResponse(fournisseurDomainService.create(fournisseurRequest, entreprise));
    }

    /** Retourne le fournisseur ou lève `EntityException`. */
    @Override
    public Fournisseur findById(UUID id) {
        return fournisseurDomainService.findById(id);
    }

    /** Retourne le fournisseur en `Response` après vérification de l'appartenance à l'entreprise du caller. */
    @Override
    public FournisseurResponse findResponseById(UUID id) {
        Fournisseur fournisseur = ensureBelongsToCurrentEntreprise(fournisseurDomainService.findById(id));
        return new FournisseurResponse(fournisseur);
    }

    /** Liste paginée + filtrée des fournisseurs de l'entreprise du caller. */
    @Override
    public Page<FournisseurResponse> findAll(FournisseurFilter filter) {
        UUID entrepriseId = currentUserService.getCurrent().entrepriseId();
        return fournisseurDomainService.findResponsesByFilter(filter, entrepriseId);
    }

    /** Met à jour le fournisseur après contrôle d'appartenance et d'unicité de référence si modifiée. */
    @Override
    @Transactional
    public FournisseurResponse update(UUID id, FournisseurRequest fournisseurRequest) {
        Fournisseur fournisseur = ensureBelongsToCurrentEntreprise(fournisseurDomainService.findById(id));
        if (!java.util.Objects.equals(fournisseur.getReference(), fournisseurRequest.reference())) {
            ensureReferenceAvailable(fournisseurRequest.reference(), fournisseur.getEntreprise().getId());
        }
        fournisseur.setNom(fournisseurRequest.nom());
        fournisseur.setPrenom(fournisseurRequest.prenom());
        fournisseur.setEmail(fournisseurRequest.email());
        fournisseur.setTelephone(fournisseurRequest.telephone());
        fournisseur.setAdresse(fournisseurRequest.adresse());
        fournisseur.setReference(fournisseurRequest.reference());
        fournisseur.setOrigine(fournisseurRequest.origine());
        return new FournisseurResponse(fournisseurDomainService.save(fournisseur));
    }

    /** Supprime le fournisseur après contrôle d'appartenance à l'entreprise du caller. */
    @Override
    @Transactional
    public void delete(UUID id) {
        Fournisseur fournisseur = ensureBelongsToCurrentEntreprise(fournisseurDomainService.findById(id));
        fournisseurDomainService.delete(fournisseur);
    }

    /** Lève `ForbiddenException` si le fournisseur n'appartient pas à l'entreprise du caller. */
    @Override
    public Fournisseur ensureBelongsToCurrentEntreprise(Fournisseur fournisseur) {
        UserPrincipal currentUser = currentUserService.getCurrent();
        if (!fournisseur.getEntreprise().getId().equals(currentUser.entrepriseId())) {
            throw new ForbiddenException("fournisseur.notOwned");
        }
        return fournisseur;
    }

    /** Lève `UniqueResourceException` si la référence est déjà utilisée dans l'entreprise (skippé si null/blank). */
    @Override
    public void ensureReferenceAvailable(String reference, UUID entrepriseId) {
        if (reference == null || reference.isBlank()) {
            return;
        }
        if (fournisseurDomainService.existsByReferenceAndEntrepriseId(reference, entrepriseId)) {
            throw new UniqueResourceException("fournisseur.reference.alreadyExists", reference);
        }
    }
}
