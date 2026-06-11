-- =============================================================================
-- V1__init_schema.sql
-- Schema baseline complet (Postgres) du projet STORE.
--
-- Genere depuis le modele JPA via ddl-auto=create + pg_dump --schema-only.
-- Toute modification ulterieure du schema doit donner lieu a une nouvelle
-- migration incrementale (V2, V3, ...) ; ne plus reecrire V1.
--
-- Conventions:
--   * UUID natif Postgres.
--   * TIMESTAMP pour LocalDateTime, DATE pour LocalDate.
--   * NUMERIC(19,2) pour les montants BigDecimal.
--   * Heritage JOINED (Person -> Utilisateur -> Proprietaire/Employe) :
--     la PK de la sous-table est aussi FK vers la table parent.
--   * Contraintes (PK, UNIQUE, CHECK, FK) declarees en fin de fichier
--     via ALTER TABLE ONLY pour eviter les references avant-cycle.
-- =============================================================================

CREATE TABLE abonnement (
    actif boolean NOT NULL,
    date_debut date,
    date_fin date,
    renouvellement_auto boolean NOT NULL,
    created_at timestamp(6) without time zone,
    updated_at timestamp(6) without time zone,
    entreprise_id uuid,
    id uuid NOT NULL,
    plan_id uuid,
    type_abonnement_id uuid,
    created_by character varying(255),
    statut character varying(255),
    updated_by character varying(255),
    CONSTRAINT abonnement_statut_check CHECK (((statut)::text = ANY ((ARRAY['ACTIF'::character varying, 'EXPIRE'::character varying, 'SUSPENDU'::character varying, 'EN_ATTENTE'::character varying])::text[])))
);

CREATE TABLE account (
    enabled boolean NOT NULL,
    locked boolean NOT NULL,
    created_at timestamp(6) without time zone,
    creation_date timestamp(6) without time zone,
    updated_at timestamp(6) without time zone,
    id uuid NOT NULL,
    role_id uuid,
    created_by character varying(255),
    password character varying(255),
    updated_by character varying(255),
    username character varying(255)
);

CREATE TABLE category_depense (
    actif boolean NOT NULL,
    created_at timestamp(6) without time zone,
    updated_at timestamp(6) without time zone,
    entreprise_id uuid NOT NULL,
    id uuid NOT NULL,
    created_by character varying(255),
    description character varying(255),
    nom character varying(255),
    updated_by character varying(255)
);

CREATE TABLE category_product (
    entreprise_id uuid NOT NULL,
    id uuid NOT NULL,
    description character varying(255),
    libelle character varying(255) NOT NULL
);

CREATE TABLE client (
    id uuid NOT NULL,
    magasin_id uuid
);

CREATE TABLE commande_achat (
    date date,
    created_at timestamp(6) without time zone,
    date_annulation timestamp(6) without time zone,
    updated_at timestamp(6) without time zone,
    fournisseur_id uuid,
    id uuid NOT NULL,
    magasin_id uuid,
    motif_annulation character varying(30),
    commentaire_annulation text,
    created_by character varying(255),
    reference character varying(255),
    statut character varying(255),
    updated_by character varying(255),
    CONSTRAINT commande_achat_motif_annulation_check CHECK (((motif_annulation)::text = ANY ((ARRAY['ERREUR_SAISIE'::character varying, 'REFUS_FOURNISSEUR'::character varying, 'ARTICLE_DEFECTUEUX'::character varying, 'AUTRE'::character varying])::text[]))),
    CONSTRAINT commande_achat_statut_check CHECK (((statut)::text = ANY ((ARRAY['DRAFT'::character varying, 'VALIDEE'::character varying, 'PARTIELLEMENT_RECEPTIONNEE'::character varying, 'RECEPTIONNEE'::character varying, 'ANNULEE'::character varying])::text[])))
);

CREATE TABLE commande_vente (
    date date,
    created_at timestamp(6) without time zone,
    date_annulation timestamp(6) without time zone,
    updated_at timestamp(6) without time zone,
    client_id uuid,
    id uuid NOT NULL,
    magasin_id uuid,
    motif_annulation character varying(30),
    commentaire_annulation text,
    created_by character varying(255),
    reference character varying(255),
    statut character varying(255),
    updated_by character varying(255),
    CONSTRAINT commande_vente_motif_annulation_check CHECK (((motif_annulation)::text = ANY ((ARRAY['ERREUR_SAISIE'::character varying, 'REFUS_CLIENT'::character varying, 'ARTICLE_DEFECTUEUX'::character varying, 'AUTRE'::character varying])::text[]))),
    CONSTRAINT commande_vente_statut_check CHECK (((statut)::text = ANY ((ARRAY['DRAFT'::character varying, 'NOT_DELIVERED'::character varying, 'DELIVERED'::character varying, 'ANNULEE'::character varying])::text[])))
);

CREATE TABLE coupon (
    actif boolean NOT NULL,
    date_debut date,
    date_fin date,
    nombre_utilisations integer NOT NULL,
    nombre_utilisations_max integer NOT NULL,
    valeur_reduction numeric(19,2),
    created_at timestamp(6) without time zone,
    updated_at timestamp(6) without time zone,
    id uuid NOT NULL,
    plan_id uuid,
    code character varying(255) NOT NULL,
    created_by character varying(255),
    description character varying(255),
    reduction_type character varying(255),
    updated_by character varying(255),
    CONSTRAINT coupon_reduction_type_check CHECK (((reduction_type)::text = ANY ((ARRAY['POURCENTAGE'::character varying, 'MONTANT_FIXE'::character varying])::text[])))
);

