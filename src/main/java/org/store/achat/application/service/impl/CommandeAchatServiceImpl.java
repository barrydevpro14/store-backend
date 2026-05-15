package org.store.achat.application.service.impl;

import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.store.achat.application.dto.CommandeAchatFilter;
import org.store.achat.application.dto.CommandeAchatResponse;
import org.store.achat.application.service.ICommandeAchatService;
import org.store.achat.domain.model.CommandeAchat;
import org.store.achat.domain.service.CommandeAchatDomainService;
import org.store.common.exceptions.ForbiddenException;
import org.store.common.service.ValidatorService;
import org.store.security.application.dto.UserPrincipal;
import org.store.security.application.service.ICurrentUserService;

import java.util.UUID;

/**
 * Lecture des commandes d'achat, scopée sur l'entreprise du caller.
 */
@Service
@Transactional(readOnly = true)
public class CommandeAchatServiceImpl implements ICommandeAchatService {

    private final CommandeAchatDomainService commandeAchatDomainService;
    private final ICurrentUserService currentUserService;
    private final ValidatorService validatorService;

    public CommandeAchatServiceImpl(CommandeAchatDomainService commandeAchatDomainService,
                                    ICurrentUserService currentUserService,
                                    ValidatorService validatorService) {
        this.commandeAchatDomainService = commandeAchatDomainService;
        this.currentUserService = currentUserService;
        this.validatorService = validatorService;
    }

    /** Retourne l'entité commande ou lève `EntityException` (sans scoping, usage interne par d'autres services). */
    @Override
    public CommandeAchat findById(UUID id) {
        return commandeAchatDomainService.findById(id);
    }

    /** Retourne la commande après vérification d'appartenance à l'entreprise du caller. */
    @Override
    public CommandeAchatResponse findResponseById(UUID id) {
        CommandeAchat commande = ensureBelongsToCurrentEntreprise(commandeAchatDomainService.findById(id));
        return new CommandeAchatResponse(commande);
    }

    /** Valide le filter et délègue la query scopée par entreprise. */
    @Override
    public Page<CommandeAchatResponse> findAllByCurrentEntreprise(CommandeAchatFilter commandeAchatFilter) {
        validatorService.validate(commandeAchatFilter);
        return commandeAchatDomainService.findResponsesByFilter(commandeAchatFilter, currentUserService.getCurrent().entrepriseId());
    }

    /** Lève `ForbiddenException` si la commande n'appartient pas à l'entreprise du caller. */
    @Override
    public CommandeAchat ensureBelongsToCurrentEntreprise(CommandeAchat commande) {
        UserPrincipal currentUser = currentUserService.getCurrent();
        if (!commande.getMagasin().getEntreprise().getId().equals(currentUser.entrepriseId())) {
            throw new ForbiddenException("commandeAchat.notOwned");
        }
        return commande;
    }
}
