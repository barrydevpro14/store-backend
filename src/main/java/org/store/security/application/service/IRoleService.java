package org.store.security.application.service;

import org.store.security.domain.model.Role;

public interface IRoleService {

    Role findByLibelle(String libelle);
}
