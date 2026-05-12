-- =============================================================================
-- V1__init_schema.sql
-- Schema baseline (Postgres) pour le projet STORE.
-- Couvre les 40 entites JPA reparties dans 13 modules.
-- Generated manually a partir des entites du domaine (2026-05-12).
--
-- Conventions :
--   * UUID natif Postgres comme PK.
--   * TIMESTAMP pour LocalDateTime, DATE pour LocalDate.
--   * NUMERIC(19,2) pour les montants BigDecimal.
--   * Heritage JOINED : la PK de la sous-table est aussi FK vers la table parent.
--   * Toutes les FK sont declarees a la fin via ALTER TABLE pour eviter
--     les references avant-cycle.
-- =============================================================================

-- ============================ A. COMMON ======================================

CREATE TABLE person (
    id          UUID PRIMARY KEY,
    created_at  TIMESTAMP,
    updated_at  TIMESTAMP,
    created_by  VARCHAR(255),
    updated_by  VARCHAR(255),
    nom         VARCHAR(255),
    prenom      VARCHAR(255),
    email       VARCHAR(255),
    telephone   VARCHAR(255),
    adresse     VARCHAR(255)
);

CREATE TABLE piece_jointe (
    id          UUID PRIMARY KEY,
    document    OID,
    date        DATE,
    product_id  UUID
);

-- ============================ B. SECURITY ====================================

CREATE TABLE role (
    id          UUID PRIMARY KEY,
    libelle     VARCHAR(255),
    description VARCHAR(255)
);

CREATE TABLE permissions (
    id   UUID PRIMARY KEY,
    code VARCHAR(255)
);

CREATE TABLE role_permission (
    role_id       UUID NOT NULL,
    permission_id UUID NOT NULL,
    PRIMARY KEY (role_id, permission_id)
);

CREATE TABLE account (
    id            UUID PRIMARY KEY,
    created_at    TIMESTAMP,
    updated_at    TIMESTAMP,
    created_by    VARCHAR(255),
    updated_by    VARCHAR(255),
    username      VARCHAR(255),
    password      VARCHAR(255),
    enabled       BOOLEAN NOT NULL,
    locked        BOOLEAN NOT NULL,
    creation_date TIMESTAMP,
    role_id       UUID
);

CREATE TABLE refresh_token (
    id                  UUID PRIMARY KEY,
    created_at          TIMESTAMP,
    updated_at          TIMESTAMP,
    created_by          VARCHAR(255),
    updated_by          VARCHAR(255),
    token               VARCHAR(255),
    expiry_date         TIMESTAMP,
    revoked             BOOLEAN NOT NULL,
    replaced_by_token   VARCHAR(255),
    user_id             UUID
);

-- ============================ C. USERS (JOINED) ==============================

CREATE TABLE utilisateur (
    id         UUID PRIMARY KEY,
    account_id UUID NOT NULL,
    CONSTRAINT uk_utilisateur_account UNIQUE (account_id)
);

CREATE TABLE proprietaire (
    id UUID PRIMARY KEY
);

CREATE TABLE employees (
    id         UUID PRIMARY KEY,
    magasin_id UUID
);

-- ============================ D. ENTREPRISE / MAGASIN ========================

CREATE TABLE entreprise (
    id               UUID PRIMARY KEY,
    created_at       TIMESTAMP,
    updated_at       TIMESTAMP,
    created_by       VARCHAR(255),
    updated_by       VARCHAR(255),
    sigle            VARCHAR(255),
    raison_sociale   VARCHAR(255),
    ninea            VARCHAR(255),
    rccm             VARCHAR(255),
    adresse          VARCHAR(255),
    trial_used       BOOLEAN NOT NULL,
    proprietaire_id  UUID NOT NULL,
    CONSTRAINT uk_entreprise_proprietaire UNIQUE (proprietaire_id)
);

CREATE TABLE magasin (
    id            UUID PRIMARY KEY,
    created_at    TIMESTAMP,
    updated_at    TIMESTAMP,
    created_by    VARCHAR(255),
    updated_by    VARCHAR(255),
    nom           VARCHAR(255),
    adresse       VARCHAR(255),
    entreprise_id UUID
);

