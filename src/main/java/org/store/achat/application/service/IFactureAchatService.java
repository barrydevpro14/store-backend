package org.store.achat.application.service;

import org.springframework.data.domain.Page;
import org.store.achat.application.dto.FactureAchatEcheanceFilter;
import org.store.achat.application.dto.FactureAchatFilter;
import org.store.achat.application.dto.FactureAchatResponse;

import java.util.UUID;

public interface IFactureAchatService {

    /** Lecture d'une facture achat par id, scopée sur l'entreprise du caller. */
    FactureAchatResponse findResponseById(UUID id);

    /** Liste paginée des factures achat de l'entreprise du caller. */
    Page<FactureAchatResponse> findAllByCurrentEntreprise(FactureAchatFilter factureAchatFilter);

    /** Liste paginée des factures dont l'échéance tombe dans la fenêtre et qui ne sont pas entièrement payées. */
    Page<FactureAchatResponse> findEcheances(FactureAchatEcheanceFilter factureAchatEcheanceFilter);
}
