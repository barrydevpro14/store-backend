package org.store.vente.application.service.impl;

import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.store.common.exceptions.BadArgumentException;
import org.store.common.exceptions.EntityException;
import org.store.common.service.ValidatorService;
import org.store.magasin.application.service.IMagasinService;
import org.store.security.application.dto.UserPrincipal;
import org.store.security.application.service.ICurrentUserService;
import org.store.vente.application.dto.CommandeVenteFilter;
import org.store.vente.application.dto.CommandeVenteResponse;
import org.store.vente.application.service.ICommandeVenteService;
import org.store.vente.domain.enums.CommandeVenteStatut;
import org.store.vente.domain.model.CommandeVente;
import org.store.vente.domain.service.CommandeVenteDomainService;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Lectures listing et détail unitaire des commandes vente. Le scoping (entreprise + magasin
 * accessible au caller) est appliqué à chaque appel via {@link IMagasinService} et la query JPQL
 * (qui filtre toujours par entrepriseId).
 */
@Service
@Transactional(readOnly = true)
public class CommandeVenteServiceImpl implements ICommandeVenteService {

    private final CommandeVenteDomainService commandeVenteDomainService;
    private final IMagasinService magasinService;
    private final ICurrentUserService currentUserService;
    private final ValidatorService validatorService;

    public CommandeVenteServiceImpl(CommandeVenteDomainService commandeVenteDomainService,
                                    IMagasinService magasinService,
                                    ICurrentUserService currentUserService,
                                    ValidatorService validatorService) {
        this.commandeVenteDomainService = commandeVenteDomainService;
        this.magasinService = magasinService;
        this.currentUserService = currentUserService;
        this.validatorService = validatorService;
    }

    /** Listing paginé filtré : valide le filter, vérifie l'accès magasin du caller, délègue au domain. */
    @Override
    public Page<CommandeVenteResponse> findAllByCurrentEntreprise(CommandeVenteFilter filter) {
        validatorService.validate(filter);
        UserPrincipal currentUser = currentUserService.getCurrent();
        magasinService.ensureAccessibleByCurrentUser(magasinService.findById(filter.magasinId()));
        return commandeVenteDomainService.findResponsesByFilter(filter, currentUser.entrepriseId());
    }

    /** GET by id : retourne la projection JPQL avec user résolu, scopée par l'entreprise du caller. */
    @Override
    public CommandeVenteResponse findResponseById(UUID id) {
        UserPrincipal currentUser = currentUserService.getCurrent();
        return commandeVenteDomainService.findResponseById(id, currentUser.entrepriseId())
                .orElseThrow(() -> new EntityException("commandeVente.notFound", id));
    }

    /** Délègue le comptage journalier des commandes vente au domain. */
    @Override
    public long countByEntrepriseAndDay(UUID entrepriseId, LocalDateTime startOfDay, LocalDateTime endOfDay) {
        return commandeVenteDomainService.countByEntrepriseAndDay(entrepriseId, startOfDay, endOfDay);
    }

    /** Bascule une commande VALIDATE en CLOTURE après vérification d'accès et de statut. */
    @Override
    @Transactional
    public CommandeVenteResponse cloturerCommande(UUID commandeId) {
        UserPrincipal currentUser = currentUserService.getCurrent();
        CommandeVente commande = commandeVenteDomainService.findById(commandeId);
        magasinService.ensureAccessibleByCurrentUser(commande.getMagasin());

        if (commande.getStatut() != CommandeVenteStatut.VALIDATE) {
            throw new BadArgumentException("commandeVente.cloturer.notValidated", commande.getStatut().name());
        }

        commandeVenteDomainService.cloturer(commande);

        return commandeVenteDomainService.findResponseById(commandeId, currentUser.entrepriseId())
                .orElseThrow(() -> new EntityException("commandeVente.notFound", commandeId));
    }
}