-- ============================ E. PRODUIT =====================================

CREATE TABLE category_product (
    id          UUID PRIMARY KEY,
    libelle     VARCHAR(255),
    description VARCHAR(255)
);

CREATE TABLE quality (
    id          UUID PRIMARY KEY,
    libelle     VARCHAR(255),
    description VARCHAR(255)
);

CREATE TABLE product (
    id                  UUID PRIMARY KEY,
    created_at          TIMESTAMP,
    updated_at          TIMESTAMP,
    created_by          VARCHAR(255),
    updated_by          VARCHAR(255),
    nom                 VARCHAR(255) NOT NULL,
    reference           VARCHAR(255) NOT NULL,
    description         VARCHAR(255),
    category_product_id UUID,
    quality_id          UUID,
    entreprise_id       UUID
);

CREATE TABLE product_fournisseur (
    id              UUID PRIMARY KEY,
    created_at      TIMESTAMP,
    updated_at      TIMESTAMP,
    created_by      VARCHAR(255),
    updated_by      VARCHAR(255),
    product_id      UUID,
    fournisseur_id  UUID,
    prix_achat      NUMERIC(19,2) NOT NULL
);

-- ============================ F. ABONNEMENT ==================================

CREATE TABLE plan_abonnement (
    id                      UUID PRIMARY KEY,
    created_at              TIMESTAMP,
    updated_at              TIMESTAMP,
    created_by              VARCHAR(255),
    updated_by              VARCHAR(255),
    nom                     VARCHAR(255) NOT NULL,
    description             VARCHAR(255),
    prix                    NUMERIC(19,2) NOT NULL,
    nombre_magasins_max     INTEGER NOT NULL,
    nombre_employes_max     INTEGER NOT NULL,
    gestion_stock           BOOLEAN NOT NULL,
    gestion_vente           BOOLEAN NOT NULL,
    gestion_achat           BOOLEAN NOT NULL,
    gestion_comptabilite    BOOLEAN NOT NULL,
    actif                   BOOLEAN NOT NULL,
    visible                 BOOLEAN NOT NULL,
    trial                   BOOLEAN NOT NULL,
    ordre                   INTEGER NOT NULL,
    CONSTRAINT uk_plan_abonnement_nom UNIQUE (nom)
);

CREATE TABLE type_abonnement (
    id                UUID PRIMARY KEY,
    created_at        TIMESTAMP,
    updated_at        TIMESTAMP,
    created_by        VARCHAR(255),
    updated_by        VARCHAR(255),
    nom               VARCHAR(255) NOT NULL,
    duree_mois        INTEGER NOT NULL,
    reduction_type    VARCHAR(255),
    valeur_reduction  NUMERIC(19,2),
    recommande        BOOLEAN NOT NULL,
    actif             BOOLEAN NOT NULL,
    ordre             INTEGER NOT NULL,
    CONSTRAINT uk_type_abonnement_nom UNIQUE (nom)
);

CREATE TABLE abonnement (
    id                  UUID PRIMARY KEY,
    created_at          TIMESTAMP,
    updated_at          TIMESTAMP,
    created_by          VARCHAR(255),
    updated_by          VARCHAR(255),
    entreprise_id       UUID,
    plan_id             UUID,
    type_abonnement_id  UUID,
    date_debut          DATE,
    date_fin            DATE,
    actif               BOOLEAN NOT NULL,
    renouvellement_auto BOOLEAN NOT NULL,
    statut              VARCHAR(255)
);

CREATE TABLE paiement_abonnement (
    id                       UUID PRIMARY KEY,
    created_at               TIMESTAMP,
    updated_at               TIMESTAMP,
    created_by               VARCHAR(255),
    updated_by               VARCHAR(255),
    abonnement_id            UUID,
    montant_avant_reduction  NUMERIC(19,2),
    reduction                NUMERIC(19,2),
    montant_final            NUMERIC(19,2),
    date_paiement            DATE,
    moyen                    VARCHAR(255),
    reference_transaction    VARCHAR(255)
);