CREATE TABLE depense (
    date_depense date,
    montant numeric(19,2) NOT NULL,
    created_at timestamp(6) without time zone,
    updated_at timestamp(6) without time zone,
    category_id uuid,
    id uuid NOT NULL,
    magasin_id uuid,
    mode_paiement character varying(20) NOT NULL,
    created_by character varying(255),
    description text,
    libelle character varying(255),
    updated_by character varying(255),
    CONSTRAINT depense_mode_paiement_check CHECK (((mode_paiement)::text = ANY ((ARRAY['CASH'::character varying, 'WAVE'::character varying, 'OM'::character varying, 'CARD'::character varying])::text[])))
);

CREATE TABLE echeance (
    date_echeance date,
    dernier_rappel date,
    montant numeric(19,2),
    nombre_relances integer NOT NULL,
    notification_envoyee boolean NOT NULL,
    rappel_envoye boolean NOT NULL,
    created_at timestamp(6) without time zone,
    updated_at timestamp(6) without time zone,
    abonnement_id uuid,
    id uuid NOT NULL,
    created_by character varying(255),
    statut character varying(255),
    updated_by character varying(255),
    CONSTRAINT echeance_statut_check CHECK (((statut)::text = ANY ((ARRAY['EN_ATTENTE'::character varying, 'PAYEE'::character varying, 'EXPIREE'::character varying])::text[])))
);

CREATE TABLE employees (
    id uuid NOT NULL,
    magasin_id uuid
);

CREATE TABLE entree_stock (
    annulee boolean NOT NULL,
    date_expiration date,
    prix_achat numeric(19,2),
    quantite_initiale integer NOT NULL,
    quantite_restante integer NOT NULL,
    created_at timestamp(6) without time zone,
    updated_at timestamp(6) without time zone,
    commande_achat_id uuid,
    id uuid NOT NULL,
    magasin_id uuid,
    product_fournisseur_id uuid,
    produit_id uuid,
    created_by character varying(255),
    numero_lot character varying(255),
    updated_by character varying(255)
);

CREATE TABLE entreprise (
    actif boolean NOT NULL,
    trial_used boolean NOT NULL,
    created_at timestamp(6) without time zone,
    updated_at timestamp(6) without time zone,
    id uuid NOT NULL,
    logo_id uuid,
    proprietaire_id uuid NOT NULL,
    adresse character varying(255),
    created_by character varying(255),
    ninea character varying(255),
    raison_sociale character varying(255),
    rccm character varying(255),
    sigle character varying(255),
    updated_by character varying(255)
);

CREATE TABLE facture_achat (
    date date,
    date_echeance date,
    montant_paye numeric(19,2),
    montant_total numeric(19,2),
    created_at timestamp(6) without time zone,
    updated_at timestamp(6) without time zone,
    commande_id uuid,
    id uuid NOT NULL,
    created_by character varying(255),
    numero character varying(255),
    statut character varying(255),
    updated_by character varying(255),
    CONSTRAINT facture_achat_statut_check CHECK (((statut)::text = ANY ((ARRAY['NON_PAYEE'::character varying, 'PARTIELLEMENT_PAYEE'::character varying, 'PAYEE'::character varying, 'ANNULEE'::character varying])::text[])))
);

CREATE TABLE facture_client (
    date date,
    date_echeance date NOT NULL,
    montant_paye numeric(19,2),
    montant_total numeric(19,2),
    created_at timestamp(6) without time zone,
    updated_at timestamp(6) without time zone,
    commande_id uuid,
    id uuid NOT NULL,
    created_by character varying(255),
    numero character varying(255),
    statut character varying(255),
    updated_by character varying(255),
    CONSTRAINT facture_client_statut_check CHECK (((statut)::text = ANY ((ARRAY['NON_PAYEE'::character varying, 'PARTIELLEMENT_PAYEE'::character varying, 'PAYEE'::character varying, 'ANNULEE'::character varying])::text[])))
);

CREATE TABLE fournisseur (
    entreprise_id uuid NOT NULL,
    id uuid NOT NULL,
    origine character varying(255),
    reference character varying(255)
);

CREATE TABLE inventaire (
    date date NOT NULL,
    created_at timestamp(6) without time zone,
    date_validation timestamp(6) without time zone,
    updated_at timestamp(6) without time zone,
    id uuid NOT NULL,
    magasin_id uuid NOT NULL,
    statut character varying(20) NOT NULL,
    created_by character varying(255),
    updated_by character varying(255),
    CONSTRAINT inventaire_statut_check CHECK (((statut)::text = ANY ((ARRAY['EN_COURS'::character varying, 'BILAN'::character varying, 'CLOTURE'::character varying, 'ANNULE'::character varying])::text[])))
);

CREATE TABLE ligne_commande_achat (
    date_expiration date,
    prix_achat numeric(19,2),
    prix_vente numeric(19,2) NOT NULL,
    quantite integer NOT NULL,
    quantite_recue integer NOT NULL DEFAULT 0,
    commande_id uuid,
    id uuid NOT NULL,
    product_fournisseur_id uuid,
    numero_lot character varying(100)
);

CREATE TABLE ligne_commande_vente (
    montant_total numeric(19,2),
    prix_unitaire numeric(19,2),
    quantite integer NOT NULL,
    commande_id uuid,
    id uuid NOT NULL,
    product_fournisseur_id uuid NOT NULL,
    product_id uuid
);

CREATE TABLE ligne_inventaire (
    ecart integer NOT NULL,
    quantite_reelle integer NOT NULL,
    quantite_theorique integer NOT NULL,
    id uuid NOT NULL,
    inventaire_id uuid NOT NULL,
    product_fournisseur_id uuid NOT NULL
);

CREATE TABLE magasin (
    actif boolean NOT NULL,
    created_at timestamp(6) without time zone,
    updated_at timestamp(6) without time zone,
    entreprise_id uuid,
    id uuid NOT NULL,
    logo_id uuid,
    adresse character varying(255),
    created_by character varying(255),
    nom character varying(255),
    updated_by character varying(255)
);

