package org.store.paiement.application.service.impl;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.store.common.exceptions.BadArgumentException;
import org.store.common.service.ValidatorService;
import org.store.paiement.application.dto.MoyenPaiementRequest;
import org.store.paiement.application.dto.MoyenPaiementResponse;
import org.store.paiement.application.service.IMoyenPaiementService;
import org.store.paiement.domain.model.MoyenPaiement;
import org.store.paiement.domain.service.MoyenPaiementDomainService;

import java.util.List;
import java.util.UUID;

/**
 * Gère le CRUD des moyens de paiement globaux (ADMIN uniquement).
 */
@Service
@Transactional(readOnly = true)
public class MoyenPaiementServiceImpl implements IMoyenPaiementService {

    private final MoyenPaiementDomainService domainService;
    private final ValidatorService validatorService;

    public MoyenPaiementServiceImpl(MoyenPaiementDomainService domainService,
                                    ValidatorService validatorService) {
        this.domainService = domainService;
        this.validatorService = validatorService;
    }

    /** Retourne tous les moyens de paiement (actifs et inactifs). */
    @Override
    public List<MoyenPaiementResponse> findAll() {
        return domainService.findAll().stream()
                .map(MoyenPaiementResponse::new)
                .toList();
    }

    /** Retourne l'entité par id — utilisée par les autres services pour résoudre l'UUID. */
    @Override
    public MoyenPaiement findById(UUID id) {
        return domainService.findById(id);
    }

    /** Crée un nouveau moyen de paiement après vérification d'unicité du libellé. */
    @Override
    @Transactional
    public MoyenPaiementResponse create(MoyenPaiementRequest request) {
        validatorService.validate(request);
        ensureLibelleUnique(request.libelle(), null);

        MoyenPaiement moyen = new MoyenPaiement();
        moyen.setLibelle(request.libelle());
        moyen.setCode(request.libelle().toUpperCase().replaceAll("[^A-Z0-9]", "_"));

        return new MoyenPaiementResponse(domainService.save(moyen));
    }

    /** Met à jour le libellé d'un moyen de paiement existant. */
    @Override
    @Transactional
    public MoyenPaiementResponse update(UUID id, MoyenPaiementRequest request) {
        validatorService.validate(request);
        MoyenPaiement moyen = domainService.findById(id);
        ensureLibelleUnique(request.libelle(), id);

        moyen.setLibelle(request.libelle());

        return new MoyenPaiementResponse(domainService.save(moyen));
    }

    /** Active un moyen de paiement désactivé. */
    @Override
    @Transactional
    public MoyenPaiementResponse activate(UUID id) {
        MoyenPaiement moyen = domainService.findById(id);
        moyen.setActif(true);
        return new MoyenPaiementResponse(domainService.save(moyen));
    }

    /** Désactive un moyen de paiement (soft-disable — garde l'historique). */
    @Override
    @Transactional
    public MoyenPaiementResponse deactivate(UUID id) {
        MoyenPaiement moyen = domainService.findById(id);
        moyen.setActif(false);
        return new MoyenPaiementResponse(domainService.save(moyen));
    }

    /** Supprime définitivement un moyen de paiement. */
    @Override
    @Transactional
    public void delete(UUID id) {
        domainService.delete(domainService.findById(id));
    }

    private void ensureLibelleUnique(String libelle, UUID excludeId) {
        boolean conflict = domainService.findAll().stream()
                .anyMatch(m -> m.getLibelle().equalsIgnoreCase(libelle)
                        && (excludeId == null || !m.getId().equals(excludeId)));
        if (conflict) {
            throw new BadArgumentException("moyenPaiement.libelle.alreadyExists", libelle);
        }
    }
}
