package org.store.vente.application.service;

import org.springframework.data.domain.Page;
import org.store.vente.application.dto.CommandeVenteFilter;
import org.store.vente.application.dto.CommandeVenteResponse;

import java.util.UUID;

public interface ICommandeVenteService {

    Page<CommandeVenteResponse> findAllByCurrentEntreprise(CommandeVenteFilter filter);

    CommandeVenteResponse findResponseById(UUID id);
}
