package org.store.plateforme.application.service.impl;

import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.store.audit.application.event.AuditEvent;
import org.store.audit.application.service.IAuditEventPublisher;
import org.store.audit.domain.enums.AuditAction;
import org.store.audit.domain.enums.AuditEntityType;
import org.store.common.service.ValidatorService;
import org.store.country.domain.model.Country;
import org.store.country.domain.service.CountryDomainService;
import org.store.paiement.application.service.IMoyenPaiementService;
import org.store.paiement.domain.model.MoyenPaiement;
import org.store.plateforme.application.dto.DepensePlateformeFilter;
import org.store.plateforme.application.dto.DepensePlateformeRequest;
import org.store.plateforme.application.dto.DepensePlateformeResponse;
import org.store.plateforme.application.dto.DepensePlateformeTotalResponse;
import org.store.plateforme.application.service.ICategoryDepensePlateformeService;
import org.store.plateforme.application.service.IDepensePlateformeService;
import org.store.plateforme.domain.model.CategoryDepensePlateforme;
import org.store.plateforme.domain.model.DepensePlateforme;
import org.store.plateforme.domain.service.DepensePlateformeDomainService;
import org.store.security.application.dto.UserPrincipal;
import org.store.security.application.service.ICurrentUserService;

import java.math.BigDecimal;
import java.util.UUID;

/** Orchestre le CRUD des dépenses plateforme : résolution FK category/moyen/country, agrégation totale, audit des créations/modifications/suppressions et suppression logique via `actif`. */
@Service
@Transactional(readOnly = true)
public class DepensePlateformeServiceImpl implements IDepensePlateformeService {

    private final DepensePlateformeDomainService domainService;
    private final ICategoryDepensePlateformeService categoryService;
    private final IMoyenPaiementService moyenPaiementService;
    private final CountryDomainService countryDomainService;
    private final ValidatorService validatorService;
    private final IAuditEventPublisher auditEventPublisher;
    private final ICurrentUserService currentUserService;

    public DepensePlateformeServiceImpl(DepensePlateformeDomainService domainService,
                                        ICategoryDepensePlateformeService categoryService,
                                        IMoyenPaiementService moyenPaiementService,
                                        CountryDomainService countryDomainService,
                                        ValidatorService validatorService,
                                        IAuditEventPublisher auditEventPublisher,
                                        ICurrentUserService currentUserService) {
        this.domainService = domainService;
        this.categoryService = categoryService;
        this.moyenPaiementService = moyenPaiementService;
        this.countryDomainService = countryDomainService;
        this.validatorService = validatorService;
        this.auditEventPublisher = auditEventPublisher;
        this.currentUserService = currentUserService;
    }

    /** Publie un événement d'audit non scopé (entrepriseId/magasinId null car référentiel global). */
    private void audit(AuditAction action, UUID entityId, String label) {
        UserPrincipal caller = currentUserService.getCurrent();
        auditEventPublisher.publish(new AuditEvent(action, AuditEntityType.DEPENSE_PLATEFORME, entityId, label,
                caller.accountId().toString(), caller.username(), null, null, null));
    }

    /** Crée la dépense après résolution des FK category/moyen/country et publie l'événement d'audit associé. */
    @Override
    @Transactional
    public DepensePlateformeResponse create(DepensePlateformeRequest request) {
        CategoryDepensePlateforme category = categoryService.findById(request.categoryId());
        MoyenPaiement moyen = moyenPaiementService.findById(request.moyenPaiementId());
        Country country = request.countryId() != null ? countryDomainService.findById(request.countryId()) : null;
        DepensePlateforme created = domainService.create(request, category, moyen, country);
        audit(AuditAction.DEPENSE_PLATEFORME_CREATED, created.getId(), created.getLibelle());
        return new DepensePlateformeResponse(created);
    }

    /** Retourne la dépense correspondant à l'identifiant donné. */
    @Override
    public DepensePlateformeResponse findResponseById(UUID id) {
        return new DepensePlateformeResponse(domainService.findById(id));
    }

    /** Liste paginée des dépenses filtrées ; montre à la fois les lignes actives et désactivées (voir clause `actif` optionnelle). */
    @Override
    public Page<DepensePlateformeResponse> findAll(DepensePlateformeFilter filter) {
        validatorService.validate(filter);
        return domainService.findResponsesByFilter(filter);
    }

    /** Calcule le total/nombre de dépenses filtrées, en excluant systématiquement les lignes désactivées (agrégat P&L). */
    @Override
    public DepensePlateformeTotalResponse computeTotal(DepensePlateformeFilter filter) {
        validatorService.validate(filter);
        return domainService.computeTotal(filter);
    }

    /** Simple period+country sum — consumed by PlateformeReportingServiceImpl, no category/moyen/libelle filters. */
    @Override
    public BigDecimal computeTotal(String startDate, String endDate, UUID countryId) {
        return domainService.sumByPeriod(startDate, endDate, countryId);
    }

    /** Met à jour la dépense après résolution des FK category/moyen/country et publie l'événement d'audit associé. */
    @Override
    @Transactional
    public DepensePlateformeResponse update(UUID id, DepensePlateformeRequest request) {
        DepensePlateforme depense = domainService.findById(id);
        CategoryDepensePlateforme category = categoryService.findById(request.categoryId());
        MoyenPaiement moyen = moyenPaiementService.findById(request.moyenPaiementId());
        Country country = request.countryId() != null ? countryDomainService.findById(request.countryId()) : null;

        depense.setCategory(category);
        depense.setLibelle(request.libelle());
        depense.setDescription(request.description());
        depense.setDateDepense(request.dateDepense());
        depense.setMontant(request.montant());
        depense.setModePaiement(moyen);
        depense.setCountry(country);
        if (request.actif() != null) {
            depense.setActif(request.actif());
        }

        DepensePlateforme saved = domainService.save(depense);
        audit(AuditAction.DEPENSE_PLATEFORME_UPDATED, saved.getId(), saved.getLibelle());
        return new DepensePlateformeResponse(saved);
    }

    /** Désactive la dépense (actif=false) au lieu de supprimer la ligne, puis publie l'événement d'audit associé. */
    @Override
    @Transactional
    public void delete(UUID id) {
        DepensePlateforme depense = domainService.findById(id);
        depense.setActif(false);
        domainService.save(depense);
        audit(AuditAction.DEPENSE_PLATEFORME_DELETED, depense.getId(), depense.getLibelle());
    }
}