CREATE TABLE mouvement_stock (
    quantite integer NOT NULL,
    stock_apres integer NOT NULL,
    stock_avant integer NOT NULL,
    created_at timestamp(6) without time zone,
    updated_at timestamp(6) without time zone,
    id uuid NOT NULL,
    stock_id uuid,
    commentaire character varying(255),
    created_by character varying(255),
    reference_document character varying(255),
    type character varying(255),
    updated_by character varying(255),
    CONSTRAINT mouvement_stock_type_check CHECK (((type)::text = ANY ((ARRAY['ENTREE_ACHAT'::character varying, 'SORTIE_VENTE'::character varying, 'INVENTAIRE'::character varying, 'AJUSTEMENT'::character varying, 'RETOUR_CLIENT'::character varying, 'RETOUR_FOURNISSEUR'::character varying])::text[])))
);

CREATE TABLE notification (
    nombre_tentatives integer NOT NULL,
    prochaine_tentative date,
    created_at timestamp(6) without time zone,
    date_envoi timestamp(6) without time zone,
    updated_at timestamp(6) without time zone,
    destinataire_id uuid,
    echeance_id uuid,
    facture_achat_id uuid,
    facture_client_id uuid,
    id uuid NOT NULL,
    canal character varying(255),
    created_by character varying(255),
    message text,
    statut character varying(255),
    titre character varying(255),
    updated_by character varying(255),
    CONSTRAINT notification_canal_check CHECK (((canal)::text = ANY ((ARRAY['EMAIL'::character varying, 'SMS'::character varying, 'PUSH'::character varying, 'IN_APP'::character varying])::text[]))),
    CONSTRAINT notification_statut_check CHECK (((statut)::text = ANY ((ARRAY['EN_ATTENTE'::character varying, 'ENVOYEE'::character varying, 'ECHEC'::character varying, 'LUE'::character varying])::text[])))
);

CREATE TABLE notification_template (
    actif boolean NOT NULL,
    created_at timestamp(6) without time zone,
    updated_at timestamp(6) without time zone,
    id uuid NOT NULL,
    canal character varying(255),
    code character varying(255),
    contenu text,
    created_by character varying(255),
    sujet character varying(255),
    updated_by character varying(255),
    CONSTRAINT notification_template_canal_check CHECK (((canal)::text = ANY ((ARRAY['EMAIL'::character varying, 'SMS'::character varying, 'PUSH'::character varying, 'IN_APP'::character varying])::text[])))
);

CREATE TABLE paiement_abonnement (
    date_paiement date,
    montant_avant_reduction numeric(19,2),
    montant_final numeric(19,2),
    reduction numeric(19,2),
    created_at timestamp(6) without time zone,
    updated_at timestamp(6) without time zone,
    abonnement_id uuid,
    id uuid NOT NULL,
    preuve_id uuid,
    statut character varying(50) NOT NULL,
    created_by character varying(255),
    motif_rejet text,
    moyen character varying(255),
    reference_transaction character varying(255),
    updated_by character varying(255),
    CONSTRAINT paiement_abonnement_moyen_check CHECK (((moyen)::text = ANY ((ARRAY['CASH'::character varying, 'WAVE'::character varying, 'OM'::character varying, 'CARD'::character varying])::text[]))),
    CONSTRAINT paiement_abonnement_statut_check CHECK (((statut)::text = ANY ((ARRAY['EN_ATTENTE_VALIDATION'::character varying, 'VALIDE'::character varying, 'REJETE'::character varying])::text[])))
);

CREATE TABLE paiement_achat (
    date_paiement date,
    montant numeric(19,2),
    created_at timestamp(6) without time zone,
    updated_at timestamp(6) without time zone,
    facture_id uuid,
    id uuid NOT NULL,
    created_by character varying(255),
    moyen character varying(255),
    updated_by character varying(255),
    CONSTRAINT paiement_achat_moyen_check CHECK (((moyen)::text = ANY ((ARRAY['CASH'::character varying, 'WAVE'::character varying, 'OM'::character varying, 'CARD'::character varying])::text[])))
);

CREATE TABLE paiement_vente (
    date_paiement date NOT NULL,
    montant numeric(19,2) NOT NULL,
    created_at timestamp(6) without time zone,
    updated_at timestamp(6) without time zone,
    facture_id uuid NOT NULL,
    id uuid NOT NULL,
    moyen character varying(20) NOT NULL,
    created_by character varying(255),
    updated_by character varying(255),
    CONSTRAINT paiement_vente_moyen_check CHECK (((moyen)::text = ANY ((ARRAY['CASH'::character varying, 'WAVE'::character varying, 'OM'::character varying, 'CARD'::character varying])::text[])))
);

CREATE TABLE permissions (
    id uuid NOT NULL,
    code character varying(255)
);

CREATE TABLE person (
    created_at timestamp(6) without time zone,
    updated_at timestamp(6) without time zone,
    id uuid NOT NULL,
    adresse character varying(255),
    created_by character varying(255),
    email character varying(255),
    nom character varying(255),
    prenom character varying(255),
    telephone character varying(255),
    updated_by character varying(255)
);

CREATE TABLE piece_jointe (
    date date,
    id uuid NOT NULL,
    product_id uuid,
    content_type character varying(100) NOT NULL,
    document oid
);

CREATE TABLE plan_abonnement (
    actif boolean NOT NULL,
    gestion_achat boolean NOT NULL,
    gestion_comptabilite boolean NOT NULL,
    gestion_stock boolean NOT NULL,
    gestion_vente boolean NOT NULL,
    nombre_employes_max integer NOT NULL,
    nombre_magasins_max integer NOT NULL,
    ordre integer NOT NULL,
    prix numeric(19,2) NOT NULL,
    trial boolean NOT NULL,
    visible boolean NOT NULL,
    created_at timestamp(6) without time zone,
    updated_at timestamp(6) without time zone,
    id uuid NOT NULL,
    created_by character varying(255),
    description character varying(255),
    nom character varying(255) NOT NULL,
    updated_by character varying(255)
);

