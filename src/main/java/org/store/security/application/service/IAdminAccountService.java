package org.store.security.application.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.store.security.application.dto.AdminAccountRequest;
import org.store.security.application.dto.AdminAccountResponse;

import java.util.UUID;

public interface IAdminAccountService {

    Page<AdminAccountResponse> findAll(Pageable pageable);

    AdminAccountResponse create(AdminAccountRequest request);

    AdminAccountResponse setEnabled(UUID id, boolean enabled);
}
