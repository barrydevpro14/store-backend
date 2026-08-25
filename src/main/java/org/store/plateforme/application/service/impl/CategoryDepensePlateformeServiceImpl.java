package org.store.plateforme.application.service.impl;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.store.audit.application.event.AuditEvent;
import org.store.audit.application.service.IAuditEventPublisher;
import org.store.audit.domain.enums.AuditAction;
import org.store.audit.domain.enums.AuditEntityType;
import org.store.common.dto.DataSelect;
import org.store.common.exceptions.UniqueResourceException;
import org.store.plateforme.application.dto.CategoryDepensePlateformeFilter;
import org.store.plateforme.application.dto.CategoryDepensePlateformeRequest;
import org.store.plateforme.application.dto.CategoryDepensePlateformeResponse;
import org.store.plateforme.application.service.ICategoryDepensePlateformeService;
import org.store.plateforme.domain.model.CategoryDepensePlateforme;
import org.store.plateforme.domain.service.CategoryDepensePlateformeDomainService;
import org.store.security.application.dto.UserPrincipal;
import org.store.security.application.service.ICurrentUserService;

import java.util.UUID;

/** CRUD des catégories de dépense plateforme — référentiel global, non scopé par entreprise, avec audit des créations/modifications/suppressions et suppression logique via `actif`. */
@Service
@Transactional(readOnly = true)
public class CategoryDepensePlateformeServiceImpl implements ICategoryDepensePlateformeService {

    private final CategoryDepensePlateformeDomainService domainService;
    private final IAuditEventPublisher auditEventPublisher;
    private final ICurrentUserService currentUserService;

    public CategoryDepensePlateformeServiceImpl(CategoryDepensePlateformeDomainService domainService,
                                                 IAuditEventPublisher auditEventPublisher,
                                                 ICurrentUserService currentUserService) {
        this.domainService = domainService;
        this.auditEventPublisher = auditEventPublisher;
        this.currentUserService = currentUserService;
    }

    /** Publie un événement d'audit non scopé (entrepriseId/magasinId null car référentiel global). */
    private void audit(AuditAction action, UUID entityId, String label) {
        UserPrincipal caller = currentUserService.getCurrent();
        auditEventPublisher.publish(new AuditEvent(action, AuditEntityType.CATEGORY_DEPENSE_PLATEFORME, entityId, label,
                caller.accountId().toString(), caller.username(), null, null, null));
    }

    /** Crée la catégorie après vérification d'unicité du nom (globale) et publie l'événement d'audit associé. */
    @Override
    @Transactional
    public CategoryDepensePlateformeResponse create(CategoryDepensePlateformeRequest request) {
        ensureNomAvailable(request.nom());
        CategoryDepensePlateforme created = domainService.create(request);
        audit(AuditAction.CATEGORY_DEPENSE_PLATEFORME_CREATED, created.getId(), created.getNom());
        return new CategoryDepensePlateformeResponse(created);
    }

    @Override
    public CategoryDepensePlateforme findById(UUID id) {
        return domainService.findById(id);
    }

    @Override
    public CategoryDepensePlateformeResponse findResponseById(UUID id) {
        return new CategoryDepensePlateformeResponse(domainService.findById(id));
    }

    @Override
    public Page<CategoryDepensePlateformeResponse> findAll(CategoryDepensePlateformeFilter filter) {
        return domainService.findResponses(filter);
    }

    @Override
    public Page<DataSelect> findSelectItems(String q, int page, int size) {
        return domainService.findSelectItems(q, PageRequest.of(page, size));
    }

    /** Met à jour la catégorie après contrôle d'unicité du nom (si changé) et publie l'événement d'audit associé. */
    @Override
    @Transactional
    public CategoryDepensePlateformeResponse update(UUID id, CategoryDepensePlateformeRequest request) {
        CategoryDepensePlateforme category = domainService.findById(id);
        if (!category.getNom().equals(request.nom())) {
            ensureNomAvailable(request.nom());
        }
        category.setNom(request.nom());
        category.setDescription(request.description());
        if (request.actif() != null) {
            category.setActif(request.actif());
        }
        CategoryDepensePlateforme saved = domainService.save(category);
        audit(AuditAction.CATEGORY_DEPENSE_PLATEFORME_UPDATED, saved.getId(), saved.getNom());
        return new CategoryDepensePlateformeResponse(saved);
    }

    /** Désactive la catégorie (actif=false) au lieu de supprimer la ligne, puis publie l'événement d'audit associé. */
    @Override
    @Transactional
    public void delete(UUID id) {
        CategoryDepensePlateforme category = domainService.findById(id);
        category.setActif(false);
        domainService.save(category);
        audit(AuditAction.CATEGORY_DEPENSE_PLATEFORME_DELETED, category.getId(), category.getNom());
    }

    /** Lève UniqueResourceException si une catégorie portant ce nom existe déjà. */
    private void ensureNomAvailable(String nom) {
        if (domainService.existsByNom(nom)) {
            throw new UniqueResourceException("categoryDepensePlateforme.nom.alreadyExists", nom);
        }
    }
}