CREATE TABLE product (
    created_at timestamp(6) without time zone,
    updated_at timestamp(6) without time zone,
    category_product_id uuid,
    entreprise_id uuid,
    id uuid NOT NULL,
    image_principal_id uuid,
    created_by character varying(255),
    description character varying(255),
    nom character varying(255) NOT NULL,
    reference character varying(255) NOT NULL,
    updated_by character varying(255)
);

CREATE TABLE product_fournisseur (
    prix_achat numeric(19,2) NOT NULL,
    prix_vente numeric(19,2) NOT NULL,
    created_at timestamp(6) without time zone,
    updated_at timestamp(6) without time zone,
    fournisseur_id uuid NOT NULL,
    id uuid NOT NULL,
    product_id uuid NOT NULL,
    quality_id uuid NOT NULL,
    origine character varying(100),
    reference_fournisseur character varying(100),
    created_by character varying(255),
    updated_by character varying(255)
);

CREATE TABLE promotion (
    actif boolean NOT NULL,
    date_debut date,
    date_fin date,
    valeur_reduction numeric(19,2),
    created_at timestamp(6) without time zone,
    updated_at timestamp(6) without time zone,
    id uuid NOT NULL,
    plan_id uuid,
    created_by character varying(255),
    description character varying(255),
    nom character varying(255),
    reduction_type character varying(255),
    updated_by character varying(255),
    CONSTRAINT promotion_reduction_type_check CHECK (((reduction_type)::text = ANY ((ARRAY['POURCENTAGE'::character varying, 'MONTANT_FIXE'::character varying])::text[])))
);

CREATE TABLE proprietaire (
    id uuid NOT NULL
);

CREATE TABLE quality (
    entreprise_id uuid NOT NULL,
    id uuid NOT NULL,
    description character varying(255),
    libelle character varying(255) NOT NULL
);

CREATE TABLE rapport_inventaire (
    benefice numeric(19,2) NOT NULL,
    date_debut_periode date NOT NULL,
    date_fin_periode date NOT NULL,
    depense numeric(19,2) NOT NULL,
    ecart numeric(19,2) NOT NULL,
    montant_automatique numeric(19,2) NOT NULL,
    montant_caisse numeric(19,2) NOT NULL,
    montant_physique numeric(19,2) NOT NULL,
    montant_roulement numeric(19,2) NOT NULL,
    created_at timestamp(6) without time zone,
    updated_at timestamp(6) without time zone,
    id uuid NOT NULL,
    inventaire_id uuid NOT NULL,
    status character varying(20) NOT NULL,
    created_by character varying(255),
    updated_by character varying(255),
    CONSTRAINT rapport_inventaire_status_check CHECK (((status)::text = ANY ((ARRAY['BENEFICE'::character varying, 'PERTE'::character varying, 'EQUILIBRE'::character varying])::text[])))
);

CREATE TABLE refresh_token (
    revoked boolean NOT NULL,
    created_at timestamp(6) without time zone,
    expiry_date timestamp(6) without time zone,
    updated_at timestamp(6) without time zone,
    id uuid NOT NULL,
    user_id uuid,
    created_by character varying(255),
    replaced_by_token character varying(255),
    token character varying(255),
    updated_by character varying(255)
);

CREATE TABLE role (
    id uuid NOT NULL,
    description character varying(255),
    libelle character varying(255)
);

CREATE TABLE role_permission (
    permission_id uuid NOT NULL,
    role_id uuid NOT NULL
);

CREATE TABLE sortie_stock (
    annulee boolean NOT NULL,
    marge numeric(19,2),
    prix_achat numeric(19,2),
    prix_vente numeric(19,2),
    quantite_sortie integer NOT NULL,
    created_at timestamp(6) without time zone,
    updated_at timestamp(6) without time zone,
    entree_stock_id uuid,
    id uuid NOT NULL,
    ligne_vente_id uuid,
    created_by character varying(255),
    updated_by character varying(255)
);

CREATE TABLE stock (
    prix_achat_moyen numeric(19,2),
    quantite_disponible integer NOT NULL,
    seuil_approvisionnement integer NOT NULL,
    created_at timestamp(6) without time zone,
    updated_at timestamp(6) without time zone,
    id uuid NOT NULL,
    magasin_id uuid NOT NULL,
    produit_id uuid NOT NULL,
    created_by character varying(255),
    updated_by character varying(255)
);

CREATE TABLE type_abonnement (
    actif boolean NOT NULL,
    duree_mois integer NOT NULL,
    ordre integer NOT NULL,
    recommande boolean NOT NULL,
    valeur_reduction numeric(19,2),
    created_at timestamp(6) without time zone,
    updated_at timestamp(6) without time zone,
    id uuid NOT NULL,
    created_by character varying(255),
    nom character varying(255) NOT NULL,
    reduction_type character varying(255),
    updated_by character varying(255),
    CONSTRAINT type_abonnement_reduction_type_check CHECK (((reduction_type)::text = ANY ((ARRAY['POURCENTAGE'::character varying, 'MONTANT_FIXE'::character varying])::text[])))
);

CREATE TABLE utilisateur (
    account_id uuid NOT NULL,
    id uuid NOT NULL,
    photo_id uuid
);

CREATE TABLE utilisation_coupon (
    abonnement_id uuid,
    coupon_id uuid,
    entreprise_id uuid,
    id uuid NOT NULL
);

ALTER TABLE ONLY abonnement
    ADD CONSTRAINT abonnement_pkey PRIMARY KEY (id);

