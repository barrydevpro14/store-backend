package org.store.security.application.service;

import org.store.security.application.dto.AccountRequest;
import org.store.security.domain.model.Account;
import org.store.security.domain.model.Role;

public interface IAccountService {

    Account create(AccountRequest info, Role role);
}