CREATE TABLE coupon (
    id                       UUID PRIMARY KEY,
    created_at               TIMESTAMP,
    updated_at               TIMESTAMP,
    created_by               VARCHAR(255),
    updated_by               VARCHAR(255),
    code                     VARCHAR(255) NOT NULL,
    description              VARCHAR(255),
    reduction_type           VARCHAR(255),
    valeur_reduction         NUMERIC(19,2),
    nombre_utilisations_max  INTEGER NOT NULL,
    nombre_utilisations      INTEGER NOT NULL,
    date_debut               DATE,
    date_fin                 DATE,
    actif                    BOOLEAN NOT NULL,
    plan_id                  UUID,
    CONSTRAINT uk_coupon_code UNIQUE (code)
);

CREATE TABLE utilisation_coupon (
    id            UUID PRIMARY KEY,
    coupon_id     UUID,
    entreprise_id UUID,
    abonnement_id UUID
);

CREATE TABLE promotion (
    id                UUID PRIMARY KEY,
    created_at        TIMESTAMP,
    updated_at        TIMESTAMP,
    created_by        VARCHAR(255),
    updated_by        VARCHAR(255),
    nom               VARCHAR(255),
    description       VARCHAR(255),
    reduction_type    VARCHAR(255),
    valeur_reduction  NUMERIC(19,2),
    date_debut        DATE,
    date_fin          DATE,
    actif             BOOLEAN NOT NULL,
    plan_id           UUID
);

-- ============================ G. ACHAT =======================================

CREATE TABLE fournisseur (
    id        UUID PRIMARY KEY,
    reference VARCHAR(255),
    origine   VARCHAR(255)
);

CREATE TABLE commande_achat (
    id              UUID PRIMARY KEY,
    created_at      TIMESTAMP,
    updated_at      TIMESTAMP,
    created_by      VARCHAR(255),
    updated_by      VARCHAR(255),
    reference       VARCHAR(255),
    statut          VARCHAR(255),
    fournisseur_id  UUID,
    magasin_id      UUID,
    date            DATE,
    CONSTRAINT uk_commande_achat_reference UNIQUE (reference)
);

CREATE TABLE ligne_commande_achat (
    id                      UUID PRIMARY KEY,
    commande_id             UUID,
    product_fournisseur_id  UUID,
    quantite                INTEGER NOT NULL,
    prix_achat              NUMERIC(19,2)
);

CREATE TABLE facture_achat (
    id              UUID PRIMARY KEY,
    created_at      TIMESTAMP,
    updated_at      TIMESTAMP,
    created_by      VARCHAR(255),
    updated_by      VARCHAR(255),
    numero          VARCHAR(255),
    statut          VARCHAR(255),
    montant_total   NUMERIC(19,2),
    montant_paye    NUMERIC(19,2),
    commande_id     UUID,
    date            DATE,
    date_echeance   DATE,
    CONSTRAINT uk_facture_achat_numero UNIQUE (numero),
    CONSTRAINT uk_facture_achat_commande UNIQUE (commande_id)
);

CREATE TABLE paiement_achat (
    id              UUID PRIMARY KEY,
    created_at      TIMESTAMP,
    updated_at      TIMESTAMP,
    created_by      VARCHAR(255),
    updated_by      VARCHAR(255),
    montant         NUMERIC(19,2),
    date_paiement   DATE,
    moyen           VARCHAR(255),
    facture_id      UUID
);

-- ============================ H. STOCK =======================================

CREATE TABLE stock (
    id                        UUID PRIMARY KEY,
    created_at                TIMESTAMP,
    updated_at                TIMESTAMP,
    created_by                VARCHAR(255),
    updated_by                VARCHAR(255),
    magasin_id                UUID,
    produit_id                UUID,
    product_fournisseur_id    UUID,
    quantite_disponible       INTEGER NOT NULL,
    seuil_approvisionnement   INTEGER NOT NULL,
    prix_achat_moyen          NUMERIC(19,2)
);

