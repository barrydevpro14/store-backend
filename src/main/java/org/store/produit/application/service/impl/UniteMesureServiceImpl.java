package org.store.produit.application.service.impl;

import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.store.common.exceptions.UniqueResourceException;
import org.store.produit.application.dto.UniteMesureFilter;
import org.store.produit.application.dto.UniteMesureRequest;
import org.store.produit.application.dto.UniteMesureResponse;
import org.store.produit.application.dto.UniteMesureSummaryResponse;
import org.store.produit.application.service.IUniteMesureService;
import org.store.produit.domain.model.UniteMesure;
import org.store.produit.domain.service.UniteMesureDomainService;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Gère le CRUD des unités de mesure, table de référence globale (non scopée par entreprise).
 */
@Service
@Transactional(readOnly = true)
public class UniteMesureServiceImpl implements IUniteMesureService {

    private final UniteMesureDomainService uniteMesureDomainService;

    public UniteMesureServiceImpl(UniteMesureDomainService uniteMesureDomainService) {
        this.uniteMesureDomainService = uniteMesureDomainService;
    }

    /** Crée une unité de mesure après contrôle d'unicité du code. */
    @Override
    @Transactional
    public UniteMesureResponse create(UniteMesureRequest request) {
        ensureCodeAvailable(request.code());

        return new UniteMesureResponse(uniteMesureDomainService.create(request));
    }

    /** Retourne l'entité ou lève {@code EntityException}. */
    @Override
    public UniteMesure findById(UUID id) {
        return uniteMesureDomainService.findById(id);
    }

    /** Retourne l'unité de mesure en DTO. */
    @Override
    public UniteMesureResponse findResponseById(UUID id) {
        return new UniteMesureResponse(uniteMesureDomainService.findById(id));
    }

    /** Liste paginée et filtrée des unités de mesure. */
    @Override
    public Page<UniteMesureResponse> findAll(UniteMesureFilter filter) {
        return uniteMesureDomainService.findResponsesByFilter(filter);
    }

    /** Liste complète ordonnée par libellé pour les sélecteurs produit. */
    @Override
    public List<UniteMesureSummaryResponse> listAll() {
        return uniteMesureDomainService.findAllOrdered().stream()
                .map(UniteMesureSummaryResponse::new)
                .toList();
    }

    /** Met à jour libellé, symbole et précision. Le code est immuable après création. */
    @Override
    @Transactional
    public UniteMesureResponse update(UUID id, UniteMesureRequest request) {
        UniteMesure unite = uniteMesureDomainService.findById(id);

        unite.setLibelle(request.libelle());
        unite.setSymbole(request.symbole());

        return new UniteMesureResponse(uniteMesureDomainService.save(unite));
    }

    /** Supprime l'unité de mesure. */
    @Override
    @Transactional
    public void delete(UUID id) {
        UniteMesure unite = uniteMesureDomainService.findById(id);
        uniteMesureDomainService.delete(unite);
    }

    /** Retourne l'unité correspondant au code technique ou lève {@code EntityException}. */
    @Override
    public UniteMesure findByCode(String code) {
        return uniteMesureDomainService.findByCode(code);
    }

    /** Retourne l'unité correspondant au code technique, ou {@code Optional.empty()} si absente. */
    @Override
    public Optional<UniteMesure> findByCodeOptional(String code) {
        return uniteMesureDomainService.findByCodeOptional(code);
    }

    /** Lève {@code UniqueResourceException} si le code est déjà utilisé. */
    @Override
    public void ensureCodeAvailable(String code) {
        if (uniteMesureDomainService.existsByCode(code)) {
            throw new UniqueResourceException("uniteMesure.code.alreadyExists", code);
        }
    }
}
