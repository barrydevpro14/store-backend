package org.store.security.application.service;

import org.store.security.application.dto.RoleResponse;
import org.store.security.domain.model.Role;

import java.util.List;

public interface IRoleService {

    Role findByLibelle(String libelle);

    List<RoleResponse> findAll();
}