CREATE TABLE entree_stock (
    id                       UUID PRIMARY KEY,
    created_at               TIMESTAMP,
    updated_at               TIMESTAMP,
    created_by               VARCHAR(255),
    updated_by               VARCHAR(255),
    produit_id               UUID,
    product_fournisseur_id   UUID,
    magasin_id               UUID,
    quantite_initiale        INTEGER NOT NULL,
    quantite_restante        INTEGER NOT NULL,
    prix_achat               NUMERIC(19,2),
    numero_lot               VARCHAR(255),
    date_expiration          DATE,
    commande_achat_id        UUID
);

CREATE TABLE sortie_stock (
    id              UUID PRIMARY KEY,
    created_at      TIMESTAMP,
    updated_at      TIMESTAMP,
    created_by      VARCHAR(255),
    updated_by      VARCHAR(255),
    ligne_vente_id  UUID,
    entree_stock_id UUID,
    quantite_sortie INTEGER NOT NULL,
    prix_achat      NUMERIC(19,2),
    prix_vente      NUMERIC(19,2),
    marge           NUMERIC(19,2)
);

CREATE TABLE mouvement_stock (
    id                  UUID PRIMARY KEY,
    created_at          TIMESTAMP,
    updated_at          TIMESTAMP,
    created_by          VARCHAR(255),
    updated_by          VARCHAR(255),
    stock_id            UUID,
    type                VARCHAR(255),
    quantite            INTEGER NOT NULL,
    stock_avant         INTEGER NOT NULL,
    stock_apres         INTEGER NOT NULL,
    reference_document  VARCHAR(255),
    commentaire         VARCHAR(255)
);

-- ============================ I. VENTE =======================================

CREATE TABLE client (
    id         UUID PRIMARY KEY,
    magasin_id UUID
);

CREATE TABLE commande_vente (
    id            UUID PRIMARY KEY,
    created_at    TIMESTAMP,
    updated_at    TIMESTAMP,
    created_by    VARCHAR(255),
    updated_by    VARCHAR(255),
    reference     VARCHAR(255),
    statut        VARCHAR(255),
    client_id     UUID,
    magasin_id    UUID,
    vendeur_id    UUID,
    montant_total NUMERIC(19,2),
    montant_paye  NUMERIC(19,2),
    date          DATE,
    CONSTRAINT uk_commande_vente_reference UNIQUE (reference)
);

CREATE TABLE ligne_commande_vente (
    id            UUID PRIMARY KEY,
    commande_id   UUID,
    product_id    UUID,
    quantite      INTEGER NOT NULL,
    prix_unitaire NUMERIC(19,2),
    montant_total NUMERIC(19,2)
);

CREATE TABLE facture_client (
    id              UUID PRIMARY KEY,
    created_at      TIMESTAMP,
    updated_at      TIMESTAMP,
    created_by      VARCHAR(255),
    updated_by      VARCHAR(255),
    numero          VARCHAR(255),
    statut          VARCHAR(255),
    commande_id     UUID,
    montant_total   NUMERIC(19,2),
    montant_paye    NUMERIC(19,2),
    date            DATE,
    date_echeache   DATE,
    CONSTRAINT uk_facture_client_numero UNIQUE (numero),
    CONSTRAINT uk_facture_client_commande UNIQUE (commande_id)
);

-- ============================ J. INVENTAIRE ==================================

CREATE TABLE inventaire (
    id              UUID PRIMARY KEY,
    created_at      TIMESTAMP,
    updated_at      TIMESTAMP,
    created_by      VARCHAR(255),
    updated_by      VARCHAR(255),
    magasin_id      UUID,
    date_inventaire DATE,
    statut          VARCHAR(255)
);

CREATE TABLE ligne_inventaire (
    id                 UUID PRIMARY KEY,
    created_at         TIMESTAMP,
    updated_at         TIMESTAMP,
    created_by         VARCHAR(255),
    updated_by         VARCHAR(255),
    inventaire_id      UUID,
    stock_id           UUID,
    quantite_systeme   INTEGER NOT NULL,
    quantite_physique  INTEGER NOT NULL,
    ecart              INTEGER NOT NULL
);

-- ============================ K. NOTIFICATION ================================

