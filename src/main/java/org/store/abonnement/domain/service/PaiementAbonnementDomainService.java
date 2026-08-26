package org.store.abonnement.domain.service;

import org.springframework.data.domain.Page;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Service;
import org.store.abonnement.application.dto.*;
import org.store.abonnement.application.service.impl.FactureGenereeCommand;
import org.store.abonnement.application.service.impl.PaiementAbonnementCreationContext;
import org.store.abonnement.application.service.impl.SubscriptionAmountInputs;
import org.store.abonnement.domain.enums.StatutPaiementAbonnement;
import org.store.abonnement.domain.model.Abonnement;
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
        // Motif rejet is now stored in the preuve_paiement table; not stored here anymore.
        return save(paiement);
    }

    /** Compte les paiements dans un statut donné. */
    public long countByStatut(StatutPaiementAbonnement statut) {
        return repository.countByStatut(statut);
    }

    /** Counts payments matching an optional statut and optional createdAt date range. */
    public long countByStatutAndCreatedBetween(StatutPaiementAbonnement statut, LocalDate startDate, LocalDate endDate) {
        return repository.countByStatutAndCreatedBetween(statut, startDate, endDate);
    }

    /** Sums montantFinal of VALIDE payments whose dateEcheance falls within the given period. */
    public BigDecimal sumValidatedRevenueForPeriod(LocalDate startDate, LocalDate endDate) {
        return repository.sumValidatedRevenueForPeriod(startDate, endDate);
    }

    /** Somme le montantFinal des paiements VALIDE dont la datePaiement tombe dans l'année donnée. */
    public BigDecimal sumValidatedRevenueForYear(int year) {
        LocalDate startOfYear = LocalDate.of(year, 1, 1);
        LocalDate startOfNextYear = LocalDate.of(year + 1, 1, 1);
        return repository.sumValidatedRevenueForYear(startOfYear, startOfNextYear);
    }

    public PaiementAbonnementStatsResponse getStatistiquesPaiement(String startDate, String endDate){
        return repository.getStatistiquesPaiement(startDate, endDate);
    }

    /** Creates a FACTURE_GENEREE payment — stores tarif + coupon snapshots, no moyen/preuve/datePaiement yet. */
    public PaiementAbonnement createFactureGeneree(FactureGenereeCommand command) {
        PaiementAbonnement paiement = new PaiementAbonnement();
        paiement.setAbonnement(command.abonnement());
        paiement.setPlanAbonnementTarif(command.tarif());
        paiement.setCoupon(command.coupon());
        paiement.setMontantAvantReduction(command.breakdown().prixDeBase());
        paiement.setReduction(command.breakdown().reductionCoupon());
        paiement.setMontantFinal(command.breakdown().montantAPayer());
        paiement.setDateEcheance(command.dateEcheance());
        paiement.setStatut(StatutPaiementAbonnement.FACTURE_GENEREE);
        return save(paiement);
    }

    /** Marks a payment as EN_RETARD (overdue). */
    public PaiementAbonnement markAsEnRetard(PaiementAbonnement paiement) {
        paiement.setStatut(StatutPaiementAbonnement.EN_RETARD);
        return save(paiement);
    }

    /** Returns all overdue invoices (FACTURE_GENEREE or EN_ATTENTE_VALIDATION with dateEcheance < today). */
    public List<PaiementAbonnement> findOverdueInvoices(LocalDate today) {
        return repository.findOverdueInvoices(today);
    }

    /** Returns FACTURE_GENEREE invoices due on any of the given alert dates (for daily reminder scheduler). */
    public List<PaiementAbonnement> findFacturesAbonnementDues(List<LocalDate> dates) {
        return repository.findFacturesAbonnementDues(dates);
    }

    /** Returns the most recent FACTURE_GENEREE or EN_RETARD invoice for the abonnement, or empty. */
    public Optional<PaiementAbonnement> findFactureNonPayeeByAbonnement(UUID abonnementId) {
        List<PaiementAbonnement> results = repository.findFacturesNonPayeesByAbonnement(abonnementId, PageRequest.of(0, 1));
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }

    /** Recalculates amounts on an existing unpaid invoice after a plan/periodicite change. */
    public PaiementAbonnement recalculer(PaiementAbonnement paiement, SubscriptionAmountInputs inputs, SubscriptionAmountBreakdown breakdown) {
        paiement.setPlanAbonnementTarif(inputs.tarif());
        paiement.setCoupon(inputs.coupon());
        paiement.setMontantAvantReduction(breakdown.prixDeBase());
        paiement.setReduction(breakdown.reductionCoupon());
        paiement.setMontantFinal(breakdown.montantAPayer());
        return save(paiement);
    }
}
