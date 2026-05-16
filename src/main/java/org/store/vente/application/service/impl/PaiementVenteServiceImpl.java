package org.store.vente.application.service.impl;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.store.achat.domain.enums.StatutFacture;
import org.store.common.exceptions.BadArgumentException;
import org.store.common.exceptions.ForbiddenException;
import org.store.common.service.ValidatorService;
import org.store.security.application.dto.UserPrincipal;
import org.store.security.application.service.ICurrentUserService;
import org.store.vente.application.dto.PaiementVenteCreate;
import org.store.vente.application.dto.PaiementVenteRequest;
import org.store.vente.application.dto.PaiementVenteResponse;
import org.store.vente.application.service.IPaiementVenteService;
import org.store.vente.domain.model.FactureClient;
import org.store.vente.domain.model.PaiementVente;
import org.store.vente.domain.service.FactureClientDomainService;
import org.store.vente.domain.service.PaiementVenteDomainService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Gere les paiements client : listing paginé par facture (F-V4) et ajout de paiements
 * supplémentaires sur facture existante (F-V5, paiement échelonné). Les montants vivent
 * uniquement sur FactureClient — depuis V16, plus de propagation sur CommandeVente.
 */
@Service
@Transactional(readOnly = true)
public class PaiementVenteServiceImpl implements IPaiementVenteService {

    private final PaiementVenteDomainService paiementVenteDomainService;
    private final FactureClientDomainService factureClientDomainService;
    private final ICurrentUserService currentUserService;
    private final ValidatorService validatorService;

    public PaiementVenteServiceImpl(PaiementVenteDomainService paiementVenteDomainService,
                                    FactureClientDomainService factureClientDomainService,
                                    ICurrentUserService currentUserService,
                                    ValidatorService validatorService) {
        this.paiementVenteDomainService = paiementVenteDomainService;
        this.factureClientDomainService = factureClientDomainService;
        this.currentUserService = currentUserService;
        this.validatorService = validatorService;
    }

    /** Retourne les paiements de la facture filtres par l'entreprise du caller (page vide si la facture n'appartient pas a l'entreprise). */
    @Override
    public Page<PaiementVenteResponse> findByFactureId(UUID factureId, Pageable pageable) {
        UserPrincipal currentUser = currentUserService.getCurrent();
        return paiementVenteDomainService.findResponsesByFactureId(factureId, currentUser.entrepriseId(), pageable);
    }

    /** Ajoute un paiement sur une facture existante (F-V5 paiement échelonné). Recalcule le statut de la facture automatiquement. */
    @Override
    @Transactional
    public PaiementVenteResponse create(UUID factureId, PaiementVenteRequest paiementVenteRequest) {
        validatorService.validate(paiementVenteRequest);
        UserPrincipal currentUser = currentUserService.getCurrent();

        FactureClient facture = factureClientDomainService.findById(factureId);
        ensureBelongsToCurrentEntreprise(facture, currentUser.entrepriseId());
        ensureNotAlreadyPaid(facture);
        ensureAmountDoesNotExceedRemaining(facture, paiementVenteRequest.montant());

        LocalDate datePaiement = paiementVenteRequest.datePaiement() != null
                ? paiementVenteRequest.datePaiement() : LocalDate.now();
        PaiementVente paiement = paiementVenteDomainService.create(new PaiementVenteCreate(
                facture, paiementVenteRequest.montant(),
                paiementVenteRequest.modePaiementAsEnum(), datePaiement
        ));
        factureClientDomainService.applyPaiement(facture, paiementVenteRequest.montant());

        return new PaiementVenteResponse(paiement);
    }

    /** Vérifie que la facture appartient à l'entreprise du caller (via commande.magasin.entreprise). */
    public void ensureBelongsToCurrentEntreprise(FactureClient facture, UUID entrepriseId) {
        UUID factureEntrepriseId = facture.getCommande().getMagasin().getEntreprise().getId();
        if (!factureEntrepriseId.equals(entrepriseId)) {
            throw new ForbiddenException("commandeVente.notOwned");
        }
    }

    /** Rejette les paiements sur une facture déjà entièrement payée. */
    public void ensureNotAlreadyPaid(FactureClient facture) {
        if (facture.getStatut() == StatutFacture.PAYEE) {
            throw new BadArgumentException("factureClient.alreadyPaid");
        }
    }

    /** Vérifie que (montantPaye actuel + nouveau montant) ne dépasse pas le montantTotal. */
    public void ensureAmountDoesNotExceedRemaining(FactureClient facture, BigDecimal montant) {
        BigDecimal montantPayeActuel = facture.getMontantPaye() != null ? facture.getMontantPaye() : BigDecimal.ZERO;
        BigDecimal restant = facture.getMontantTotal().subtract(montantPayeActuel);
        if (montant.compareTo(restant) > 0) {
            throw new BadArgumentException("paiementVente.exceedsRemainingAmount", montant, restant);
        }
    }
}
