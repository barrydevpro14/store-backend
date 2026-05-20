package org.store.users.application.service.impl;

import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.store.common.exceptions.EntityException;
import org.store.common.exceptions.ForbiddenException;
import org.store.common.service.ValidatorService;
import org.store.magasin.application.service.IMagasinService;
import org.store.magasin.domain.model.Magasin;
import org.store.security.application.dto.ResetPasswordRequest;
import org.store.security.application.dto.UserPrincipal;
import org.store.security.application.enums.PermissionCode;
import org.store.security.application.service.IAccountService;
import org.store.security.application.service.ICurrentUserService;
import org.store.security.application.service.IPermissionsService;
import org.store.security.application.service.IRoleService;
import org.store.security.domain.model.Account;
import org.store.security.domain.model.Role;
import org.store.users.application.dto.EmployeFilter;
import org.store.users.application.dto.EmployeRequest;
import org.store.users.application.dto.EmployeResponse;
import org.store.users.application.dto.EmployeUpdateCommand;
import org.store.users.application.dto.EmployeUpdateRequest;
import org.store.users.application.service.IEmployeService;
import org.store.users.domain.model.Employe;
import org.store.users.domain.service.EmployeDomainService;
import org.store.users.domain.service.UtilisateurDomainService;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Orchestre le cycle de vie d'un Employe : creation (avec regles data-driven de
 * hierarchie role + unicite Manager par magasin), listing pagine filtre scope
 * entreprise, lecture detail, mise a jour (infos perso + role + magasin avec
 * re-validation des regles de hierarchie), desactivation/reactivation via
 * account.enabled.
 *
 * <p>Scoping : PROPRIETAIRE/ADMIN voient toute l'entreprise. MANAGER est force
 * sur son propre magasin (filter.magasinId remplace par currentUser.magasinId).
 */
@Service
@Transactional(readOnly = true)
public class EmployeServiceImpl implements IEmployeService {

    private final EmployeDomainService employeDomainService;
    private final UtilisateurDomainService utilisateurDomainService;
    private final IAccountService accountService;
    private final IRoleService roleService;
    private final IPermissionsService permissionsService;
    private final IMagasinService magasinService;
    private final ICurrentUserService currentUserService;
    private final ValidatorService validatorService;

    public EmployeServiceImpl(EmployeDomainService employeDomainService,
                              UtilisateurDomainService utilisateurDomainService,
                              IAccountService accountService,
                              IRoleService roleService,
                              IPermissionsService permissionsService,
                              IMagasinService magasinService,
                              ICurrentUserService currentUserService,
                              ValidatorService validatorService) {
        this.employeDomainService = employeDomainService;
        this.utilisateurDomainService = utilisateurDomainService;
        this.accountService = accountService;
        this.roleService = roleService;
        this.permissionsService = permissionsService;
        this.magasinService = magasinService;
        this.currentUserService = currentUserService;
        this.validatorService = validatorService;
    }

    /** Cree un employe (account + utilisateur + rattachement magasin) avec validation hierarchie role et unicite Manager. */
    @Override
    @Transactional
    public EmployeResponse create(EmployeRequest employeRequest) {
        UserPrincipal currentUser = currentUserService.getCurrent();

        Role role = roleService.findByLibelle(employeRequest.role());
        List<String> rolePermissions = permissionsService.findAllByRoleId(role.getId());

        ensureRoleAllowed(role);
        ensureCallerCanAssignRole(currentUser, rolePermissions);

        Magasin magasin = magasinService.ensureAccessibleByCurrentUser(
                magasinService.findById(employeRequest.magasinId())
        );

        ensureMagasinDoesNotAlreadyHaveManager(magasin.getId(), rolePermissions);

        utilisateurDomainService.ensureContactsAvailable(
                employeRequest.utilisateur().email(),
                employeRequest.utilisateur().telephone()
        );

        Account account = accountService.create(employeRequest.account(), role);

        return employeDomainService.create(employeRequest.utilisateur(), account, magasin);
    }

