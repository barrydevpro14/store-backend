# Store

Backend SaaS multi‑tenant de gestion de commerce/magasin (ventes, achats, stock, inventaire, dépenses, abonnement).

## Stack

- **Java 21**, **Spring Boot 4.0.6**, Maven
- **PostgreSQL** + Spring Data JPA (Hibernate)
- **Spring Security** + **JWT** (`jjwt 0.11.5`)
- **springdoc-openapi 2.8.1** (Swagger UI)
- **Lombok** pour les entités
- JUnit 5 + Mockito

## Démarrage

```bash
# Variables (valeurs par défaut entre parenthèses)
export URL_DATABASE=localhost:5432
export DB_NAME=db_store
export DB_USERNAME=
export DB_PASSWORD=

./mvnw spring-boot:run
```

Application sur `http://localhost:8080` ; healthcheck Actuator sur `/actuator/health`.

## Architecture

Architecture **DDD / hexagonale**, organisée par module métier (bounded context). Chaque module suit la même structure à 4 couches :

```
<module>/
├── domain/           # cœur métier (model, enums, repository, service)
│   ├── model/        # entités JPA
│   ├── enums/
│   ├── repository/   # interfaces (ports)
│   └── service/      # logique métier
├── application/      # cas d'usage
│   ├── service/      # orchestration
│   ├── dto/
│   └── mappers/
├── infrastructure/   # adaptateurs techniques
│   ├── repository/   # implémentations Spring Data
│   └── specifications/
└── presentation/     # API REST (controllers)
```

### Modules métier

| Module | Rôle |
|---|---|
| `magasin` | Tenant root : `Entreprise`, `Magasin` |
| `users` | Utilisateurs : `Person`, `Utilisateur`, `Proprietaire`, `Employe` |
| `security` | Authentification & RBAC : `Account`, `Role`, `Permissions`, `RefreshToken` |
| `produit` | Catalogue : `Product`, `CategoryProduct`, `Quality`, `ProductFournisseur` |
| `stock` | Stocks et mouvements : `Stock`, `MouvementStock`, `EntreeStock`, `SortieStock` |
| `inventaire` | Inventaires physiques : `Inventaire`, `LigneInventaire` |
| `achat` | Achats : `CommandeAchat`, `Fournisseur`, `FactureAchat`, `PaiementAchat` |
| `vente` | Ventes : `CommandeVente`, `Client`, `FactureClient` |
| `abonnement` | Facturation SaaS : `PlanAbonnement`, `Abonnement`, `Coupon`, `Promotion`, `PaiementAbonnement` |
| `notification` | Notifs & échéances : `Notification`, `NotificationTemplate`, `Echeance` |
| `depense` | Dépenses : `Depense`, `CategoryDepense` |
| `common` | Bases techniques : `BaseEntity`, `AuditableEntity`, `Person`, `PieceJointe` |

## Modèle de données

- **Multi‑tenancy** applicatif : `Entreprise` (1) → `Magasin` (N) → `Employe` / `Client` (N).
- **Propriété** : un `Proprietaire` (sous‑classe de `Utilisateur`) possède une `Entreprise`.
- **Authentification** : `Utilisateur` ↔ `Account` (1‑1), `Account` ↔ `Role` (N‑1), `Role` ↔ `Permissions` (N‑N).
- **Héritage** : `Person` (`@Inheritance JOINED`) → `Utilisateur` → `Proprietaire` / `Employe` ; `Person` est aussi parent de `Client` et `Fournisseur`.
- **Audit** : toutes les entités étendent `BaseEntity` (UUID) ou `AuditableEntity` (`createdAt`/`updatedAt`/`createdBy`/`updatedBy` via Spring Data Auditing).
- **Abonnement** : un `PlanAbonnement` (Trial/Standard/Pro) + un `TypeAbonnement` (durée + remise) → `Abonnement` lié à une `Entreprise`. Réductions via `Coupon` (avec `UtilisationCoupon`) et `Promotion`.
- **Stock FIFO** : entrées (`EntreeStock` lié à `CommandeAchat`) consommées par sorties (`SortieStock` lié à `LigneCommandeVente`), mouvements tracés par `MouvementStock`.
- **Facturation** : `CommandeVente` → `FactureClient`, `CommandeAchat` → `FactureAchat`, paiements suivis séparément.