CREATE TABLE notification_template (
    id          UUID PRIMARY KEY,
    created_at  TIMESTAMP,
    updated_at  TIMESTAMP,
    created_by  VARCHAR(255),
    updated_by  VARCHAR(255),
    code        VARCHAR(255),
    sujet       VARCHAR(255),
    contenu     TEXT,
    canal       VARCHAR(255),
    actif       BOOLEAN NOT NULL,
    CONSTRAINT uk_notification_template_code UNIQUE (code)
);

CREATE TABLE echeance (
    id                    UUID PRIMARY KEY,
    created_at            TIMESTAMP,
    updated_at            TIMESTAMP,
    created_by            VARCHAR(255),
    updated_by            VARCHAR(255),
    abonnement_id         UUID,
    date_echeance         DATE,
    montant               NUMERIC(19,2),
    statut                VARCHAR(255),
    notification_envoyee  BOOLEAN NOT NULL,
    rappel_envoye         BOOLEAN NOT NULL,
    nombre_relances       INTEGER NOT NULL,
    dernier_rappel        DATE
);

CREATE TABLE notification (
    id                    UUID PRIMARY KEY,
    created_at            TIMESTAMP,
    updated_at            TIMESTAMP,
    created_by            VARCHAR(255),
    updated_by            VARCHAR(255),
    titre                 VARCHAR(255),
    message               TEXT,
    canal                 VARCHAR(255),
    statut                VARCHAR(255),
    date_envoi            TIMESTAMP,
    prochaine_tentative   DATE,
    nombre_tentatives     INTEGER NOT NULL,
    destinataire_id       UUID,
    facture_client_id     UUID,
    facture_achat_id      UUID,
    echeance_id           UUID
);

-- ============================ L. DEPENSE =====================================

CREATE TABLE category_depense (
    id          UUID PRIMARY KEY,
    created_at  TIMESTAMP,
    updated_at  TIMESTAMP,
    created_by  VARCHAR(255),
    updated_by  VARCHAR(255),
    nom         VARCHAR(255) NOT NULL,
    description VARCHAR(255),
    actif       BOOLEAN NOT NULL,
    CONSTRAINT uk_category_depense_nom UNIQUE (nom)
);

CREATE TABLE depense (
    id            UUID PRIMARY KEY,
    created_at    TIMESTAMP,
    updated_at    TIMESTAMP,
    created_by    VARCHAR(255),
    updated_by    VARCHAR(255),
    magasin_id    UUID,
    category_id   UUID,
    libelle       VARCHAR(255),
    description   TEXT,
    date_depense  DATE,
    date_echeance DATE,
    montant       NUMERIC(19,2) NOT NULL
);

-- =============================================================================
-- M. FOREIGN KEY CONSTRAINTS
-- =============================================================================

-- common
ALTER TABLE piece_jointe
    ADD CONSTRAINT fk_piece_jointe_product FOREIGN KEY (product_id) REFERENCES product (id);

-- security
ALTER TABLE role_permission
    ADD CONSTRAINT fk_role_permission_role       FOREIGN KEY (role_id)       REFERENCES role (id),
    ADD CONSTRAINT fk_role_permission_permission FOREIGN KEY (permission_id) REFERENCES permissions (id);

ALTER TABLE account
    ADD CONSTRAINT fk_account_role FOREIGN KEY (role_id) REFERENCES role (id);

ALTER TABLE refresh_token
    ADD CONSTRAINT fk_refresh_token_user FOREIGN KEY (user_id) REFERENCES utilisateur (id);

-- users (JOINED)
ALTER TABLE utilisateur
    ADD CONSTRAINT fk_utilisateur_person  FOREIGN KEY (id)         REFERENCES person (id),
    ADD CONSTRAINT fk_utilisateur_account FOREIGN KEY (account_id) REFERENCES account (id);

ALTER TABLE proprietaire
    ADD CONSTRAINT fk_proprietaire_utilisateur FOREIGN KEY (id) REFERENCES utilisateur (id);

ALTER TABLE employees
    ADD CONSTRAINT fk_employees_utilisateur FOREIGN KEY (id)         REFERENCES utilisateur (id),
    ADD CONSTRAINT fk_employees_magasin     FOREIGN KEY (magasin_id) REFERENCES magasin (id);