    /** Retourne l'Employe correspondant au user courant. Throw ForbiddenException si l'utilisateur connecte n'est pas un Employe (ex : un PROPRIETAIRE). */
    @Override
    public Employe findCurrentUser() {
        UserPrincipal currentUser = currentUserService.getCurrent();
        return Optional.ofNullable(currentUser.userId())
                .flatMap(employeDomainService::findOptionalById)
                .orElseThrow(() -> new ForbiddenException("vente.user.required"));
    }

    /** Listing scope entreprise. MANAGER force sur son magasin (filter.magasinId ignore au profit du sien). */
    @Override
    public Page<EmployeResponse> findAllByCurrentEntreprise(EmployeFilter filter) {
        validatorService.validate(filter);
        UserPrincipal currentUser = currentUserService.getCurrent();
        EmployeFilter scopedFilter = scopeFilterForManager(filter, currentUser);
        return employeDomainService.findResponsesByFilter(scopedFilter, currentUser.entrepriseId());
    }

    /** Detail by id avec scoping entreprise + acces magasin pour MANAGER. */
    @Override
    public EmployeResponse findResponseById(UUID id) {
        UserPrincipal currentUser = currentUserService.getCurrent();
        Employe employe = findAccessibleEmploye(id, currentUser);
        return new EmployeResponse(employe);
    }

    /** Met a jour les infos personnelles + role + magasin avec re-validation des regles de hierarchie. */
    @Override
    @Transactional
    public EmployeResponse update(UUID id, EmployeUpdateRequest request) {
        validatorService.validate(request);
        UserPrincipal currentUser = currentUserService.getCurrent();
        Employe employe = findAccessibleEmploye(id, currentUser);

        Role newRole = roleService.findByLibelle(request.role());
        List<String> newRolePermissions = permissionsService.findAllByRoleId(newRole.getId());
        ensureRoleAllowed(newRole);
        ensureCallerCanAssignRole(currentUser, newRolePermissions);

        Magasin newMagasin = magasinService.ensureAccessibleByCurrentUser(magasinService.findById(request.magasinId()));

        if (estMontageVersManagerSurAutreMagasin(employe, newRole, newMagasin)) {
            ensureMagasinDoesNotAlreadyHaveManager(newMagasin.getId(), newRolePermissions);
        }

        utilisateurDomainService.ensureContactsAvailableForUpdate(request.email(), request.telephone(), employe.getId());

        employeDomainService.update(employe, new EmployeUpdateCommand(
                request.nom(), request.prenom(), request.email(), request.telephone(), request.adresse()
        ));
        employeDomainService.changeRole(employe, newRole);
        employeDomainService.changeMagasin(employe, newMagasin);

        return new EmployeResponse(employe);
    }

    /** Desactive l'employe (account.enabled = false) — l'historique reste intact, login bloque. */
    @Override
    @Transactional
    public void deactivate(UUID id) {
        UserPrincipal currentUser = currentUserService.getCurrent();
        Employe employe = findAccessibleEmploye(id, currentUser);
        accountService.setEnabled(employe.getAccount(), false);
    }

    /** Reactive un employe precedemment desactive. */
    @Override
    @Transactional
    public void activate(UUID id) {
        UserPrincipal currentUser = currentUserService.getCurrent();
        Employe employe = findAccessibleEmploye(id, currentUser);
        accountService.setEnabled(employe.getAccount(), true);
    }

    /** Force le mot de passe d'un employe (reset admin, sans verification de l'ancien). */
    @Override
    @Transactional
    public void resetPassword(UUID id, ResetPasswordRequest request) {
        validatorService.validate(request);
        UserPrincipal currentUser = currentUserService.getCurrent();
        Employe employe = findAccessibleEmploye(id, currentUser);
        accountService.resetPassword(employe.getAccount(), request.newPassword());
    }

