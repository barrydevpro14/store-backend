# Store

Multi-tenant SaaS backend for retail/store management (sales, purchases, stock, inventory, expenses, subscription).

## Stack

- **Java 21**, **Spring Boot 4.0.6**, Maven
- **PostgreSQL** + Spring Data JPA (Hibernate)
- **Spring Security** + **JWT** (`jjwt 0.11.5`)
- **springdoc-openapi 2.8.1** (Swagger UI)
- **Lombok** for entities
- JUnit 5 + Mockito

## Getting started

```bash
# Environment variables (defaults in parentheses)
export URL_DATABASE=localhost:5432
export DB_NAME=db_store
export DB_USERNAME=
export DB_PASSWORD=

./mvnw spring-boot:run
```

Application on `http://localhost:8080`; Actuator health check at `/actuator/health`.

## Architecture

**DDD / hexagonal** architecture, organized per business module (bounded context). Each module follows the same 4-layer structure:

```
<module>/
├── domain/           # business core (model, enums, repository, service)
│   ├── model/        # JPA entities
│   ├── enums/
│   ├── repository/   # interfaces (ports)
│   └── service/      # business logic
├── application/      # use cases
│   ├── service/      # orchestration
│   ├── dto/
│   └── mappers/
├── infrastructure/   # technical adapters
│   ├── repository/   # Spring Data implementations
│   └── specifications/
└── presentation/     # REST API (controllers)
```

### Business modules

| Module | Role |
|---|---|
| `magasin` | Root tenant: `Entreprise`, `Magasin` |
| `users` | Users: `Person`, `Utilisateur`, `Proprietaire`, `Employe` |
| `security` | Authentication & RBAC: `Account`, `Role`, `Permissions`, `RefreshToken` |
| `produit` | Catalog: `Product`, `CategoryProduct`, `Quality`, `ProductFournisseur` |
| `stock` | Stock and movements: `Stock`, `MouvementStock`, `EntreeStock`, `SortieStock` |
| `inventaire` | Physical inventories: `Inventaire`, `LigneInventaire` |
| `achat` | Purchases: `CommandeAchat`, `Fournisseur`, `FactureAchat`, `PaiementAchat` |
| `vente` | Sales: `CommandeVente`, `Client`, `FactureClient` |
| `abonnement` | SaaS billing: `PlanAbonnement`, `Abonnement`, `Coupon`, `Promotion`, `PaiementAbonnement` |
| `notification` | Notifications & alerts: `Notification`, `Alerte`, `AlerteType`, daily `AlertScheduler` |
| `depense` | Expenses: `Depense`, `CategoryDepense` |
| `contact` | Contact form: `ContactMessage`, `ContactStatut` |
| `audit` | Audit log: `AuditLog`, `AuditAction`, `AuditEntityType` |
| `country` | Country catalog: `Country` (65 seeds), currency + countryName in JWT |
| `paiement` | Payment methods: `MoyenPaiement` (ADMIN CRUD, replaces hardcoded enum) |
| `common` | Technical foundations: `BaseEntity`, `AuditableEntity`, `Person`, `PieceJointe`, email Strategy pattern |

## Data model

- **Application multi-tenancy**: `Entreprise` (1) → `Magasin` (N) → `Employe` / `Client` (N).
- **Ownership**: a `Proprietaire` (subclass of `Utilisateur`) owns an `Entreprise`.
- **Authentication**: `Utilisateur` ↔ `Account` (1-1), `Account` ↔ `Role` (N-1), `Role` ↔ `Permissions` (N-N).
- **Inheritance**: `Person` (`@Inheritance JOINED`) → `Utilisateur` → `Proprietaire` / `Employe`; `Person` is also the parent of `Client` and `Fournisseur`.
- **Audit**: every entity extends `BaseEntity` (UUID) or `AuditableEntity` (`createdAt` / `updatedAt` / `createdBy` / `updatedBy` via Spring Data Auditing).
- **Subscription**: a `PlanAbonnement` (Trial/Standard/Pro) + a `TypeAbonnement` (duration + discount) → `Abonnement` linked to an `Entreprise`. Discounts via `Coupon` (with `UtilisationCoupon`) and `Promotion`.
- **FIFO stock**: entries (`EntreeStock` linked to `CommandeAchat`) consumed by exits (`SortieStock` linked to `LigneCommandeVente`), movements tracked by `MouvementStock`.
- **Billing**: `CommandeVente` → `FactureClient`, `CommandeAchat` → `FactureAchat`, payments tracked separately.
- **Email**: `IEmailServiceStrategy` pattern — `BrevoApiEmailServiceStrategy` (prod, HTTPS 443) / `SmtpEmailServiceStrategy` (dev) / `NoOpEmailServiceStrategy` (fallback). Selected by `MailConfig` based on `BREVO_API_KEY` / `app.mail.password`.
- **Stock movements**: all types carry `referenceDocument` (commande reference). `prixAchat` on `ProductFournisseur` retains the last purchase price — never overwritten by the weighted average (`Stock.prixAchatMoyen` is for valuation only).
- **Async locale**: `LocaleAwareTaskDecorator` + `AsyncConfig` propagate the HTTP request locale to `@Async` threads (notifications/alerts in FR even when triggered from a request with `Accept-Language: fr`).