-- entreprise / magasin
ALTER TABLE entreprise
    ADD CONSTRAINT fk_entreprise_proprietaire FOREIGN KEY (proprietaire_id) REFERENCES proprietaire (id);

ALTER TABLE magasin
    ADD CONSTRAINT fk_magasin_entreprise FOREIGN KEY (entreprise_id) REFERENCES entreprise (id);

-- produit
ALTER TABLE product
    ADD CONSTRAINT fk_product_category    FOREIGN KEY (category_product_id) REFERENCES category_product (id),
    ADD CONSTRAINT fk_product_quality     FOREIGN KEY (quality_id)          REFERENCES quality (id),
    ADD CONSTRAINT fk_product_entreprise  FOREIGN KEY (entreprise_id)       REFERENCES entreprise (id);

ALTER TABLE product_fournisseur
    ADD CONSTRAINT fk_product_fournisseur_product     FOREIGN KEY (product_id)     REFERENCES product (id),
    ADD CONSTRAINT fk_product_fournisseur_fournisseur FOREIGN KEY (fournisseur_id) REFERENCES fournisseur (id);

-- abonnement
ALTER TABLE abonnement
    ADD CONSTRAINT fk_abonnement_entreprise FOREIGN KEY (entreprise_id)      REFERENCES entreprise (id),
    ADD CONSTRAINT fk_abonnement_plan       FOREIGN KEY (plan_id)            REFERENCES plan_abonnement (id),
    ADD CONSTRAINT fk_abonnement_type       FOREIGN KEY (type_abonnement_id) REFERENCES type_abonnement (id);

ALTER TABLE paiement_abonnement
    ADD CONSTRAINT fk_paiement_abonnement FOREIGN KEY (abonnement_id) REFERENCES abonnement (id);

ALTER TABLE coupon
    ADD CONSTRAINT fk_coupon_plan FOREIGN KEY (plan_id) REFERENCES plan_abonnement (id);

ALTER TABLE utilisation_coupon
    ADD CONSTRAINT fk_utilisation_coupon_coupon     FOREIGN KEY (coupon_id)     REFERENCES coupon (id),
    ADD CONSTRAINT fk_utilisation_coupon_entreprise FOREIGN KEY (entreprise_id) REFERENCES entreprise (id),
    ADD CONSTRAINT fk_utilisation_coupon_abonnement FOREIGN KEY (abonnement_id) REFERENCES abonnement (id);

ALTER TABLE promotion
    ADD CONSTRAINT fk_promotion_plan FOREIGN KEY (plan_id) REFERENCES plan_abonnement (id);

-- achat (JOINED fournisseur)
ALTER TABLE fournisseur
    ADD CONSTRAINT fk_fournisseur_person FOREIGN KEY (id) REFERENCES person (id);

ALTER TABLE commande_achat
    ADD CONSTRAINT fk_commande_achat_fournisseur FOREIGN KEY (fournisseur_id) REFERENCES fournisseur (id),
    ADD CONSTRAINT fk_commande_achat_magasin     FOREIGN KEY (magasin_id)     REFERENCES magasin (id);

ALTER TABLE ligne_commande_achat
    ADD CONSTRAINT fk_ligne_commande_achat_commande FOREIGN KEY (commande_id)            REFERENCES commande_achat (id),
    ADD CONSTRAINT fk_ligne_commande_achat_pf       FOREIGN KEY (product_fournisseur_id) REFERENCES product_fournisseur (id);

ALTER TABLE facture_achat
    ADD CONSTRAINT fk_facture_achat_commande FOREIGN KEY (commande_id) REFERENCES commande_achat (id);

ALTER TABLE paiement_achat
    ADD CONSTRAINT fk_paiement_achat_facture FOREIGN KEY (facture_id) REFERENCES facture_achat (id);

