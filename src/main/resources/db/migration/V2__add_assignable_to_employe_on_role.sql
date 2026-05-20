-- Marque explicite des rôles assignables à un employé (par opposition aux
-- rôles "infrastructure" : ADMIN SaaS-vendor, PROPRIETAIRE issu de l'inscription
-- libre). Remplace l'heuristique "porte la permission EMPLOYE_ACCESS" qui
-- fuyait sur ADMIN (lequel possède EMPLOYE_ACCESS pour la console super-admin).
--
-- Convention : DEFAULT false (sûr par défaut). Le YAML RBAC active explicitement
-- la colonne pour MANAGER et VENDEUR via RolesPermissionsSyncService.

ALTER TABLE role
    ADD COLUMN assignable_to_employe BOOLEAN NOT NULL DEFAULT FALSE;

-- Backfill des rôles existants. Idempotent (UPDATE … WHERE libelle IN …).
UPDATE role
SET assignable_to_employe = TRUE
WHERE libelle IN ('MANAGER', 'VENDEUR');