    /** Retourne l'employe si scope entreprise OK + magasin accessible (MANAGER force sur son magasin). */
    public Employe findAccessibleEmploye(UUID id, UserPrincipal currentUser) {
        Employe employe = employeDomainService.findOptionalById(id)
                .orElseThrow(() -> new EntityException("employe.notFound", id));
        ensureBelongsToCurrentEntreprise(employe, currentUser.entrepriseId());
        ensureAccessibleByManager(employe, currentUser);
        return employe;
    }

    /** Verifie que l'employe est dans la meme entreprise que le caller (via magasin.entreprise). */
    public void ensureBelongsToCurrentEntreprise(Employe employe, UUID entrepriseId) {
        if (!employe.getMagasin().getEntreprise().getId().equals(entrepriseId)) {
            throw new ForbiddenException("employe.notOwned");
        }
    }

    /** Bloque le MANAGER qui tente d'acceder a un employe d'un autre magasin. */
    public void ensureAccessibleByManager(Employe employe, UserPrincipal currentUser) {
        if (currentUser.hasPermission(PermissionCode.PROPRIETAIRE_ACCESS) || currentUser.hasPermission(PermissionCode.ADMIN_ACCESS)) {
            return;
        }
        if (currentUser.magasinId() != null && !currentUser.magasinId().equals(employe.getMagasin().getId())) {
            throw new ForbiddenException("employe.notAccessibleByManager");
        }
    }

    /** Force le filtre magasinId sur le magasin du MANAGER (proprietaire/admin gardent le filtre fourni). */
    public EmployeFilter scopeFilterForManager(EmployeFilter filter, UserPrincipal currentUser) {
        if (currentUser.hasPermission(PermissionCode.PROPRIETAIRE_ACCESS) || currentUser.hasPermission(PermissionCode.ADMIN_ACCESS)) {
            return filter;
        }
        return new EmployeFilter(filter.nom(), filter.prenom(), filter.role(),
                currentUser.magasinId(), filter.actif(), filter.page(), filter.size());
    }

    /**
     * Vérifie que le rôle cible est explicitement marqué `assignableToEmploye`
     * en base — empêche l'assignation de PROPRIETAIRE / ADMIN à un employé,
     * même si ces rôles portent par ailleurs `EMPLOYE_ACCESS` (cas ADMIN
     * vendor-super-admin).
     */
    public void ensureRoleAllowed(Role role) {
        if (!role.isAssignableToEmploye()) {
            throw new ForbiddenException("employe.create.role.notAllowed", role.getLibelle());
        }
    }

    /** Verifie que le caller a l'autorite pour assigner un role "eleve" (qui inclut EMPLOYE_CREATE) — reserve PROPRIETAIRE. */
    public void ensureCallerCanAssignRole(UserPrincipal currentUser, List<String> rolePermissions) {
        boolean elevatedRole = rolePermissions.contains(PermissionCode.EMPLOYE_CREATE.name());
        if (elevatedRole && !currentUser.hasPermission(PermissionCode.PROPRIETAIRE_ACCESS)) {
            throw new ForbiddenException("employe.create.elevatedRole.forbidden");
        }
    }

    /** Empeche la nomination d'un second MANAGER sur un magasin qui en a deja un. */
    public void ensureMagasinDoesNotAlreadyHaveManager(UUID magasinId, List<String> rolePermissions) {
        boolean elevatedRole = rolePermissions.contains(PermissionCode.EMPLOYE_CREATE.name());
        if (elevatedRole && employeDomainService.existsByMagasinIdAndRolePermissionCode(magasinId, PermissionCode.EMPLOYE_CREATE.name())) {
            throw new ForbiddenException("magasin.alreadyHasManager");
        }
    }

    /** Detecte si on promeut un employe en MANAGER (ou change le magasin d'un MANAGER vers un autre magasin) → declenche la verif d'unicite. */
    public boolean estMontageVersManagerSurAutreMagasin(Employe employe, Role newRole, Magasin newMagasin) {
        return !employe.getAccount().getRole().getId().equals(newRole.getId())
                || !employe.getMagasin().getId().equals(newMagasin.getId());
    }
}