ALTER TABLE ONLY account
    ADD CONSTRAINT account_pkey PRIMARY KEY (id);

ALTER TABLE ONLY category_depense
    ADD CONSTRAINT category_depense_pkey PRIMARY KEY (id);

ALTER TABLE ONLY category_product
    ADD CONSTRAINT category_product_pkey PRIMARY KEY (id);

ALTER TABLE ONLY client
    ADD CONSTRAINT client_pkey PRIMARY KEY (id);

ALTER TABLE ONLY commande_achat
    ADD CONSTRAINT commande_achat_pkey PRIMARY KEY (id);

ALTER TABLE ONLY commande_achat
    ADD CONSTRAINT commande_achat_reference_key UNIQUE (reference);

ALTER TABLE ONLY commande_vente
    ADD CONSTRAINT commande_vente_pkey PRIMARY KEY (id);

ALTER TABLE ONLY commande_vente
    ADD CONSTRAINT commande_vente_reference_key UNIQUE (reference);

ALTER TABLE ONLY coupon
    ADD CONSTRAINT coupon_code_key UNIQUE (code);

ALTER TABLE ONLY coupon
    ADD CONSTRAINT coupon_pkey PRIMARY KEY (id);

ALTER TABLE ONLY depense
    ADD CONSTRAINT depense_pkey PRIMARY KEY (id);

ALTER TABLE ONLY echeance
    ADD CONSTRAINT echeance_pkey PRIMARY KEY (id);

ALTER TABLE ONLY employees
    ADD CONSTRAINT employees_pkey PRIMARY KEY (id);

ALTER TABLE ONLY entree_stock
    ADD CONSTRAINT entree_stock_pkey PRIMARY KEY (id);

ALTER TABLE ONLY entreprise
    ADD CONSTRAINT entreprise_logo_id_key UNIQUE (logo_id);

ALTER TABLE ONLY entreprise
    ADD CONSTRAINT entreprise_pkey PRIMARY KEY (id);

ALTER TABLE ONLY entreprise
    ADD CONSTRAINT entreprise_proprietaire_id_key UNIQUE (proprietaire_id);

ALTER TABLE ONLY facture_achat
    ADD CONSTRAINT facture_achat_commande_id_key UNIQUE (commande_id);

ALTER TABLE ONLY facture_achat
    ADD CONSTRAINT facture_achat_numero_key UNIQUE (numero);

ALTER TABLE ONLY facture_achat
    ADD CONSTRAINT facture_achat_pkey PRIMARY KEY (id);

ALTER TABLE ONLY facture_client
    ADD CONSTRAINT facture_client_commande_id_key UNIQUE (commande_id);

ALTER TABLE ONLY facture_client
    ADD CONSTRAINT facture_client_numero_key UNIQUE (numero);

ALTER TABLE ONLY facture_client
    ADD CONSTRAINT facture_client_pkey PRIMARY KEY (id);

ALTER TABLE ONLY fournisseur
    ADD CONSTRAINT fournisseur_pkey PRIMARY KEY (id);

ALTER TABLE ONLY inventaire
    ADD CONSTRAINT inventaire_pkey PRIMARY KEY (id);

ALTER TABLE ONLY ligne_commande_achat
    ADD CONSTRAINT ligne_commande_achat_pkey PRIMARY KEY (id);

ALTER TABLE ONLY ligne_commande_vente
    ADD CONSTRAINT ligne_commande_vente_pkey PRIMARY KEY (id);

ALTER TABLE ONLY ligne_inventaire
    ADD CONSTRAINT ligne_inventaire_pkey PRIMARY KEY (id);

ALTER TABLE ONLY magasin
    ADD CONSTRAINT magasin_logo_id_key UNIQUE (logo_id);

ALTER TABLE ONLY magasin
    ADD CONSTRAINT magasin_pkey PRIMARY KEY (id);

ALTER TABLE ONLY mouvement_stock
    ADD CONSTRAINT mouvement_stock_pkey PRIMARY KEY (id);

ALTER TABLE ONLY notification
    ADD CONSTRAINT notification_pkey PRIMARY KEY (id);

ALTER TABLE ONLY notification_template
    ADD CONSTRAINT notification_template_code_key UNIQUE (code);

ALTER TABLE ONLY notification_template
    ADD CONSTRAINT notification_template_pkey PRIMARY KEY (id);

ALTER TABLE ONLY paiement_abonnement
    ADD CONSTRAINT paiement_abonnement_pkey PRIMARY KEY (id);

ALTER TABLE ONLY paiement_abonnement
    ADD CONSTRAINT paiement_abonnement_preuve_id_key UNIQUE (preuve_id);

ALTER TABLE ONLY paiement_achat
    ADD CONSTRAINT paiement_achat_pkey PRIMARY KEY (id);

ALTER TABLE ONLY paiement_vente
    ADD CONSTRAINT paiement_vente_pkey PRIMARY KEY (id);

ALTER TABLE ONLY permissions
    ADD CONSTRAINT permissions_pkey PRIMARY KEY (id);

ALTER TABLE ONLY person
    ADD CONSTRAINT person_email_key UNIQUE (email);

ALTER TABLE ONLY person
    ADD CONSTRAINT person_pkey PRIMARY KEY (id);

ALTER TABLE ONLY person
    ADD CONSTRAINT person_telephone_key UNIQUE (telephone);

ALTER TABLE ONLY piece_jointe
    ADD CONSTRAINT piece_jointe_pkey PRIMARY KEY (id);

ALTER TABLE ONLY plan_abonnement
    ADD CONSTRAINT plan_abonnement_nom_key UNIQUE (nom);

ALTER TABLE ONLY plan_abonnement
    ADD CONSTRAINT plan_abonnement_pkey PRIMARY KEY (id);

