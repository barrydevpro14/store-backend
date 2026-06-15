package org.store.achat.domain.service;

import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.store.achat.application.dto.FactureAchatCreate;
import org.store.achat.application.dto.FactureAchatEcheanceFilter;
import org.store.achat.application.dto.FactureAchatFilter;
import org.store.achat.application.dto.FactureAchatResponse;
import org.store.achat.domain.enums.StatutFacture;
import org.store.achat.domain.model.FactureAchat;
import org.store.achat.domain.repository.FactureAchatRepository;
import org.store.common.service.GlobalService;
import org.store.common.tools.ReferenceHelper;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

@Service
public class FactureAchatDomainService extends GlobalService<FactureAchat, FactureAchatRepository> {
    public FactureAchatDomainService(FactureAchatRepository repository) {
        super(repository);
    }

    /** Crée et persiste une facture d'achat initiale (statut NON_PAYEE, montantPaye=0). */
    public FactureAchat create(FactureAchatCreate factureAchatCreate) {
        FactureAchat facture = new FactureAchat();
        facture.setCommande(factureAchatCreate.commande());
        facture.setNumero(factureAchatCreate.numero());
        facture.setDate(factureAchatCreate.date());
        facture.setDateEcheance(factureAchatCreate.dateEcheance());
        facture.setMontantTotal(factureAchatCreate.montantTotal());
        facture.setMontantPaye(BigDecimal.ZERO);
        facture.setStatut(StatutFacture.NON_PAYEE);
        return save(facture);
    }

    public Page<FactureAchatResponse> findResponsesByFilter(FactureAchatFilter filter, UUID entrepriseId) {
        return repository.findResponsesByFilter(
                entrepriseId, filter.magasinId(), filter.fournisseurId(),
                filter.statutAsEnum(), filter.startDate(), filter.endDate(),
                filter.toPageable());
    }

    public Page<FactureAchatResponse> findEcheances(FactureAchatEcheanceFilter filter, UUID entrepriseId) {
        return repository.findEcheances(
                entrepriseId, filter.magasinId(),
                filter.startDate(), filter.endDate(),
                filter.toPageable());
    }

    public Optional<FactureAchat> findByCommandeId(UUID commandeId) {
        return repository.findByCommandeId(commandeId);
    }

    /**
     * Indique si une facture portant ce numéro existe déjà. Utilisé en
     * pré-check avant `create(...)` pour surfacer une erreur métier
     * propre (400 + message i18n) plutôt que de laisser la contrainte
     * unique `facture_achat_numero_key` lever un
     * `DataIntegrityViolationException` au flush.
     */
    public boolean existsByNumero(String numero) {
        return repository.existsByNumero(numero);
    }

    /**
     * Génère un numéro de facture unique au format
     * `FACT-yyyyMMdd-HHmmssSSS` quand l'OWNER laisse le champ vide au
     * `validate(...)`. Format aligné sur les autres références
     * métier (`CMD-…`, etc.) — granularité ms suffisante pour éviter
     * les collisions en pratique ; la contrainte unique DB reste le
     * garde-fou ultime.
     */
    public String generateNumero() {
        return ReferenceHelper.generate("FACT");
    }

    /** Bascule la facture en statut ANNULEE (paiements conservés tels quels pour audit). */
    public FactureAchat cancel(FactureAchat facture) {
        facture.setStatut(StatutFacture.ANNULEE);
        return save(facture);
    }

    /** Incrémente montantPaye et recalcule le statut selon le rapport montantPaye/montantTotal. */
    public FactureAchat applyPaiement(FactureAchat facture, BigDecimal montant) {
        BigDecimal nouveauPaye = (facture.getMontantPaye() != null ? facture.getMontantPaye() : BigDecimal.ZERO).add(montant);
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
}
