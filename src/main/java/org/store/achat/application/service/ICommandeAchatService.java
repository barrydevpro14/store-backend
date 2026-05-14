package org.store.achat.application.service;

import org.springframework.data.domain.Page;
import org.store.achat.application.dto.CommandeAchatFilter;
import org.store.achat.application.dto.CommandeAchatResponse;

import java.util.UUID;

public interface ICommandeAchatService {

    /** Lecture d'une commande achat par id, scopée sur l'entreprise du caller. */
    CommandeAchatResponse findResponseById(UUID id);

    /** Liste paginée des commandes achat de l'entreprise du caller. */
    Page<CommandeAchatResponse> findAllByCurrentEntreprise(CommandeAchatFilter commandeAchatFilter);
}
