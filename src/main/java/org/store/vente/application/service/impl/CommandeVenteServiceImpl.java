package org.store.vente.application.service.impl;

import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.store.common.exceptions.EntityException;
import org.store.common.service.ValidatorService;
import org.store.magasin.application.service.IMagasinService;
import org.store.security.application.dto.UserPrincipal;
import org.store.security.application.service.ICurrentUserService;
import org.store.vente.application.dto.CommandeVenteFilter;
import org.store.vente.application.dto.CommandeVenteResponse;
import org.store.vente.application.service.ICommandeVenteService;
import org.store.vente.domain.service.CommandeVenteDomainService;

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
}
