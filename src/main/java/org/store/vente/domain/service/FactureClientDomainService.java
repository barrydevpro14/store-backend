package org.store.vente.domain.service;

import org.springframework.data.domain.Page;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Service;
import org.store.achat.domain.enums.StatutFacture;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.store.common.service.GlobalService;
import org.store.common.tools.ReferenceHelper;
import org.store.vente.application.dto.CaisseResumeFilter;
import org.store.vente.application.dto.FactureClientCreate;
import org.store.vente.application.dto.FactureClientFilter;
import org.store.vente.application.dto.FactureClientResponse;
import org.store.vente.domain.model.FactureClient;
import org.store.vente.domain.repository.FactureClientRepository;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

@Service
public class FactureClientDomainService extends GlobalService<FactureClient, FactureClientRepository> {
    public FactureClientDomainService(FactureClientRepository repository) {
        super(repository);
    }

    /** Crée et persiste une facture client initiale (statut NON_PAYEE, montantPaye=0). */
    public FactureClient create(FactureClientCreate factureClientCreate) {
        FactureClient facture = new FactureClient();
        facture.setCommande(factureClientCreate.commande());
        facture.setNumero(factureClientCreate.numero());
        facture.setDate(factureClientCreate.date());
        facture.setDateEcheance(factureClientCreate.dateEcheance());
        facture.setMontantTotal(factureClientCreate.montantTotal());
        facture.setMontantPaye(BigDecimal.ZERO);
        facture.setStatut(StatutFacture.NON_PAYEE);
        return save(facture);
    }

    /** Génère un numéro de facture unique au format FACT-yyyyMMdd-HHmmssSSS. */
    public String generateNumero() {
        return ReferenceHelper.generate("FACT");
    }

    public Optional<FactureClient> findByCommandeId(UUID commandeId) {
        return repository.findByCommandeId(commandeId);
    }

    /** Listing paginé filtré scopé entreprise (projection JPQL). */
    public Page<FactureClientResponse> findResponsesByFilter(FactureClientFilter filter, UUID entrepriseId) {
        return repository.findResponsesByFilter(entrepriseId, filter.magasinId(),filter.clientId(),
                filter.vendeurId(),filter.statutAsEnum(),filter.numero(),filter.montantMin() , filter.montantMax(),
                filter.startDate(), filter.endDate(),
                filter.toPageable());
    }

    /** Projection JPQL d'une facture par id, scopée entreprise (Optional empty si introuvable ou autre entreprise). */
    public Optional<FactureClientResponse> findResponseById(UUID id, UUID entrepriseId) {
        return repository.findResponseById(id, entrepriseId);
    }

    /** Somme des montants totaux des factures rattachées à des commandes créées dans le magasin sur la période du range. */
    public BigDecimal sumMontantCommandesForCaisse(CaisseResumeFilter range, UUID entrepriseId) {
        return repository.sumMontantTotalByMagasinAndDay(range.magasinId(), entrepriseId, range.startOfPeriod(), range.endOfPeriod());
    }

    /** Bascule la facture en statut ANNULEE (paiements conservés tels quels pour audit). */
    public FactureClient cancel(FactureClient facture) {
        facture.setStatut(StatutFacture.ANNULEE);
        return save(facture);
    }

    /** Incrémente montantPaye et recalcule le statut selon le rapport montantPaye/montantTotal. */
    public FactureClient applyPaiement(FactureClient facture, BigDecimal montant) {
        BigDecimal montantPayeActuel = facture.getMontantPaye() != null ? facture.getMontantPaye() : BigDecimal.ZERO;
        BigDecimal nouveauPaye = montantPayeActuel.add(montant);
        facture.setMontantPaye(nouveauPaye);

        if (nouveauPaye.compareTo(BigDecimal.ZERO) == 0) {
            facture.setStatut(StatutFacture.NON_PAYEE);
        } else if (nouveauPaye.compareTo(facture.getMontantTotal()) < 0) {
            facture.setStatut(StatutFacture.PARTIELLEMENT_PAYEE);
        } else {
            facture.setStatut(StatutFacture.PAYEE);
        }

        return save(facture);
    }

    /** Compte les factures dans un statut donné pour un magasin. */
    public long countByMagasinIdAndStatut(UUID magasinId, List<StatutFacture> statuts) {
        return repository.countByMagasinIdAndStatut(magasinId, statuts);
    }

    /** Revenue total des ventes VALIDATE aujourd'hui pour toute l'entreprise. */
    public BigDecimal sumMontantByEntrepriseAndDay(UUID entrepriseId, LocalDateTime startOfDay, LocalDateTime endOfDay) {
        BigDecimal result = repository.sumMontantTotalByEntrepriseAndDay(entrepriseId, startOfDay, endOfDay);
        return result != null ? result : BigDecimal.ZERO;
    }

    /** Nombre de factures non soldées (NON_PAYEE ou PARTIELLEMENT_PAYEE) pour toute l'entreprise. */
    public long countUnpaidByEntreprise(UUID entrepriseId) {
        return repository.countByEntrepriseAndStatuts(entrepriseId,
                List.of(StatutFacture.NON_PAYEE, StatutFacture.PARTIELLEMENT_PAYEE));
    }

    public List<FactureClient> findDueOnDates(@Param("dates") List<LocalDate> dates , List<StatutFacture> statutFactures){
        return repository.findDueOnDates(dates, statutFactures);
    }
}