ALTER TABLE ONLY product_fournisseur
    ADD CONSTRAINT product_fournisseur_pkey PRIMARY KEY (id);

ALTER TABLE ONLY product
    ADD CONSTRAINT product_image_principal_id_key UNIQUE (image_principal_id);

ALTER TABLE ONLY product
    ADD CONSTRAINT product_pkey PRIMARY KEY (id);

ALTER TABLE ONLY promotion
    ADD CONSTRAINT promotion_pkey PRIMARY KEY (id);

ALTER TABLE ONLY proprietaire
    ADD CONSTRAINT proprietaire_pkey PRIMARY KEY (id);

ALTER TABLE ONLY quality
    ADD CONSTRAINT quality_pkey PRIMARY KEY (id);

ALTER TABLE ONLY rapport_inventaire
    ADD CONSTRAINT rapport_inventaire_inventaire_id_key UNIQUE (inventaire_id);

ALTER TABLE ONLY rapport_inventaire
    ADD CONSTRAINT rapport_inventaire_pkey PRIMARY KEY (id);

ALTER TABLE ONLY refresh_token
    ADD CONSTRAINT refresh_token_pkey PRIMARY KEY (id);

ALTER TABLE ONLY role_permission
    ADD CONSTRAINT role_permission_pkey PRIMARY KEY (permission_id, role_id);

ALTER TABLE ONLY role
    ADD CONSTRAINT role_pkey PRIMARY KEY (id);

ALTER TABLE ONLY sortie_stock
    ADD CONSTRAINT sortie_stock_pkey PRIMARY KEY (id);

ALTER TABLE ONLY stock
    ADD CONSTRAINT stock_pkey PRIMARY KEY (id);

ALTER TABLE ONLY type_abonnement
    ADD CONSTRAINT type_abonnement_nom_key UNIQUE (nom);

ALTER TABLE ONLY type_abonnement
    ADD CONSTRAINT type_abonnement_pkey PRIMARY KEY (id);

ALTER TABLE ONLY category_depense
    ADD CONSTRAINT uk_category_depense_entreprise_nom UNIQUE (entreprise_id, nom);

ALTER TABLE ONLY stock
    ADD CONSTRAINT uk_stock_magasin_produit UNIQUE (magasin_id, produit_id);

ALTER TABLE ONLY utilisateur
    ADD CONSTRAINT utilisateur_account_id_key UNIQUE (account_id);

ALTER TABLE ONLY utilisateur
    ADD CONSTRAINT utilisateur_photo_id_key UNIQUE (photo_id);

ALTER TABLE ONLY utilisateur
    ADD CONSTRAINT utilisateur_pkey PRIMARY KEY (id);

ALTER TABLE ONLY utilisation_coupon
    ADD CONSTRAINT utilisation_coupon_pkey PRIMARY KEY (id);

ALTER TABLE ONLY promotion
    ADD CONSTRAINT fk18sq3qv8maxr6sri8ngui8wc4 FOREIGN KEY (plan_id) REFERENCES plan_abonnement(id);

ALTER TABLE ONLY stock
    ADD CONSTRAINT fk2784xu0813tbhvtve3rhbrqkf FOREIGN KEY (magasin_id) REFERENCES magasin(id);

ALTER TABLE ONLY ligne_inventaire
    ADD CONSTRAINT fk2kf3eq52tgvhein8gd2jxyrej FOREIGN KEY (inventaire_id) REFERENCES inventaire(id);

ALTER TABLE ONLY commande_vente
    ADD CONSTRAINT fk2mwgbyc0ngyjoebbw25jv9fvl FOREIGN KEY (magasin_id) REFERENCES magasin(id);

ALTER TABLE ONLY role_permission
    ADD CONSTRAINT fk2xn8qv4vw30i04xdxrpvn3bdi FOREIGN KEY (permission_id) REFERENCES permissions(id);

ALTER TABLE ONLY utilisateur
    ADD CONSTRAINT fk307mxeawsr7sce1nu3npky7d0 FOREIGN KEY (id) REFERENCES person(id);

ALTER TABLE ONLY product
    ADD CONSTRAINT fk43bykbdlgu80xil81boke1cmb FOREIGN KEY (image_principal_id) REFERENCES piece_jointe(id);

ALTER TABLE ONLY entreprise
    ADD CONSTRAINT fk4wonokv2r1tyinu88aeov7k8b FOREIGN KEY (logo_id) REFERENCES piece_jointe(id);

ALTER TABLE ONLY coupon
    ADD CONSTRAINT fk56mwqtfmt189qqfbipn3m18yt FOREIGN KEY (plan_id) REFERENCES plan_abonnement(id);

ALTER TABLE ONLY fournisseur
    ADD CONSTRAINT fk5ihggswnqinggb8nk2yhfke7l FOREIGN KEY (entreprise_id) REFERENCES entreprise(id);

ALTER TABLE ONLY facture_achat
    ADD CONSTRAINT fk6e7c9gfynppy0xiho361qpaeo FOREIGN KEY (commande_id) REFERENCES commande_achat(id);

ALTER TABLE ONLY notification
    ADD CONSTRAINT fk6g5hf86rljcbv51xilqs9gmc3 FOREIGN KEY (echeance_id) REFERENCES echeance(id);

ALTER TABLE ONLY paiement_abonnement
    ADD CONSTRAINT fk6ho8o0jb1hp60nhmr27tq78r2 FOREIGN KEY (abonnement_id) REFERENCES abonnement(id);

ALTER TABLE ONLY quality
    ADD CONSTRAINT fk6ms7yvps37ga4iu6imcwb3so1 FOREIGN KEY (entreprise_id) REFERENCES entreprise(id);

ALTER TABLE ONLY notification
    ADD CONSTRAINT fk7xqjp9353yrfnywr4maexeutp FOREIGN KEY (destinataire_id) REFERENCES account(id);

