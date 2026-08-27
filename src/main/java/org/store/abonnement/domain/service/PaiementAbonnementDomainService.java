package org.store.abonnement.domain.service;

import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.store.abonnement.application.dto.*;
import org.store.abonnement.application.service.impl.FactureGenereeCommand;
import org.store.abonnement.application.service.impl.SubscriptionAmountInputs;
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

    public Page<PaiementAbonnementResponse> findResponses(PaiementAbonnementFilter filter) {
        return repository.findResponsesByFilter(filter.statutAsEnum(), filter.abonnementId(), filter.entrepriseId(), filter.startDate(), filter.endDate(), filter.toPageable());
    }

    /**
     * Returns the entreprise's most recent unpaid facture (FACTURE_GENEREE or EN_RETARD) with its
     * full preuve history, or empty when none exists.
     */
    public Optional<PaiementAbonnementDetailsResponse> findCurrentUnpaidFactureByEntreprise(UUID entrepriseId) {
        List<PaiementAbonnementDetailsResponse> rows = repository.findCurrentUnpaidFacturesByEntreprise(entrepriseId, PageRequest.of(0, 1));
        return rows.isEmpty() ? Optional.empty() : Optional.of(rows.get(0));
    }

    /** Marks the facture VALIDE and records the payment date (copied from the winning preuve). */
    public PaiementAbonnement markAsValide(PaiementAbonnement paiement, LocalDate datePaiement) {
        paiement.setDatePaiement(datePaiement);
        paiement.setStatut(StatutPaiementAbonnement.VALIDE);
        return save(paiement);
    }

    public long countByStatutAndCreatedBetween(StatutPaiementAbonnement statut, LocalDate startDate, LocalDate endDate) {
        return repository.countByStatutAndCreatedBetween(statut, startDate, endDate);
    }

    public BigDecimal sumValidatedRevenueForPeriod(LocalDate startDate, LocalDate endDate) {
        return repository.sumValidatedRevenueForPeriod(startDate, endDate);
    }

    public BigDecimal sumValidatedRevenueForYear(int year) {
        LocalDate startOfYear = LocalDate.of(year, 1, 1);
        LocalDate startOfNextYear = LocalDate.of(year + 1, 1, 1);
        return repository.sumValidatedRevenueForYear(startOfYear, startOfNextYear);
    }

    public PaiementAbonnementStatsResponse getStatistiquesPaiement(String startDate, String endDate){
        return repository.getStatistiquesPaiement(startDate, endDate);
    }

    /** Creates a FACTURE_GENEREE payment — stores tarif + coupon snapshots. */
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

    public PaiementAbonnement markAsEnRetard(PaiementAbonnement paiement) {
        paiement.setStatut(StatutPaiementAbonnement.EN_RETARD);
        return save(paiement);
    }

    public List<PaiementAbonnement> findOverdueInvoices(LocalDate today) {
        return repository.findOverdueInvoices(today);
    }

    public List<PaiementAbonnement> findFacturesAbonnementDues(List<LocalDate> dates) {
        return repository.findFacturesAbonnementDues(dates);
    }

    public Optional<PaiementAbonnement> findFactureNonPayeeByAbonnement(UUID abonnementId) {
        List<PaiementAbonnement> results = repository.findFacturesNonPayeesByAbonnement(abonnementId, PageRequest.of(0, 1));
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }

    public PaiementAbonnement recalculer(PaiementAbonnement paiement, SubscriptionAmountInputs inputs, SubscriptionAmountBreakdown breakdown) {
        paiement.setPlanAbonnementTarif(inputs.tarif());
        paiement.setCoupon(inputs.coupon());
        paiement.setMontantAvantReduction(breakdown.prixDeBase());
        paiement.setReduction(breakdown.reductionCoupon());
        paiement.setMontantFinal(breakdown.montantAPayer());
        return save(paiement);
    }
}
