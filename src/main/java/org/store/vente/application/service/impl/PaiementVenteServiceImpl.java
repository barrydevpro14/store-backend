package org.store.vente.application.service.impl;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.store.security.application.dto.UserPrincipal;
import org.store.security.application.service.ICurrentUserService;
import org.store.vente.application.dto.PaiementVenteResponse;
import org.store.vente.application.service.IPaiementVenteService;
import org.store.vente.domain.service.PaiementVenteDomainService;

import java.util.UUID;

/** Listing paginé des paiements d'une facture, scopé par l'entreprise du caller (sécurité multi-tenant). */
@Service
@Transactional(readOnly = true)
public class PaiementVenteServiceImpl implements IPaiementVenteService {

    private final PaiementVenteDomainService paiementVenteDomainService;
    private final ICurrentUserService currentUserService;

    public PaiementVenteServiceImpl(PaiementVenteDomainService paiementVenteDomainService,
                                    ICurrentUserService currentUserService) {
        this.paiementVenteDomainService = paiementVenteDomainService;
        this.currentUserService = currentUserService;
    }

    /** Retourne les paiements de la facture filtres par l'entreprise du caller (page vide si la facture n'appartient pas a l'entreprise). */
    @Override
    public Page<PaiementVenteResponse> findByFactureId(UUID factureId, Pageable pageable) {
        UserPrincipal currentUser = currentUserService.getCurrent();
        return paiementVenteDomainService.findResponsesByFactureId(factureId, currentUser.entrepriseId(), pageable);
    }
}