ALTER TABLE ONLY entree_stock
    ADD CONSTRAINT fk8aafuecw84s0en05eu88vuiyl FOREIGN KEY (product_fournisseur_id) REFERENCES product_fournisseur(id);

ALTER TABLE ONLY commande_vente
    ADD CONSTRAINT fk8muli53puye75s2wwf3u8owlg FOREIGN KEY (client_id) REFERENCES client(id);

ALTER TABLE ONLY ligne_commande_vente
    ADD CONSTRAINT fk909isyjxxysoxir0w0vnjlvwk FOREIGN KEY (commande_id) REFERENCES commande_vente(id);

ALTER TABLE ONLY inventaire
    ADD CONSTRAINT fk91ln5r50tqojbu3xbx1p2f184 FOREIGN KEY (magasin_id) REFERENCES magasin(id);

ALTER TABLE ONLY utilisation_coupon
    ADD CONSTRAINT fk93v27ndmr3cu6dj4hre8e46vu FOREIGN KEY (coupon_id) REFERENCES coupon(id);

ALTER TABLE ONLY product_fournisseur
    ADD CONSTRAINT fk9qn7siyii63l2awrap5q19tha FOREIGN KEY (quality_id) REFERENCES quality(id);

ALTER TABLE ONLY notification
    ADD CONSTRAINT fka2i124e169jk64heipxabi7c6 FOREIGN KEY (facture_achat_id) REFERENCES facture_achat(id);

ALTER TABLE ONLY role_permission
    ADD CONSTRAINT fka6jx8n8xkesmjmv6jqug6bg68 FOREIGN KEY (role_id) REFERENCES role(id);

ALTER TABLE ONLY product
    ADD CONSTRAINT fkajpbtihjrbl27dg3w1anvnr88 FOREIGN KEY (entreprise_id) REFERENCES entreprise(id);

ALTER TABLE ONLY entreprise
    ADD CONSTRAINT fkakunaehowdgo5b1nr5gs1m2v1 FOREIGN KEY (proprietaire_id) REFERENCES proprietaire(id);

ALTER TABLE ONLY category_product
    ADD CONSTRAINT fkb0b8nthv79tocphs5yr9cm0t1 FOREIGN KEY (entreprise_id) REFERENCES entreprise(id);

ALTER TABLE ONLY product_fournisseur
    ADD CONSTRAINT fkbku89gwb4w14lhxlgxgoifwvd FOREIGN KEY (product_id) REFERENCES product(id);

ALTER TABLE ONLY utilisation_coupon
    ADD CONSTRAINT fkbyx2m2luemtdpv84wskor1088 FOREIGN KEY (abonnement_id) REFERENCES abonnement(id);

ALTER TABLE ONLY employees
    ADD CONSTRAINT fkcbckf402v1ol6dlp2ugmb7vq FOREIGN KEY (id) REFERENCES utilisateur(id);

ALTER TABLE ONLY account
    ADD CONSTRAINT fkd4vb66o896tay3yy52oqxr9w0 FOREIGN KEY (role_id) REFERENCES role(id);

ALTER TABLE ONLY ligne_commande_achat
    ADD CONSTRAINT fkdbwp8an9tlmplk9ptoe67rcie FOREIGN KEY (product_fournisseur_id) REFERENCES product_fournisseur(id);

ALTER TABLE ONLY depense
    ADD CONSTRAINT fkdr5lmj6jereygqrep7ro4xdrc FOREIGN KEY (magasin_id) REFERENCES magasin(id);

ALTER TABLE ONLY commande_achat
    ADD CONSTRAINT fke2fyrm2ry11ekrqr7vx33j68l FOREIGN KEY (magasin_id) REFERENCES magasin(id);

ALTER TABLE ONLY notification
    ADD CONSTRAINT fkethn5q9doxflvlgnmcpst4v80 FOREIGN KEY (facture_client_id) REFERENCES facture_client(id);

ALTER TABLE ONLY sortie_stock
    ADD CONSTRAINT fkeu9xpej2pjo9ycvvo229k86uj FOREIGN KEY (entree_stock_id) REFERENCES entree_stock(id);

ALTER TABLE ONLY abonnement
    ADD CONSTRAINT fkfjc6qrp3tpg10uhb99q786dyp FOREIGN KEY (type_abonnement_id) REFERENCES type_abonnement(id);

ALTER TABLE ONLY paiement_abonnement
    ADD CONSTRAINT fkfrjp7w0p87f5j47u5ybekcw23 FOREIGN KEY (preuve_id) REFERENCES piece_jointe(id);

ALTER TABLE ONLY ligne_commande_vente
    ADD CONSTRAINT fkglk2r7ih1e9o36g0soa5xdkvw FOREIGN KEY (product_fournisseur_id) REFERENCES product_fournisseur(id);

ALTER TABLE ONLY ligne_commande_achat
    ADD CONSTRAINT fkgyvwcjjq4m0uxftoo0tvopv1s FOREIGN KEY (commande_id) REFERENCES commande_achat(id);

ALTER TABLE ONLY employees
    ADD CONSTRAINT fkh2sn6vie08h2iv64fkgoerrhg FOREIGN KEY (magasin_id) REFERENCES magasin(id);

ALTER TABLE ONLY utilisation_coupon
    ADD CONSTRAINT fkhggbqtung5mf7mqgf5954rf12 FOREIGN KEY (entreprise_id) REFERENCES entreprise(id);

ALTER TABLE ONLY client
    ADD CONSTRAINT fkhpsk6nnlcko0mkqkt1byfc4t6 FOREIGN KEY (magasin_id) REFERENCES magasin(id);

ALTER TABLE ONLY paiement_achat
    ADD CONSTRAINT fkhwvowf8qkmwmttuipkun12g34 FOREIGN KEY (facture_id) REFERENCES facture_achat(id);

