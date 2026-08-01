package org.store.achat.application.service;

import org.springframework.data.domain.Page;
import org.springframework.data.repository.query.Param;
import org.store.achat.application.dto.FactureAchatEcheanceFilter;
import org.store.achat.application.dto.FactureAchatFilter;
import org.store.achat.application.dto.FactureAchatResponse;
import org.store.achat.domain.enums.StatutFacture;
import org.store.achat.domain.model.FactureAchat;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface IFactureAchatService {

    /** Lecture d'une facture achat par id, scopée sur l'entreprise du caller. */
    FactureAchatResponse findResponseById(UUID id);

    /** Liste paginée des factures achat de l'entreprise du caller. */
    Page<FactureAchatResponse> findAllByCurrentEntreprise(FactureAchatFilter factureAchatFilter);

    /** Liste paginée des factures dont l'échéance tombe dans la fenêtre et qui ne sont pas entièrement payées. */
    Page<FactureAchatResponse> findEcheances(FactureAchatEcheanceFilter factureAchatEcheanceFilter);

    /** Lecture interne de la facture liée à une commande achat (la cardinalité est 1-1, créée atomiquement). Throw `EntityException("factureAchat.notFoundForCommande")` si absente. */
    FactureAchat findByCommandeId(UUID commandeId);

    List<FactureAchat> findDueOnDates(@Param("dates") List<LocalDate> dates , List<StatutFacture> statutFactures);

    /** Compte les factures achat impayées (NON_PAYEE + PARTIELLEMENT_PAYEE) à l'échelle de l'entreprise. */
    long countUnpaidByEntreprise(UUID entrepriseId);

    /** Compte les factures achat impayées (NON_PAYEE + PARTIELLEMENT_PAYEE) pour un magasin. */
    long countUnpaidByMagasin(UUID magasinId);

    /** Compte les factures achat impayées d'un magasin dans la plage de dates métier (KPI reporting). */
    long countUnpaidByMagasinAndDateBetween(UUID magasinId, LocalDate from, LocalDate to);
}
