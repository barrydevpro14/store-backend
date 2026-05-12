# FONCTIONNALITIES.md — Récap des services applicatifs métier

> Ce fichier liste, pour chaque service applicatif (use case), l'endpoint exposé, les règles métier appliquées, les dépendances injectées et les exceptions possibles. À mettre à jour à chaque nouveau service.

---

## 1. Inscription d'un propriétaire — `RegisterPropertyServiceImpl`

**Endpoint** : `POST /api/v1/auth/register` (public, pas d'auth)

**Entrée** : `RegisterPropertyRequest`
```json
{
  "account":    { "username", "password" },
  "utilisateur":{ "nom", "prenom", "email", "telephone", "adresse" },
  "entreprise": { "sigle", "raisonSociale", "ninea", "rccm", "adresse" },
  "magasin":    { "nom", "adresse" }
}
```

**Flux** :
1. Charge le rôle `PROPRIETAIRE` via `IRoleService.findByLibelle`.
2. Charge le plan d'essai actif via `IPlanAbonnementService.findFirstTrialActif`.
3. Crée l'`Account` (`IAccountService.create`) + le `Proprietaire` (`IProprietaireService.create`) + l'`Entreprise` (`IEntrepriseService.create`) + le premier `Magasin` (`IMagasinService.create`).
4. Lie : `account.user = proprietaire`, `proprietaire.entreprise = entreprise`, `entreprise.magasins = [magasin]`.
5. Crée un abonnement d'essai via `IAbonnementService.createTrial`.
6. Génère access JWT via `IJwtService.generateToken` + refresh token UUID opaque via `IRefreshTokenService.create`.

**Règles** :
- `account.username` doit être unique (`UniqueResourceException("account.username.exists")`).
- Téléphone obligatoire et au format SN (`@NotBlank @Phone`).
- Rôle `PROPRIETAIRE` doit exister en BDD (seedé par `DataInitializer`).
- Un plan trial actif doit exister.

**Sortie** : `AuthResponse{ accessToken, refreshToken }` — HTTP 201.

---

## 2. Connexion — `LoginServiceImpl`

**Endpoint** : `POST /api/v1/auth/login` (public)

**Entrée** : `LoginRequest{ username, password }`

**Flux** :
1. `AuthenticationManager.authenticate(...)` vérifie credentials via `UserDetailsServiceImpl`.
2. Si OK, recharge l'`Account` via `IAccountService.findByUsername`.
3. Construit `UserPrincipal` via `IUserPrincipalFactory.build(account)`.
4. Génère access JWT + refresh token persisté.

**Règles** :
- `BadCredentialsException` (Spring Security) si mauvais mot de passe.
- `EntityException("account.notFound")` si pas trouvé après auth (cas pathologique).

**Sortie** : `AuthResponse{ accessToken, refreshToken }` — HTTP 200.

---

## 3. Refresh token — `RefreshTokenServiceImpl.refresh`

**Endpoint** : `POST /api/v1/auth/refresh` (public)

**Entrée** : `RefreshTokenRequest{ refreshToken }`

**Flux** :
1. Lookup du refresh token via `RefreshTokenDomainService.findByToken`.
2. Vérif : non‑révoqué, non‑expiré, lié à un `Utilisateur` qui a un `Account`.
3. Reconstruit `UserPrincipal` puis émet un nouvel access JWT (le refresh token reste le même — **pas de rotation**).

**Règles / erreurs** :
- `UnauthorisedException("refreshToken.invalid|revoked|expired")` selon le cas.

**Sortie** : `AuthResponse{ accessToken (nouveau), refreshToken (inchangé) }` — HTTP 200.

---

## 4. Logout — `RefreshTokenServiceImpl.revoke`

**Endpoint** : `POST /api/v1/auth/logout` (public, idempotent)

**Entrée** : `RefreshTokenRequest{ refreshToken }`

**Flux** :
1. Lookup ; si présent et non‑révoqué : `setRevoked(true) + save`.
2. Sinon : silencieux.

**Sortie** : HTTP 204 (toujours).

---

## 5. Création d'un employé — `EmployeServiceImpl.create`

**Endpoint** : `POST /api/v1/employees` (auth requise, `@PreAuthorize("hasAuthority('EMPLOYE_CREATE')")`)

**Entrée** : `EmployeRequest`
```json
{
  "account":     { "username", "password" },
  "utilisateur": { "nom", "prenom", "email", "@Phone telephone", "adresse" },
  "role":        "MANAGER" | "VENDEUR" | "CAISSIER" | "...",
  "magasinId":   "<uuid>"
}
```

**Flux** :
1. Récupère `currentUser` via `ICurrentUserService.getCurrent`.
2. `IRoleService.findByLibelle(askedRole)` (404 si rôle inconnu).
3. `IPermissionsService.findAllByRoleId(role.id)` → codes permissions du rôle.
4. Vérifie que rôle a `EMPLOYE_ACCESS` (sinon 403).
5. Si rôle a `EMPLOYE_CREATE` (rôle "élevé") et caller n'est pas propriétaire → 403.
6. `IMagasinService.findById(magasinId)` + check ownership :
   - Propriétaire : `magasin.entreprise.id == currentUser.entrepriseId`.
   - Sinon (manager) : `magasin.id == currentUser.magasinId`.
7. Si rôle élevé : `EmployeDomainService.existsByMagasinIdAndRolePermissionCode(magasinId, "EMPLOYE_CREATE")` → 403 si déjà un manager.
8. `IAccountService.create(account, role)` (vérif username unique).
9. `EmployeDomainService.create(utilisateur, account, magasin)` construit + sauve l'`Employe`, lie `account.user`, retourne un `EmployeResponse`.

**Règles métier** :
| Règle | Mécanisme |
|---|---|
| Propriétaire peut créer n'importe quel employé | Étapes 5 et 7 ne s'appliquent qu'aux non‑propriétaires |
| Manager peut créer tout sauf un autre manager | Étape 5 (`EMPLOYE_CREATE && !PROPRIETAIRE_ACCESS`) |
| Un seul rôle "élevé" par magasin | Étape 7 (data‑driven sur `EMPLOYE_CREATE`) |
| Propriétaire ne peut créer que dans **son entreprise** | Étape 6 |
| Manager ne peut créer que dans **son magasin** | Étape 6 |
| Le rôle créé doit être un rôle d'employé | Étape 4 |

**Exceptions** :
- `ForbiddenException("employe.create.role.notAllowed")`, `("employe.create.elevatedRole.forbidden")`, `("magasin.notOwned")`, `("magasin.alreadyHasManager")`.
- `EntityException("role.notFound" | "entity.notFound")`.
- `UniqueResourceException("account.username.exists")`.

**Sortie** : `EmployeResponse{ id, nom, prenom, email, telephone, adresse, username, role, magasinId }` — HTTP 201.

---

## 6. CRUD Magasin + activate/deactivate — `MagasinServiceImpl`

**Endpoints** (`@PreAuthorize("hasAuthority('PROPRIETAIRE_ACCESS')")` au niveau classe) :

| Méthode | Endpoint | Action |
|---|---|---|
| `POST` | `/api/v1/magasins` | Crée un magasin dans l'entreprise du caller (201) |
| `GET` | `/api/v1/magasins?page=0&size=10&sort=nom,asc` | Liste paginée des magasins de l'entreprise courante (200, `Page<MagasinResponse>`) |
| `GET` | `/api/v1/magasins/{id}` | Lit un magasin (200) |
| `PUT` | `/api/v1/magasins/{id}` | Met à jour `nom`/`adresse` (200) |
| `PATCH` | `/api/v1/magasins/{id}/activate` | Soft‑activate (`actif=true`) (200) |
| `PATCH` | `/api/v1/magasins/{id}/deactivate` | Soft‑delete (`actif=false`) (200) |

**`MagasinRequest`** : `@NotBlank nom`, `@NotBlank adresse`.

**`MagasinResponse`** : `id, nom, adresse, actif, entrepriseId`. Constructeur secondaire `(Magasin)`.

**Champ `actif`** : flag visuel uniquement (pas d'effet métier en MVP — n'empêche pas les ventes/achats sur ce magasin). Ajouté via `V2__add_magasin_actif.sql` (`BOOLEAN NOT NULL DEFAULT TRUE`).

**Règle métier (toutes opérations)** : `ensureBelongsToCurrentEntreprise(magasin)` — `magasin.entreprise.id == currentUser.entrepriseId` sinon `ForbiddenException("magasin.notOwned")`.

**Pagination** : query JPQL custom dans `MagasinRepository.findResponsesByEntrepriseId` — projection directe vers `MagasinResponse` via `SELECT new ... MagasinResponse(...)`, `countQuery` séparée. Évite de matérialiser les entités complètes.

**Dépendances injectées** dans `MagasinServiceImpl` :
| Dépendance | Pourquoi |
|---|---|
| `MagasinDomainService` | CRUD Magasin + query projetée paginée |
| `IEntrepriseService` | Chargement de l'entreprise du caller (`findById`) |
| `ICurrentUserService` | `UserPrincipal` du SecurityContext |

**`IMagasinService.create(MagasinRequest, Entreprise) → Magasin`** : signature interne conservée (utilisée par le flow d'inscription propriétaire) ; le controller utilise la nouvelle `create(MagasinRequest) → MagasinResponse` qui scope automatiquement sur l'entreprise du caller.

---

## Conventions transverses

- **i18n** : tous les messages d'erreur passent par `IMessageSourceService` (clés dans `messages*.properties`, fallback `useCodeAsDefaultMessage=true`).
- **Sécurité** : `@PreAuthorize` au niveau controller pour la coarse‑grained auth ; service responsable des règles métier fines.
- **Isolation services** : un `<X>ServiceImpl` n'injecte que `<X>DomainService` + des `I<Y>Service` d'autres agrégats (jamais un `<Y>Repository`).
- **Responses** : tout `<X>Response` doit exposer un constructeur `(<X> entity)`.
- **Permissions** : centralisées dans l'enum `PermissionCode` ; chaque valeur = code en BDD.
