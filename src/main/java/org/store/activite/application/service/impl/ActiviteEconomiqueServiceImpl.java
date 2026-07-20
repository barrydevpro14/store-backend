package org.store.activite.application.service.impl;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.store.activite.application.dto.ActiviteEconomiqueRequest;
import org.store.activite.application.dto.ActiviteEconomiqueResponse;
import org.store.activite.application.dto.ActiviteEconomiqueSummaryResponse;
import org.store.activite.application.service.IActiviteEconomiqueService;
import org.store.activite.domain.model.ActiviteEconomique;
import org.store.activite.domain.service.ActiviteEconomiqueDomainService;
import org.store.common.exceptions.UniqueResourceException;

import java.util.List;
import java.util.UUID;

/**
 * Gère le CRUD des activités économiques (référentiel global, admin-scoped).
 * La suppression est un soft-delete (actif = false) ; activate/deactivate permettent
 * de gérer le cycle de vie sans perte de données.
 */
@Service
public class ActiviteEconomiqueServiceImpl implements IActiviteEconomiqueService {

    private final ActiviteEconomiqueDomainService activiteEconomiqueDomainService;

    public ActiviteEconomiqueServiceImpl(ActiviteEconomiqueDomainService activiteEconomiqueDomainService) {
        this.activiteEconomiqueDomainService = activiteEconomiqueDomainService;
    }

    /** Crée une activité économique après contrôle d'unicité du libellé (parmi les actives). */
    @Override
    @Transactional
    public ActiviteEconomiqueResponse create(ActiviteEconomiqueRequest request) {
        ensureLibelleAvailableForCreate(request.libelle());

        ActiviteEconomique activite = new ActiviteEconomique();
        activite.setLibelle(request.libelle().toLowerCase());
        activite.setDescription(request.description());

        return new ActiviteEconomiqueResponse(activiteEconomiqueDomainService.save(activite));
    }

    /** Retourne l'activité en Response ou lève EntityException. */
    @Override
    public ActiviteEconomiqueResponse findResponseById(UUID id) {
        return new ActiviteEconomiqueResponse(activiteEconomiqueDomainService.findById(id));
    }

    /** Met à jour libellé et description après contrôle d'unicité parmi les actives. */
    @Override
    @Transactional
    public ActiviteEconomiqueResponse update(UUID id, ActiviteEconomiqueRequest request) {
        ActiviteEconomique activite = activiteEconomiqueDomainService.findById(id);

        ensureLibelleAvailableForUpdate(id, request.libelle());

        activite.setLibelle(request.libelle().toLowerCase());
        activite.setDescription(request.description());

        return new ActiviteEconomiqueResponse(activiteEconomiqueDomainService.save(activite));
    }

    /** Active l'activité économique (actif = true). */
    @Override
    @Transactional
    public ActiviteEconomiqueResponse activate(UUID id) {
        ActiviteEconomique activite = activiteEconomiqueDomainService.findById(id);
        activiteEconomiqueDomainService.activate(activite);
        return new ActiviteEconomiqueResponse(activite);
    }

    /** Désactive l'activité économique (soft-delete : actif = false). */
    @Override
    @Transactional
    public ActiviteEconomiqueResponse deactivate(UUID id) {
        ActiviteEconomique activite = activiteEconomiqueDomainService.findById(id);
        activiteEconomiqueDomainService.deactivate(activite);
        return new ActiviteEconomiqueResponse(activite);
    }

    /** Soft-delete : passe actif à false sans supprimer la ligne. */
    @Override
    @Transactional
    public void delete(UUID id) {
        ActiviteEconomique activite = activiteEconomiqueDomainService.findById(id);
        activiteEconomiqueDomainService.deactivate(activite);
    }

    /** Liste toutes les activités (actives et inactives) triées par libellé croissant. */
    @Override
    public List<ActiviteEconomiqueResponse> findAll() {
        return activiteEconomiqueDomainService.findAllOrderByLibelleAsc();
    }

    /** Liste uniquement les activités actives — endpoint public pour les sélecteurs. */
    @Override
    public List<ActiviteEconomiqueSummaryResponse> findAllActive() {
        return activiteEconomiqueDomainService.findAllActiveOrderByLibelleAsc();
    }

    private void ensureLibelleAvailableForCreate(String libelle) {
        if (activiteEconomiqueDomainService.existsByLibelleIgnoreCaseAndActifTrue(libelle)) {
            throw new UniqueResourceException("activiteEconomique.libelle.alreadyExists", libelle);
        }
    }

    private void ensureLibelleAvailableForUpdate(UUID id, String libelle) {
        if (activiteEconomiqueDomainService.existsByLibelleIgnoreCaseAndActifTrueAndIdNot(libelle, id)) {
            throw new UniqueResourceException("activiteEconomique.libelle.alreadyExists", libelle);
        }
    }
}
