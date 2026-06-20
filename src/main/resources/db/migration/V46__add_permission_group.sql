-- V46 : Table permission_group — regroupe les permissions par domaine métier.
--        Chaque permission reçoit un group_id via UPDATE à la fin de la migration.
--        group_id est nullable : les permissions créées après cette migration
--        sans mise à jour explicite apparaissent comme non-groupées.

CREATE TABLE permission_group (
    id          UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    libelle     VARCHAR(100) NOT NULL,
    description VARCHAR(255)
);

ALTER TABLE permissions
    ADD COLUMN group_id UUID REFERENCES permission_group(id);

-- -----------------------------------------------------------------------
-- Seed des groupes (UUIDs fixes pour idempotence)
-- -----------------------------------------------------------------------
INSERT INTO permission_group (id, libelle, description) VALUES
    ('a1000000-0000-0000-0000-000000000001', 'Accès modules',        'Portes d''accès aux modules métier (sidebar)'),
    ('a1000000-0000-0000-0000-000000000002', 'Authentification',     'Connexion, déconnexion, gestion de session'),
    ('a1000000-0000-0000-0000-000000000003', 'Utilisateurs & rôles', 'Gestion des comptes utilisateurs, employés et rôles RBAC'),
    ('a1000000-0000-0000-0000-000000000004', 'Entreprises & magasins','Gestion des entreprises et de leurs magasins'),
    ('a1000000-0000-0000-0000-000000000005', 'Produits',             'Catalogue produits, catégories et qualités'),
    ('a1000000-0000-0000-0000-000000000006', 'Clients & fournisseurs','Gestion des contacts commerciaux'),
    ('a1000000-0000-0000-0000-000000000007', 'Stock & inventaire',   'Mouvements de stock et inventaires'),
    ('a1000000-0000-0000-0000-000000000008', 'Achats',               'Commandes et factures fournisseurs'),
    ('a1000000-0000-0000-0000-000000000009', 'Ventes',               'Commandes et factures clients'),
    ('a1000000-0000-0000-0000-000000000010', 'Dépenses',             'Enregistrement et suivi des dépenses'),
    ('a1000000-0000-0000-0000-000000000011', 'Paiements',            'Encaissements et remboursements'),
    ('a1000000-0000-0000-0000-000000000012', 'Abonnements & plans',  'Plans SaaS, coupons, promotions et abonnements'),
    ('a1000000-0000-0000-0000-000000000013', 'Documents & rapports', 'Pièces jointes, tableaux de bord et exports'),
    ('a1000000-0000-0000-0000-000000000014', 'Paramètres & audit',   'Configuration système, journal d''audit et messagerie');

-- -----------------------------------------------------------------------
-- Affectation des permissions à leur groupe
-- -----------------------------------------------------------------------
UPDATE permissions SET group_id = 'a1000000-0000-0000-0000-000000000001'
WHERE code IN (
    'OWNER_ACCESS','EMPLOYE_ACCESS','ADMIN_ACCESS','ENTREPRISE_ACCESS',
    'SETTINGS_ACCESS','SALES_ACCESS','PURCHASES_ACCESS','STOCK_ACCESS',
    'INVENTORY_ACCESS','EXPENSES_ACCESS','CATEGORY_EXPENSE_ACCESS'
);

UPDATE permissions SET group_id = 'a1000000-0000-0000-0000-000000000002'
WHERE code IN (
    'AUTH_LOGIN','AUTH_LOGOUT','AUTH_REFRESH_TOKEN',
    'AUTH_RESET_PASSWORD','AUTH_CHANGE_PASSWORD'
);

UPDATE permissions SET group_id = 'a1000000-0000-0000-0000-000000000003'
WHERE code IN (
    'USER_CREATE','USER_READ','USER_UPDATE','USER_DELETE',
    'USER_LOCK','USER_UNLOCK','USER_ASSIGN_ROLE',
    'ROLE_CREATE','ROLE_UPDATE','ROLE_DELETE','SYSTEM_ROLE_UPDATE',
    'EMPLOYE_CREATE','EMPLOYE_READ','EMPLOYE_UPDATE','EMPLOYE_DELETE',
    'EMPLOYE_RESET_PASSWORD','EMPLOYE_PURGE'
);

UPDATE permissions SET group_id = 'a1000000-0000-0000-0000-000000000004'
WHERE code IN (
    'COMPANY_CREATE','COMPANY_READ','COMPANY_UPDATE','COMPANY_DELETE',
    'STORE_CREATE','STORE_READ','STORE_READ_ONE','STORE_UPDATE',
    'STORE_DELETE','STORE_ASSIGN_MANAGER'
);

