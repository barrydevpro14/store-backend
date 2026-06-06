-- V7 : déporte l'état "essai gratuit" hors de Abonnement, sur Entreprise.
--
-- Modèle :
--   Abonnement = uniquement souscriptions payées (ou EN_ATTENTE de paiement).
--                 type_plan_abonnement_id NOT NULL, plan_abonnement_id supprimé.
--   Entreprise = porte trial_plan_id + trial_end_date pour la fenêtre d'essai.
--                 trial_used (déjà existant) marque "essai déjà consommé".
--
-- Stratégie : drop & reseed côté Abonnement (dev only). Les abonnements de
-- type "trial" existants sont supprimés ; le DataInitializer reseede le plan
-- d'essai. Les signups futurs alimenteront `entreprise.trial_*`.

-- 1. Purger les rangées Abonnement (toutes — incluant trials et payés)
DELETE FROM utilisation_coupon;
DELETE FROM paiement_abonnement;
DELETE FROM abonnement;

-- 2. Drop la FK plan + la colonne (le plan est désormais reachable via type.plan)
ALTER TABLE abonnement DROP CONSTRAINT IF EXISTS fk_abonnement_plan_abonnement;
DROP INDEX IF EXISTS idx_abonnement_plan_abonnement_id;
ALTER TABLE abonnement DROP COLUMN plan_abonnement_id;

-- 3. Promouvoir type_plan_abonnement_id en NOT NULL (toutes lignes purgées)
ALTER TABLE abonnement ALTER COLUMN type_plan_abonnement_id SET NOT NULL;

-- 4. Ajouter les colonnes trial_* sur Entreprise + FK vers plan_abonnement
ALTER TABLE entreprise
    ADD COLUMN trial_plan_id uuid,
    ADD COLUMN trial_end_date date;

ALTER TABLE entreprise
    ADD CONSTRAINT fk_entreprise_trial_plan
        FOREIGN KEY (trial_plan_id) REFERENCES plan_abonnement(id);

CREATE INDEX idx_entreprise_trial_plan_id ON entreprise(trial_plan_id);
