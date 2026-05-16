package org.store.users.domain.service;

import org.springframework.stereotype.Service;
import org.store.common.service.GlobalService;
import org.store.magasin.domain.model.Magasin;
import org.store.security.domain.model.Account;
import org.store.users.application.dto.EmployeResponse;
import org.store.users.application.dto.UtilisateurRequest;
import org.store.users.domain.model.Employe;
import org.store.users.domain.repository.EmployeRepository;

import java.util.Optional;
import java.util.UUID;

@Service
public class EmployeDomainService extends GlobalService<Employe, EmployeRepository> {
    public EmployeDomainService(EmployeRepository repository) {
        super(repository);
    }

    public boolean existsByMagasinIdAndRolePermissionCode(UUID magasinId, String permissionCode) {
        return repository.existsByMagasinIdAndRolePermissionCode(magasinId, permissionCode);
    }

    public Optional<Employe> findOptionalById(UUID id) {
        return repository.findById(id);
    }

    public EmployeResponse create(UtilisateurRequest utilisateurRequest, Account account, Magasin magasin) {
        Employe employe = new Employe();
        employe.setAccount(account);
        employe.setNom(utilisateurRequest.nom());
        employe.setPrenom(utilisateurRequest.prenom());
        employe.setEmail(utilisateurRequest.email());
        employe.setTelephone(utilisateurRequest.telephone());
        employe.setAdresse(utilisateurRequest.adresse());
        employe.setMagasin(magasin);

        Employe saved = save(employe);
        account.setUser(saved);

        return new EmployeResponse(saved);
    }
}