ALTER TABLE ONLY depense
    ADD CONSTRAINT fki2as8xxp40roe5gujt8ske3mx FOREIGN KEY (category_id) REFERENCES category_depense(id);

ALTER TABLE ONLY abonnement
    ADD CONSTRAINT fkiafomtq2cse8iu4h8ss8farxb FOREIGN KEY (plan_id) REFERENCES plan_abonnement(id);

ALTER TABLE ONLY magasin
    ADD CONSTRAINT fkibtavs75xrl39o4nuy8nx5205 FOREIGN KEY (entreprise_id) REFERENCES entreprise(id);

ALTER TABLE ONLY entree_stock
    ADD CONSTRAINT fkie3wxj58a8rtu62w63t4wj3el FOREIGN KEY (produit_id) REFERENCES product(id);

ALTER TABLE ONLY abonnement
    ADD CONSTRAINT fkigv4wn2wpghcv80u8e105aux9 FOREIGN KEY (entreprise_id) REFERENCES entreprise(id);

ALTER TABLE ONLY product
    ADD CONSTRAINT fkippb821nwiaprbcw1bb77fhm0 FOREIGN KEY (category_product_id) REFERENCES category_product(id);

ALTER TABLE ONLY paiement_vente
    ADD CONSTRAINT fkit90w4wlmur4es0iejlgu06wq FOREIGN KEY (facture_id) REFERENCES facture_client(id);

ALTER TABLE ONLY sortie_stock
    ADD CONSTRAINT fkiu4l3vdj5puhsefnrp148737 FOREIGN KEY (ligne_vente_id) REFERENCES ligne_commande_vente(id);

ALTER TABLE ONLY proprietaire
    ADD CONSTRAINT fkjek5lju456ptg7m0dnos20il4 FOREIGN KEY (id) REFERENCES utilisateur(id);

ALTER TABLE ONLY rapport_inventaire
    ADD CONSTRAINT fkjrkcdyoqnmd3g12ctgro90jq8 FOREIGN KEY (inventaire_id) REFERENCES inventaire(id);

ALTER TABLE ONLY product_fournisseur
    ADD CONSTRAINT fkk3fmhvj6gs9qj0s1bspuijtdg FOREIGN KEY (fournisseur_id) REFERENCES fournisseur(id);

ALTER TABLE ONLY ligne_commande_vente
    ADD CONSTRAINT fkl1jn0wk90ns4o586lhk98kn8p FOREIGN KEY (product_id) REFERENCES product(id);

ALTER TABLE ONLY utilisateur
    ADD CONSTRAINT fklaic77iqv7dpuet7n5oxmks6j FOREIGN KEY (account_id) REFERENCES account(id);

ALTER TABLE ONLY entree_stock
    ADD CONSTRAINT fklxee9ua3wggcv1ql9ww9j11f0 FOREIGN KEY (commande_achat_id) REFERENCES commande_achat(id);

ALTER TABLE ONLY echeance
    ADD CONSTRAINT fkmyu7shp8g9x7c2xhtilef7qx5 FOREIGN KEY (abonnement_id) REFERENCES abonnement(id);

ALTER TABLE ONLY facture_client
    ADD CONSTRAINT fkn1s8tnjpt1r8wjk2no228iftx FOREIGN KEY (commande_id) REFERENCES commande_vente(id);

ALTER TABLE ONLY fournisseur
    ADD CONSTRAINT fknw4k4ipj6qvvn7bx9qguqbr3n FOREIGN KEY (id) REFERENCES person(id);

ALTER TABLE ONLY entree_stock
    ADD CONSTRAINT fkop4r1w0lq6lmobaj4kcu8nbkx FOREIGN KEY (magasin_id) REFERENCES magasin(id);

ALTER TABLE ONLY category_depense
    ADD CONSTRAINT fkpcgj1fufd12k07bo2racpd675 FOREIGN KEY (entreprise_id) REFERENCES entreprise(id);

ALTER TABLE ONLY ligne_inventaire
    ADD CONSTRAINT fkptyy8xrgil3s0f5tbtbu26xt FOREIGN KEY (product_fournisseur_id) REFERENCES product_fournisseur(id);

ALTER TABLE ONLY commande_achat
    ADD CONSTRAINT fkpx0biijqmbjnqyfi6b2flu6ir FOREIGN KEY (fournisseur_id) REFERENCES fournisseur(id);

ALTER TABLE ONLY utilisateur
    ADD CONSTRAINT fkq2sdeijmyern48fpbpnyoktj4 FOREIGN KEY (photo_id) REFERENCES piece_jointe(id);

ALTER TABLE ONLY piece_jointe
    ADD CONSTRAINT fkq3jbr5bgcwp193166lcgd1kk9 FOREIGN KEY (product_id) REFERENCES product(id);

ALTER TABLE ONLY magasin
    ADD CONSTRAINT fkq7mchkc06kcyt59n7agcvatmc FOREIGN KEY (logo_id) REFERENCES piece_jointe(id);

ALTER TABLE ONLY client
    ADD CONSTRAINT fkr1e0j10i9v9i52l6tqfa69nj0 FOREIGN KEY (id) REFERENCES person(id);

ALTER TABLE ONLY stock
    ADD CONSTRAINT fkrtrnpvf20q4rfswt25krqrw1s FOREIGN KEY (produit_id) REFERENCES product(id);

ALTER TABLE ONLY mouvement_stock
    ADD CONSTRAINT fkt9qeprmvhii1cf0268scs4h5u FOREIGN KEY (stock_id) REFERENCES stock(id);

ALTER TABLE ONLY refresh_token
    ADD CONSTRAINT fktqdnkqvp4hrvcepyq5165v8vc FOREIGN KEY (user_id) REFERENCES utilisateur(id);
