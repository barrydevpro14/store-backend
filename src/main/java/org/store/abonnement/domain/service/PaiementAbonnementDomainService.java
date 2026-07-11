package org.store.abonnement.domain.service;

import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.store.abonnement.application.dto.PaiementAbonnementFilter;
import org.store.abonnement.application.dto.PaiementAbonnementRequest;
import org.store.abonnement.application.dto.PaiementAbonnementResponse;
import org.store.abonnement.application.dto.SubscriptionAmountBreakdown;
import org.store.abonnement.application.service.impl.PaiementAbonnementCreationContext;
import org.store.abonnement.domain.enums.StatutPaiementAbonnement;
import org.store.abonnement.domain.model.PaiementAbonnement;
import org.store.abonnement.domain.repository.PaiementAbonnementRepository;
import org.store.common.service.GlobalService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.PageRequest;

@Service
public class PaiementAbonnementDomainService extends GlobalService<PaiementAbonnement, PaiementAbonnementRepository> {
    public PaiementAbonnementDomainService(PaiementAbonnementRepository repository) {
        super(repository);
    }

    public PaiementAbonnement createPending(PaiementAbonnementCreationContext context) {
        PaiementAbonnementRequest paiementAbonnementRequest = context.paiementAbonnementRequest();
        SubscriptionAmountBreakdown breakdown = context.breakdown();

        PaiementAbonnement paiement = new PaiementAbonnement();
        paiement.setAbonnement(context.abonnement());
        paiement.setMontantAvantReduction(breakdown.prixDeBase());
        paiement.setReduction(breakdown.reductionType()
                .add(breakdown.reductionPromotion())
                .add(breakdown.reductionCoupon()));
        paiement.setMontantFinal(breakdown.montantAPayer());
        paiement.setDatePaiement(paiementAbonnementRequest.datePaiement());
        paiement.setMoyen(context.moyen());
        paiement.setReferenceTransaction(paiementAbonnementRequest.referenceTransaction());
        paiement.setStatut(StatutPaiementAbonnement.EN_ATTENTE_VALIDATION);
        paiement.setPreuve(context.preuve());
        return save(paiement);
    }

    public boolean existsPendingForAbonnement(UUID abonnementId) {
        return repository.existsByAbonnementIdAndStatut(abonnementId, StatutPaiementAbonnement.EN_ATTENTE_VALIDATION);
    }

    public Page<PaiementAbonnementResponse> findResponses(PaiementAbonnementFilter filter) {
        return repository.findResponsesByFilter(filter.statutAsEnum(), filter.abonnementId(), filter.entrepriseId(), filter.startDate(), filter.endDate(), filter.toPageable());
    }

    /**
     * Returns the entreprise's pending Paiement (statut EN_ATTENTE_VALIDATION on the EN_ATTENTE
     * Abonnement) projected as a response, or empty when nothing has been submitted yet.
     */
    public Optional<PaiementAbonnementResponse> findPendingResponseByEntreprise(UUID entrepriseId) {
        List<PaiementAbonnementResponse> rows = repository.findPendingResponsesByEntreprise(entrepriseId, PageRequest.of(0, 1));
        return rows.isEmpty() ? Optional.empty() : Optional.of(rows.get(0));
    }

    public PaiementAbonnement markAsValide(PaiementAbonnement paiement) {
        paiement.setStatut(StatutPaiementAbonnement.VALIDE);
        return save(paiement);
    }

    public PaiementAbonnement markAsRejete(PaiementAbonnement paiement, String motifRejet) {
        paiement.setStatut(StatutPaiementAbonnement.REJETE);
        paiement.setMotifRejet(motifRejet);
        return save(paiement);
    }

    /** Compte les paiements dans un statut donné. */
    public long countByStatut(StatutPaiementAbonnement statut) {
        return repository.countByStatut(statut);
    }

    /** Counts payments matching an optional statut and optional createdAt date range. */
    public long countByStatutAndCreatedBetween(StatutPaiementAbonnement statut, String startDate, String endDate) {
        return repository.countByStatutAndCreatedBetween(statut, startDate, endDate);
    }

    /** Somme le montantFinal des paiements VALIDE dont la datePaiement tombe dans l'année donnée. */
    public BigDecimal sumValidatedRevenueForYear(int year) {
        LocalDate startOfYear = LocalDate.of(year, 1, 1);
        LocalDate startOfNextYear = LocalDate.of(year + 1, 1, 1);
        return repository.sumValidatedRevenueForYear(startOfYear, startOfNextYear);
    }
}