-- stock
ALTER TABLE stock
    ADD CONSTRAINT fk_stock_magasin FOREIGN KEY (magasin_id)             REFERENCES magasin (id),
    ADD CONSTRAINT fk_stock_product FOREIGN KEY (produit_id)             REFERENCES product (id),
    ADD CONSTRAINT fk_stock_pf      FOREIGN KEY (product_fournisseur_id) REFERENCES product_fournisseur (id);

ALTER TABLE entree_stock
    ADD CONSTRAINT fk_entree_stock_product        FOREIGN KEY (produit_id)             REFERENCES product (id),
    ADD CONSTRAINT fk_entree_stock_pf             FOREIGN KEY (product_fournisseur_id) REFERENCES product_fournisseur (id),
    ADD CONSTRAINT fk_entree_stock_magasin        FOREIGN KEY (magasin_id)             REFERENCES magasin (id),
    ADD CONSTRAINT fk_entree_stock_commande_achat FOREIGN KEY (commande_achat_id)      REFERENCES commande_achat (id);

ALTER TABLE sortie_stock
    ADD CONSTRAINT fk_sortie_stock_ligne_vente  FOREIGN KEY (ligne_vente_id)  REFERENCES ligne_commande_vente (id),
    ADD CONSTRAINT fk_sortie_stock_entree_stock FOREIGN KEY (entree_stock_id) REFERENCES entree_stock (id);

ALTER TABLE mouvement_stock
    ADD CONSTRAINT fk_mouvement_stock_stock FOREIGN KEY (stock_id) REFERENCES stock (id);

-- vente (JOINED client)
ALTER TABLE client
    ADD CONSTRAINT fk_client_person  FOREIGN KEY (id)         REFERENCES person (id),
    ADD CONSTRAINT fk_client_magasin FOREIGN KEY (magasin_id) REFERENCES magasin (id);

ALTER TABLE commande_vente
    ADD CONSTRAINT fk_commande_vente_client   FOREIGN KEY (client_id)  REFERENCES client (id),
    ADD CONSTRAINT fk_commande_vente_magasin  FOREIGN KEY (magasin_id) REFERENCES magasin (id),
    ADD CONSTRAINT fk_commande_vente_vendeur  FOREIGN KEY (vendeur_id) REFERENCES employees (id);

ALTER TABLE ligne_commande_vente
    ADD CONSTRAINT fk_ligne_commande_vente_commande FOREIGN KEY (commande_id) REFERENCES commande_vente (id),
    ADD CONSTRAINT fk_ligne_commande_vente_product  FOREIGN KEY (product_id)  REFERENCES product (id);

ALTER TABLE facture_client
    ADD CONSTRAINT fk_facture_client_commande FOREIGN KEY (commande_id) REFERENCES commande_vente (id);

-- inventaire
ALTER TABLE inventaire
    ADD CONSTRAINT fk_inventaire_magasin FOREIGN KEY (magasin_id) REFERENCES magasin (id);

ALTER TABLE ligne_inventaire
    ADD CONSTRAINT fk_ligne_inventaire_inventaire FOREIGN KEY (inventaire_id) REFERENCES inventaire (id),
    ADD CONSTRAINT fk_ligne_inventaire_stock      FOREIGN KEY (stock_id)      REFERENCES stock (id);

-- notification
ALTER TABLE echeance
    ADD CONSTRAINT fk_echeance_abonnement FOREIGN KEY (abonnement_id) REFERENCES abonnement (id);

ALTER TABLE notification
    ADD CONSTRAINT fk_notification_destinataire   FOREIGN KEY (destinataire_id)   REFERENCES account (id),
    ADD CONSTRAINT fk_notification_facture_client FOREIGN KEY (facture_client_id) REFERENCES facture_client (id),
    ADD CONSTRAINT fk_notification_facture_achat  FOREIGN KEY (facture_achat_id)  REFERENCES facture_achat (id),
    ADD CONSTRAINT fk_notification_echeance       FOREIGN KEY (echeance_id)       REFERENCES echeance (id);

-- depense
ALTER TABLE depense
    ADD CONSTRAINT fk_depense_magasin  FOREIGN KEY (magasin_id)  REFERENCES magasin (id),
    ADD CONSTRAINT fk_depense_category FOREIGN KEY (category_id) REFERENCES category_depense (id);
