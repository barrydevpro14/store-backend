package org.store.achat.application.service;

import org.springframework.data.domain.Page;
import org.store.achat.application.dto.CommandeAchatFilter;
import org.store.achat.application.dto.CommandeAchatResponse;
import org.store.achat.domain.model.CommandeAchat;

import java.util.UUID;

public interface ICommandeAchatService {

    /** Lecture interne d'une commande achat par id (utilisée par d'autres agrégats, sans scoping). */
    CommandeAchat findById(UUID id);

    /** Lecture d'une commande achat par id, scopée sur l'entreprise du caller. */
    CommandeAchatResponse findResponseById(UUID id);

    /** Liste paginée des commandes achat de l'entreprise du caller. */
    Page<CommandeAchatResponse> findAllByCurrentEntreprise(CommandeAchatFilter commandeAchatFilter);

    /** Vérifie que la commande appartient à l'entreprise du caller. Throw `ForbiddenException("commandeAchat.notOwned")` sinon. */
    CommandeAchat ensureBelongsToCurrentEntreprise(CommandeAchat commande);
}
