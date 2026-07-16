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
 */
@Service
public class ActiviteEconomiqueServiceImpl implements IActiviteEconomiqueService {

    private final ActiviteEconomiqueDomainService activiteEconomiqueDomainService;

    public ActiviteEconomiqueServiceImpl(ActiviteEconomiqueDomainService activiteEconomiqueDomainService) {
        this.activiteEconomiqueDomainService = activiteEconomiqueDomainService;
    }

    /** Crée une activité économique après contrôle d'unicité du libellé. */
    @Override
    @Transactional
    public ActiviteEconomiqueResponse create(ActiviteEconomiqueRequest request) {
        ensureLibelleAvailable(request.libelle());

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

    /** Met à jour libellé et description après contrôle d'unicité. */
    @Override
    @Transactional
    public ActiviteEconomiqueResponse update(UUID id, ActiviteEconomiqueRequest request) {
        ActiviteEconomique activite = activiteEconomiqueDomainService.findById(id);

        String normalised = request.libelle().toLowerCase();
        if (!activite.getLibelle().equals(normalised)) {
            ensureLibelleAvailable(request.libelle());
        }

        activite.setLibelle(normalised);
        activite.setDescription(request.description());

        return new ActiviteEconomiqueResponse(activiteEconomiqueDomainService.save(activite));
    }

    /** Supprime l'activité ou lève EntityException. */
    @Override
    @Transactional
    public void delete(UUID id) {
        activiteEconomiqueDomainService.deleteById(id);
    }

    /** Liste toutes les activités triées par libellé croissant. */
    @Override
    public List<ActiviteEconomiqueSummaryResponse> findAll() {
        return activiteEconomiqueDomainService.findAllOrderByLibelleAsc();
    }

    /** Lève UniqueResourceException si le libellé est déjà utilisé. */
    private void ensureLibelleAvailable(String libelle) {
        if (activiteEconomiqueDomainService.existsByLibelle(libelle.toLowerCase())) {
            throw new UniqueResourceException("activiteEconomique.libelle.alreadyExists", libelle);
        }
    }
}