UPDATE permissions SET group_id = 'a1000000-0000-0000-0000-000000000005'
WHERE code IN (
    'PRODUCT_CREATE','PRODUCT_READ','PRODUCT_UPDATE','PRODUCT_DELETE',
    'PRODUCT_IMPORT','PRODUCT_EXPORT','PRODUCT_UPLOAD_IMAGE',
    'CATEGORY_PRODUCT_CREATE','CATEGORY_PRODUCT_READ',
    'CATEGORY_PRODUCT_UPDATE','CATEGORY_PRODUCT_DELETE',
    'QUALITY_CREATE','QUALITY_READ','QUALITY_UPDATE','QUALITY_DELETE'
);

UPDATE permissions SET group_id = 'a1000000-0000-0000-0000-000000000006'
WHERE code IN (
    'CLIENT_CREATE','CLIENT_READ','CLIENT_UPDATE','CLIENT_DELETE',
    'SUPPLIER_CREATE','SUPPLIER_READ','SUPPLIER_UPDATE','SUPPLIER_DELETE'
);

UPDATE permissions SET group_id = 'a1000000-0000-0000-0000-000000000007'
WHERE code IN (
    'STOCK_READ','STOCK_ENTRY','STOCK_EXIT','STOCK_ADJUSTMENT',
    'STOCK_INVENTORY','STOCK_TRANSFER',
    'INVENTORY_READ','INVENTORY_CREATE','INVENTORY_WRITE',
    'INVENTORY_BILAN','INVENTORY_CLOSE','INVENTORY_CANCEL'
);

UPDATE permissions SET group_id = 'a1000000-0000-0000-0000-000000000008'
WHERE code IN (
    'PURCHASE_CREATE','PURCHASE_READ','PURCHASE_UPDATE','PURCHASE_DELETE',
    'PURCHASE_APPROVE','PURCHASE_PAY','PURCHASE_CANCEL'
);

UPDATE permissions SET group_id = 'a1000000-0000-0000-0000-000000000009'
WHERE code IN (
    'SALE_CREATE','SALE_READ','SALE_UPDATE','SALE_DELETE',
    'SALE_APPROVE','SALE_PAY','SALE_CANCEL'
);

UPDATE permissions SET group_id = 'a1000000-0000-0000-0000-000000000010'
WHERE code IN (
    'EXPENSE_CREATE','EXPENSE_READ','EXPENSE_UPDATE',
    'EXPENSE_DELETE','EXPENSE_PAY'
);

UPDATE permissions SET group_id = 'a1000000-0000-0000-0000-000000000011'
WHERE code IN (
    'PAYMENT_CREATE','PAYMENT_READ','PAYMENT_CANCEL','PAYMENT_REFUND'
);

UPDATE permissions SET group_id = 'a1000000-0000-0000-0000-000000000012'
WHERE code IN (
    'PLAN_CREATE','PLAN_READ','PLAN_UPDATE','PLAN_DELETE',
    'TYPE_PLAN_CREATE','TYPE_PLAN_READ','TYPE_PLAN_UPDATE','TYPE_PLAN_DELETE',
    'COUPON_CREATE','COUPON_READ','COUPON_UPDATE','COUPON_DELETE',
    'PROMOTION_CREATE','PROMOTION_READ','PROMOTION_UPDATE','PROMOTION_DELETE',
    'SUBSCRIPTION_CREATE','SUBSCRIPTION_READ','SUBSCRIPTION_UPDATE',
    'SUBSCRIPTION_CANCEL','SUBSCRIPTION_PAY','SUBSCRIPTION_RENEW',
    'SUBSCRIPTION_VALIDATE',
    'MOYEN_PAIEMENT_CREATE','MOYEN_PAIEMENT_UPDATE','MOYEN_PAIEMENT_DELETE'
);

UPDATE permissions SET group_id = 'a1000000-0000-0000-0000-000000000013'
WHERE code IN (
    'DOCUMENT_UPLOAD','DOCUMENT_READ','DOCUMENT_DELETE','DOCUMENT_DOWNLOAD',
    'DASHBOARD_READ','REPORT_EXPORT','REPORT_FINANCIAL','REPORT_STOCK','REPORT_SALES'
);

UPDATE permissions SET group_id = 'a1000000-0000-0000-0000-000000000014'
WHERE code IN (
    'SETTINGS_UPDATE','SETTINGS_READ',
    'AUDIT_READ','AUDIT_EXPORT',
    'CONTACT_READ','CONTACT_RESPOND'
);
